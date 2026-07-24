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
    /** Duong gui cu danh len server, giao lai cho tung avatar khi tao. */
    private final HitSender sender;

    public RemotePlayers(HitSender sender) {
        this.sender = sender;
    }

    /**
     * Cap nhat vi tri mot nguoi choi (tao moi neu lan dau thay ten nay).
     *
     * @return true neu day la lan DAU thay nguoi nay - dung de bao "vua vao the gioi"
     */
    public boolean update(String name, float x, float y, float z, float yaw) {
        RemotePlayer player = byName.get(name);
        boolean joined = player == null;
        if (joined) {
            player = new RemotePlayer(name, sender);
            byName.put(name, player);
        }
        player.setTarget(x, y, z, yaw);
        return joined;
    }

    /** Ten nhung nguoi choi khac dang online (dung cho lenh /list). */
    public java.util.Set<String> names() {
        return byName.keySet();
    }

    /** Nguoi nay vua pha khoi hoac vua danh ai do - cho avatar cua ho quo tay. */
    public void swing(String name) {
        RemotePlayer player = byName.get(name);
        if (player != null) {
            player.swingArm();
        }
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
