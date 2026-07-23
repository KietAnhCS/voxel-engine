package com.voxel.server.auth;

import com.voxel.server.auth.AuthDtos.AuthResult;
import com.voxel.server.auth.AuthDtos.Credentials;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cac diem cuoi (endpoint) khong can dang nhap: dang ky va dang nhap.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public AuthResult register(@Valid @RequestBody Credentials request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResult login(@Valid @RequestBody Credentials request) {
        return auth.login(request);
    }
}
