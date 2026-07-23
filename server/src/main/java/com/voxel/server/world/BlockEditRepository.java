package com.voxel.server.world;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BlockEditRepository extends JpaRepository<BlockEdit, Long> {

    List<BlockEdit> findByWorldId(Long worldId);

    @Transactional
    void deleteByWorldId(Long worldId);

    /** Xoa o khoi tai dung mot toa do (dung truoc khi ghi de o do bang khoi moi). */
    @Transactional
    void deleteByWorldIdAndXAndYAndZ(Long worldId, int x, int y, int z);
}
