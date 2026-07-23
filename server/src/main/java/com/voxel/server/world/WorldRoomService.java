package com.voxel.server.world;

import com.voxel.server.world.WorldDtos.EditDto;
import com.voxel.server.world.WorldDtos.PlayerStateDto;
import com.voxel.server.world.WorldDtos.WorldSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nghiep vu cho cac THE GIOI CHUNG theo MA. Nguoi choi go mot ma (vd "123"); ai cung ma thi
 * vao cung the gioi. Ma chua ton tai thi tao moi voi seed ngau nhien.
 *
 * Moi o khoi ai dat/pha deu luu vao the gioi ung voi ma do va phat lai cho nguoi cung phong
 * qua WebSocket (xem WorldSocketHandler). Vi tri nguoi choi KHONG luu.
 */
@Service
public class WorldRoomService {

    private final WorldRepository worlds;
    private final BlockEditRepository edits;

    public WorldRoomService(WorldRepository worlds, BlockEditRepository edits) {
        this.worlds = worlds;
        this.edits = edits;
    }

    /**
     * Toan bo the gioi cua mot ma gui cho nguoi vua vao: seed + het cac o da xay.
     * Player tra ve y = 0 (dau hieu "spawn moi") de game tu tim mat dat cho nguoi choi.
     */
    @Transactional
    public WorldSnapshot snapshot(String code) {
        GameWorld world = worldOf(code);

        List<EditDto> editDtos = new ArrayList<>();
        for (BlockEdit edit : edits.findByWorldId(world.getId())) {
            editDtos.add(new EditDto(edit.getX(), edit.getY(), edit.getZ(), edit.getBlockId()));
        }

        PlayerStateDto spawn = new PlayerStateDto(0f, 0f, 0f, 0f, 0f, 1);
        return new WorldSnapshot(world.getSeed(), spawn, editDtos);
    }

    /**
     * Ghi mot o khoi vao the gioi cua mot ma: xoa o cu tai dung toa do (neu co) roi luu o moi.
     * Giu ca o khong khi (b = 0) vi do la "da pha khoi goc" - khac voi chua tung dong toi.
     */
    @Transactional
    public void applyEdit(String code, int x, int y, int z, int blockId) {
        Long id = worldOf(code).getId();
        edits.deleteByWorldIdAndXAndYAndZ(id, x, y, z);
        edits.save(new BlockEdit(id, x, y, z, (short) blockId));
    }

    /** Lay the gioi theo ma, tao moi voi seed ngau nhien neu ma nay chua ai dung. */
    private GameWorld worldOf(String code) {
        return worlds.findByCode(code)
                .orElseGet(() -> worlds.save(new GameWorld(code, ThreadLocalRandom.current().nextLong())));
    }
}
