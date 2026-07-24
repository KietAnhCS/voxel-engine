package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.world.World;

/**
 * Goi chung nhung thu moi trang thai AI cua quai vat can: the gioi khoi (de bam mat dat),
 * bo tim duong A* va nguoi choi de duoi theo. Cac hang so hanh vi (tam phat hien, toc do,
 * sat thuong...) cung nam o day cho de chinh.
 */
public final class MonsterContext {

    /** Trong pham vi nay quai bat dau phat hien va duoi nguoi choi. */
    public static final float DETECT_RANGE = 16f;
    /** Ra ngoai pham vi nay thi quai bo cuoc, quay ve dung yen. */
    public static final float LOSE_RANGE = 28f;
    /** Vao gan hon khoang nay thi quai dung lai va danh. */
    public static final float ATTACK_RANGE = 1.7f;
    /** Toc do duoi (o / giay). */
    public static final float CHASE_SPEED = 3.2f;
    /** Cach nhip giua hai cu danh (giay). */
    public static final float ATTACK_INTERVAL = 1.0f;
    /** Moi cu danh tru 2 mau = 1 trai tim, giong zombie Minecraft. */
    public static final int ATTACK_DAMAGE = 2;

    private final World world;
    private final AStarPathFinder pathFinder;
    private final PlayerTarget player;

    public MonsterContext(World world, AStarPathFinder pathFinder, PlayerTarget player) {
        this.world = world;
        this.pathFinder = pathFinder;
        this.player = player;
    }

    public World world() {
        return world;
    }

    public AStarPathFinder pathFinder() {
        return pathFinder;
    }

    public PlayerTarget player() {
        return player;
    }

    /** Khoang cach ngang (bo qua cao do) giua hai diem. */
    public static float horizontalDist(Vector3 a, Vector3 b) {
        float dx = a.x - b.x;
        float dz = a.z - b.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
