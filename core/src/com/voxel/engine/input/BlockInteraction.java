package com.voxel.engine.input;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.physics.VoxelRaycaster;
import com.voxel.engine.world.World;

public interface BlockInteraction {

    /**
     * Chuot trai vua bam: thu danh sinh vat dang o trong tam ngam TRUOC.
     *
     * @param origin    vi tri mat nguoi choi
     * @param direction huong dang nhin
     * @param reach     khong duoc voi xa hon bay nhieu khoi (da tru phan bi khoi chan che)
     * @return true neu danh trung ai do - luc do engine khong pha khoi nua
     */
    boolean onAttack(Vector3 origin, Vector3 direction, float reach);

    void onBreak(World world, VoxelRaycaster.Hit hit);

    void onPlace(World world, VoxelRaycaster.Hit hit, boolean alternate);
}
