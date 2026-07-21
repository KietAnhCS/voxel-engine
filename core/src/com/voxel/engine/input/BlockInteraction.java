package com.voxel.engine.input;

import com.voxel.engine.physics.VoxelRaycaster;
import com.voxel.engine.world.World;

public interface BlockInteraction {

    void onBreak(World world, VoxelRaycaster.Hit hit);

    void onPlace(World world, VoxelRaycaster.Hit hit, boolean alternate);
}
