package com.voxel.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.voxel.game.VoxelGame;

public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Voxel Engine";
        config.width = 1280;
        config.height = 720;
        config.useGL30 = false;
        config.vSyncEnabled = true;
        config.samples = 0;
        config.foregroundFPS = 0;
        config.backgroundFPS = 0;
        new LwjglApplication(new VoxelGame(), config);
    }
}
