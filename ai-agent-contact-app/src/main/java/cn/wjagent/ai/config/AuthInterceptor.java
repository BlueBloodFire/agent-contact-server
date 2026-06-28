package cn.wjagent.ai.config;

import cn.wjagent.ai.trigger.http.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"E_UNAUTH\",\"info\":\"未登录\"}");
            return false;
        }

        String token = auth.substring(7);
        AuthController.TokenInfo info = AuthController.TOKEN_STORE.get(token);
        if (info == null || System.currentTimeMillis() > info.expireAt()) {
            AuthController.TOKEN_STORE.remove(token);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"E_TOKEN_EXPIRED\",\"info\":\"登录已过期，请重新登录\"}");
            return false;
        }

        return true;
    }
}
