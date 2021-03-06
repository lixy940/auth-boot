package com.lixy.controller;
import com.lixy.constants.StaticParameterUtils;
import com.lixy.vo.ResponseResult;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 登录相关
 *
 * @author zjjlive)
 * @version 1.0
 * @website https://www.foreknow.me
 * @date 2018/4/24 14:37
 * @since 1.0
 */
@CrossOrigin(origins = "http://localhost:8888", maxAge = 3600)
@Controller
@RequestMapping("/passport")
public class PassportController {
    private static final Logger logger = LoggerFactory.getLogger(PassportController.class);

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/403")
    public String unAuth(Model model) {
        return "403";
    }
    /**
     * 登录
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/signin")
    @ResponseBody
    public ResponseResult submitLogin(String username, String password, boolean rememberMe, String kaptcha) {
        ResponseResult result = new ResponseResult();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password,rememberMe);
        //获取当前的Subject
        Subject currentUser = SecurityUtils.getSubject();
        try {
            // 在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
            // 每个Realm都能在必要时对提交的AuthenticationTokens作出反应
            // 所以这一步在调用login(token)方法时,它会走到xxRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
            currentUser.login(token);
            return result;
        } catch (Exception e) {
            logger.error("登录失败，用户名[{}]", username, e);
            token.clear();
            result.setMsg(e.getMessage());
            result.setStatus(500);
            return result;
        }
    }

    /**
     * 使用权限管理工具进行用户的退出，跳出登录，给出提示信息
     *
     * @param redirectAttributes
     * @return
     */
    @GetMapping("/logout")
    public String logout(RedirectAttributes redirectAttributes) {
        // http://www.oschina.net/question/99751_91561
        // 此处有坑： 退出登录，其实不用实现任何东西，只需要保留这个接口即可，也不可能通过下方的代码进行退出
//         SecurityUtils.getSubject().logout();
        // 因为退出操作是由Shiro控制的       filterChainDefinitionMap.put("/logout", "logout");
        redirectAttributes.addFlashAttribute("message", "您已安全退出");
        return StaticParameterUtils.PASSPORT_LOGIN_URI;
    }



}
