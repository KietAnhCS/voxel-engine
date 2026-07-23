package com.voxel.game.net;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Danh sach nhung nguoi choi KHAC dang online, khoa theo ten. Duoc ghi tu luong WebSocket
 * (khi co tin "player"/"leave" bay ve) va doc tu luong game (khi ve avatar) - nen dung
 * bang bam da luong an toan.
 */
public final class RemotePlayers {

    private final Map<String, RemotePlayer> byName = new ConcurrentHashMap<String, RemotePlayer>();

    /** Cap nhat vi tri mot nguoi choi (tao moi neu lan dau thay ten nay). */
    public void update(String name, float x, float y, float z, float yaw) {
        RemotePlayer player = byName.get(name);
        if (player == null) {
            player = new RemotePlayer();
            byName.put(name, player);
        }
        player.setTarget(x, y, z, yaw);
    }

    /** Nguoi choi da thoat - bo avatar cua ho. */
    public void remove(String name) {
        byName.remove(name);
    }

    /** Keo moi avatar truot mot buoc ve vi tri dich. Goi moi khung hinh. */
    public void advance(float delta) {
        for (RemotePlayer player : byName.values()) {
            player.advance(delta);
        }
    }

    public Collection<RemotePlayer> all() {
        return byName.values();
    }
}
