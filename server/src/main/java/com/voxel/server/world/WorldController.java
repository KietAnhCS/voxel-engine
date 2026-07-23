package com.voxel.server.world;

import com.voxel.server.world.WorldDtos.WorldSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tai the gioi theo MA khi nguoi choi vua dang nhap: seed + het cac o khoi da xay.
 * Da qua AuthInterceptor nen chi nguoi da dang nhap moi goi duoc.
 *
 * Tu day tro di, dat/pha khoi va di chuyen deu chay qua WebSocket (WorldSocketHandler),
 * khong con luu tung phan qua REST nua.
 */
@RestController
@RequestMapping("/api/world")
public class WorldController {

    private final WorldRoomService world;

    public WorldController(WorldRoomService world) {
        this.world = world;
    }

    @GetMapping
    public WorldSnapshot load(@RequestParam(defaultValue = "123") String code) {
        return world.snapshot(code.trim());
    }
}
