package com.voxel.server.net;

import com.voxel.server.auth.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * Kiem tra the dang nhap NGAY LUC bat tay WebSocket, truoc khi mo ket noi.
 *
 * Trinh duyet/HttpClient khong gui duoc header Authorization khi mo WebSocket, nen the JWT
 * di kem trong chuoi truy van: ws://host/ws/world?token=XXX. Neu the hop le, ta nho lai
 * id + ten nguoi choi vao thuoc tinh phien de handler dung sau; neu khong, tu choi bat tay.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    /** Ten thuoc tinh phien luu id, ten nguoi choi da xac thuc, va ma the gioi ho vao. */
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String CODE = "code";

    private final JwtService jwt;

    public JwtHandshakeInterceptor(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = paramFrom(request.getURI(), "token");
        if (token == null) {
            return false;
        }

        Long userId = jwt.userIdFromToken(token);
        String username = jwt.usernameFromToken(token);
        if (userId == null || username == null) {
            return false;
        }

        String code = paramFrom(request.getURI(), "code");
        attributes.put(USER_ID, userId);
        attributes.put(USERNAME, username);
        attributes.put(CODE, (code == null || code.isBlank()) ? "123" : code.trim());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Khong can lam gi sau khi bat tay xong.
    }

    /** Tim gia tri cua mot tham so trong chuoi truy van cua URI (khong dung thu vien ngoai cho gon). */
    private static String paramFrom(URI uri, String name) {
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }
}
