package com.lixy.shiro.credentials;

import com.lixy.constants.SessionConst;
import com.lixy.entity.SysUser;
import com.lixy.service.SysUserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 * Shiro-密码输入错误的状态下重试次数的匹配管理
 * @since 1.0
 */
public class RetryLimitCredentialsMatcher extends CredentialsMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryLimitCredentialsMatcher.class);

    /**
     * 用户登录次数计数  redisKey 前缀
     */
    private static final String SHIRO_LOGIN_COUNT = "shiro_login_count_";
    /**
     * 用户登录是否被锁定    一小时 redisKey 前缀
     */
    private static final String SHIRO_IS_LOCK = "shiro_is_lock_";
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SysUserService userService;

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        SysUser shiroUser = (SysUser) info.getPrincipals().getPrimaryPrincipal();
        Long userId = shiroUser.getId();
        SysUser user = userService.getByPrimaryKey(userId);
        String username = user.getLoginName();
        // 访问一次，计数一次
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        String loginCountKey = SHIRO_LOGIN_COUNT + username;
        String isLockKey = SHIRO_IS_LOCK + username;
        opsForValue.increment(loginCountKey, 1);

        if (redisTemplate.hasKey(isLockKey)) {
            throw new ExcessiveAttemptsException("帐号[" + username + "]已被禁止登录！");
        }

        // 计数大于5时，设置用户被锁定一小时
        String loginCount = String.valueOf(opsForValue.get(loginCountKey));
        int retryCount = (5 - Integer.parseInt(loginCount));
        if (retryCount <= 0) {
            opsForValue.set(isLockKey, "LOCK");
            redisTemplate.expire(isLockKey, 1, TimeUnit.HOURS);
            redisTemplate.expire(loginCountKey, 1, TimeUnit.HOURS);
            throw new ExcessiveAttemptsException("由于密码输入错误次数过多，帐号[" + username + "]已被禁止登录！");
        }

        boolean matches = super.doCredentialsMatch(token, info);
        if (!matches) {
            String msg = retryCount <= 0 ? "您的账号一小时内禁止登录！" : "您还剩" + retryCount + "次重试的机会";
            throw new AccountException("帐号或密码不正确！" + msg);
        }

        //清空登录计数
        redisTemplate.delete(loginCountKey);
        //跟新上次登录时间
        try {
            userService.updateUserLastLoginInfo(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 当验证都通过后，把用户信息放在session里
        // 注：User必须实现序列化
        SecurityUtils.getSubject().getSession().setAttribute(SessionConst.USER_SESSION_KEY, user);
        return true;
    }
}
