package com.voxel.game.mob;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.voxel.engine.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Sinh, cap nhat va ve toan bo quai vat. Chi hoat dong o che do SINH TON - sang tao thi
 * dep sach quai. Quai sinh ngau nhien quanh nguoi choi (nhung khong ngay truoc mat), tim
 * mat dat de dat chan, roi tu duoi theo va tan cong bang bo nao A* + State ben trong.
 *
 * <p>Quai o qua xa (nguoi choi da di rat xa) thi tu bien mat de khong don qua nhieu.
 */
public final class MonsterManager implements Disposable {

    private static final int MAX_MONSTERS = 4;
    private static final float SPAWN_INTERVAL = 6f;
    private static final float MIN_SPAWN_DIST = 12f;
    private static final float MAX_SPAWN_DIST = 22f;
    private static final float DESPAWN_DIST = 48f;
    /** Sang hon muc nay (0..15) thi quai khong sinh ra nua - dat duoc la yen than. */
    private static final int MAX_SPAWN_LIGHT = 7;
    /** Anh sang troi tu muc nay tro len giua ban ngay la quai chay nang. */
    private static final int SUNBURN_LIGHT = 12;

    private final World world;
    private final PlayerTarget player;
    private final MonsterContext context;
    private final MonsterRenderer renderer = new MonsterRenderer();
    private final List<Monster> monsters = new ArrayList<Monster>();
    private final Random random = new Random();
    private float spawnTimer;
    /** So con vua bi nguoi choi ha, cho {@code PlaySession} lay ra de cong kinh nghiem. */
    private int kills;

    public MonsterManager(World world, PlayerTarget player) {
        this.world = world;
        this.player = player;
        this.context = new MonsterContext(world, new AStarPathFinder(world), player);
    }

    /**
     * Chay mot khung hinh he quai vat. Ngoai sinh ton thi khong co con nao.
     *
     * @param night dang la ban dem (mat troi da khuat) - luc do quai moi bo ra ngoai troi
     */
    public void update(float delta, boolean survival, boolean night) {
        if (!survival) {
            monsters.clear();
            spawnTimer = 0f;
            return;
        }

        spawnTimer += delta;
        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f;
            trySpawn(night);
        }

        for (Iterator<Monster> it = monsters.iterator(); it.hasNext(); ) {
            Monster monster = it.next();
            // Het mau thi bien mat; qua xa (nguoi choi da di rat xa) cung bo di cho do don.
            if (monster.isDead()) {
                it.remove();
                kills++;
                continue;
            }
            burnInSunlight(monster, delta, night);
            monster.update(context, delta);
            if (MonsterContext.horizontalDist(monster.position(), player.position()) > DESPAWN_DIST) {
                it.remove();
            }
        }
    }

    /** Danh sach quai dang song - de ngam danh va de ve. */
    public List<Monster> alive() {
        return monsters;
    }

    /** Lay ra so quai vua ha ke tu lan hoi truoc (doc xong thi dat lai ve 0). */
    public int takeKills() {
        int taken = kills;
        kills = 0;
        return taken;
    }

    /**
     * Thu sinh mot con quai o vong tron quanh nguoi choi, tren mat dat trong.
     *
     * <p>Luat sang toi lay theo Minecraft:
     * <ul>
     *   <li>Cho nao duoc duoc/den chieu sang qua {@link #MAX_SPAWN_LIGHT} thi TUYET DOI khong sinh -
     *       cam duoc quanh nha la ngu yen.</li>
     *   <li>Ban ngay ngoai troi cung khong sinh; nhung trong hang toi (khong thay bau troi)
     *       thi gio nao cung sinh duoc.</li>
     *   <li>Con lai: cho cang sang thi kha nang sinh cang thap.</li>
     * </ul>
     */
    private void trySpawn(boolean night) {
        if (monsters.size() >= MAX_MONSTERS) {
            return;
        }
        Vector3 p = player.position();
        float angle = random.nextFloat() * MathUtils.PI2;
        float dist = MIN_SPAWN_DIST + random.nextFloat() * (MAX_SPAWN_DIST - MIN_SPAWN_DIST);
        int x = (int) Math.floor(p.x + MathUtils.cos(angle) * dist);
        int z = (int) Math.floor(p.z + MathUtils.sin(angle) * dist);

        int y = groundHeight(x, z);
        if (y <= 0) {
            return;
        }

        int torchLight = world.blockLightAt(x, y, z);
        if (torchLight > MAX_SPAWN_LIGHT) {
            return;
        }
        int skyLight = world.skyLightAt(x, y, z);
        if (!night && skyLight > MAX_SPAWN_LIGHT) {
            return;
        }

        // Ban dem anh sang troi khong con tinh; chi con quang duoc lam giam kha nang sinh.
        int light = Math.max(torchLight, night ? 0 : skyLight);
        if (random.nextInt(MAX_SPAWN_LIGHT + 1) < light) {
            return;
        }
        monsters.add(new Monster(x + 0.5f, y, z + 0.5f));
    }

    /** Ban ngay ma dung phoi nang giua troi thi quai chay dan roi boc hoi. */
    private void burnInSunlight(Monster monster, float delta, boolean night) {
        if (night) {
            return;
        }
        Vector3 at = monster.position();
        int x = (int) Math.floor(at.x);
        int y = (int) Math.floor(at.y);
        int z = (int) Math.floor(at.z);
        if (world.skyLightAt(x, y, z) >= SUNBURN_LIGHT) {
            monster.burn(delta);
        }
    }

    /** Cao do o dat trong dau tien tinh tu tren xuong o cot (x,z); -1 neu khong tim thay. */
    private int groundHeight(int x, int z) {
        for (int y = world.config().worldHeight() - 1; y > 0; y--) {
            if (world.blockAt(x, y - 1, z).isCollidable()
                    && world.blockAt(x, y, z).isAir()
                    && world.blockAt(x, y + 1, z).isAir()) {
                return y;
            }
        }
        return -1;
    }

    public void render(PerspectiveCamera camera) {
        renderer.render(camera, monsters);
    }

    /** So quai dang song - de hien len bang go loi F3 neu can. */
    public int count() {
        return monsters.size();
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}
