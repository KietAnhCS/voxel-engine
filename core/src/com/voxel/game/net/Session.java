package com.voxel.game.net;

/**
 * Phien dang nhap: the JWT + ten nguoi dung + ma the gioi da vao + the gioi da tai ve.
 * Man dang nhap tao doi tuong nay khi dang nhap thanh cong va truyen sang man choi game.
 */
public final class Session {

    public final String token;
    public final String username;
    public final String code;
    public final WorldSnapshot world;

    public Session(String token, String username, String code, WorldSnapshot world) {
        this.token = token;
        this.username = username;
        this.code = code;
        this.world = world;
    }
}
