package com.voxel.engine.block;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.util.Direction;

public interface QuadEmitter {

    void quad(float x, float y, float z,
              Direction face,
              float width, float height,
              String textureRegion,
              Color[] cornerColors);

    void billboard(float x, float y, float z,
                   float offsetX, float offsetZ,
                   boolean alongX,
                   String textureRegion,
                   Color tint);

    Color[] cornerBuffer();
}
