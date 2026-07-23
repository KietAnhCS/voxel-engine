package com.voxel.server.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cac ban ghi (record) mo ta du lieu di vao / di ra qua REST. Tach khoi @Entity de
 * khong bao gio lo chuoi bam mat khau ra ngoai - mau thiet ke DTO (Data Transfer Object).
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Du lieu dang ky / dang nhap gui len. */
    public record Credentials(
            @NotBlank @Size(min = 3, max = 32) String username,
            @NotBlank @Size(min = 4, max = 64) String password) {
    }

    /** Ket qua tra ve khi dang ky / dang nhap thanh cong. */
    public record AuthResult(String token, String username) {
    }
}
