package com.voxel.game.net;

/**
 * Noi gui cu danh len server. {@link WorldClient} hien thuc no.
 *
 * Nho co giao dien nho nay ma {@link RemotePlayer} co the "an don nguoc lai" - tuc bao cho
 * may cua nguoi bi danh biet ho vua mat mau - ma khong can biet gi ve WebSocket.
 */
public interface HitSender {

    /** Bao server: minh vua danh trung {@code victim} {@code damage} mau. */
    void sendHit(String victim, int damage);
}
