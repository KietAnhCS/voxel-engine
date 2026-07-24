package com.voxel.game.net;

/**
 * Mot cu danh minh vua an tu nguoi choi khac: ai danh va mat bao nhieu mau.
 *
 * Tin nay toi tren luong WebSocket nhung mau nam o {@code PlayerStats} cua luong game,
 * nen no duoc xep hang doi trong {@link WorldClient} roi luong game lay ra ap sau.
 */
public final class Hurt {

    public final String from;
    public final int damage;

    public Hurt(String from, int damage) {
        this.from = from;
        this.damage = damage;
    }
}
