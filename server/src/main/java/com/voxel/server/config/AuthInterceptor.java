package com.voxel.server.config;

import com.voxel.server.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chan moi request vao /api/world/** truoc khi toi controller de kiem tra the dang nhap.
 * Neu the hop le, ghi id nguoi dung vao thuoc tinh request de controller doc lai.
 * Neu khong, tra ve 401 va dung tai day.
 *
 * Cach lam nay don gian hon chuoi loc Spring Security day du - de doc cho bai tap.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Ten thuoc tinh dung de truyen id nguoi dung tu day sang controller. */
    public static final String USER_ID_ATTRIBUTE = "voxelUserId";

    private final JwtService jwt;

    public AuthInterceptor(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long userId = jwt.userIdFromToken(header.substring("Bearer ".length()));
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
