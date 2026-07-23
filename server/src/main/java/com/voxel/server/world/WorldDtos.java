package com.voxel.server.world;

import java.util.List;

/**
 * Du lieu the gioi trao doi voi game. Dung record cho gon; Jackson tu chuyen sang/tu JSON.
 */
public final class WorldDtos {

    private WorldDtos() {
    }

    /** Trang thai nguoi choi: vi tri, huong nhin, che do choi. */
    public record PlayerStateDto(float x, float y, float z, float yaw, float pitch, int mode) {
    }

    /** Mot o khoi da sua: toa do the gioi + ma khoi (0 = khong khi). */
    public record EditDto(int x, int y, int z, int b) {
    }

    /** Toan bo the gioi gui ve game khi dang nhap. */
    public record WorldSnapshot(long seed, PlayerStateDto player, List<EditDto> edits) {
    }
}
