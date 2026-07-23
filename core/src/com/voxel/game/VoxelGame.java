package com.voxel.game;

import com.badlogic.gdx.Game;

public final class VoxelGame extends Game {

    @Override
    public void create() {
        // Vao game la hien man dang nhap truoc; dang nhap dung moi mo the gioi.
        setScreen(new LoginScreen(this));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
