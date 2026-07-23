package com.voxel.server.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Mau thiet ke Repository: Spring Data tu sinh code truy van CSDL tu ten phuong thuc.
 * Chi can khai bao interface, khong phai viet SQL.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
