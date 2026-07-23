package com.voxel.game.net;

/**
 * Loi khi goi backend, kem thong bao tieng Viet de hien thang len man dang nhap.
 */
public final class BackendException extends RuntimeException {

    public BackendException(String message) {
        super(message);
    }
}
