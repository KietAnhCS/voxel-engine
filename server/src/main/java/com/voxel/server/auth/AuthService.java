package com.voxel.server.auth;

import com.voxel.server.auth.AuthDtos.AuthResult;
import com.voxel.server.auth.AuthDtos.Credentials;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Xu ly nghiep vu dang ky / dang nhap. Bam mat khau bang BCrypt (co "muoi" ngau nhien
 * moi lan) va cap JWT khi dung.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    /** Tao tai khoan moi. Bao loi 409 neu ten da co nguoi dung. */
    public AuthResult register(Credentials request) {
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
        }
        String hash = passwordEncoder.encode(request.password());
        User saved = users.save(new User(username, hash));
        return new AuthResult(jwt.issueToken(saved), saved.getUsername());
    }

    /** Kiem tra ten + mat khau. Bao loi 401 neu sai. */
    public AuthResult login(Credentials request) {
        User user = users.findByUsername(request.username().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Wrong username or password"));
        return new AuthResult(jwt.issueToken(user), user.getUsername());
    }
}
