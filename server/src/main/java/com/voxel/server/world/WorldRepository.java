package com.voxel.server.world;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorldRepository extends JpaRepository<GameWorld, Long> {

    Optional<GameWorld> findByCode(String code);
}
