package com.voxel.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Diem khoi dong cua backend. Chay ham main nay la Spring Boot tu dung mot may chu
 * web, quet cac @RestController / @Entity trong goi com.voxel.server va noi voi PostgreSQL.
 */
@SpringBootApplication
public class VoxelServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoxelServerApplication.class, args);
    }
}
