package com.voxel.game.play;

/**
 * Mau va duong khi cua nguoi choi - phan "sinh ton" cua game.
 *
 * Cac con so lay theo Minecraft: 20 mau (= 10 trai tim, moi trai 2 mau),
 * 15 giay nin tho (= 10 bong bong), roi qua 3 o thi bat dau dau.
 * Lop nay khong biet gi ve do hoa hay dieu khien - no chi nhan vao trang thai
 * the chat moi khung hinh roi tra ve mau con lai.
 */
public final class PlayerStats {

    public static final int MAX_HEALTH = 20;
    /** So giay nin tho toi da khi o duoi nuoc. */
    public static final float MAX_AIR = 15f;
    /** 20 do no = 10 cai dui ga tren man hinh. */
    public static final int MAX_FOOD = 20;

    /**
     * Di lai bao lau thi tut mot nac do no. Tinh theo GIAY chu khong theo khung hinh,
     * neu khong may chay 144 hinh/giay se lam nguoi choi doi nhanh gap doi may 60.
     */
    private static final float EXHAUSTION_WALKING = 0.05f;
    private static final float EXHAUSTION_IDLE = 0.005f;
    private static final float EXHAUSTION_LIMIT = 1f;
    /** Tren muc nay moi tu hoi mau, duoi 0 thi bat dau doi lay mau (giong Minecraft). */
    private static final int FOOD_TO_REGEN = 18;
    private static final float STARVE_INTERVAL = 4f;

    /** Roi qua bay nhieu o moi bat dau dau (Minecraft cung la 3). */
    private static final float SAFE_FALL = 3f;
    /** Het khi thi cu moi giay mat 2 mau (mot trai tim). */
    private static final float DROWN_DAMAGE_INTERVAL = 1f;
    private static final int DROWN_DAMAGE = 2;
    /** Ngoi khong mot lat thi hoi lai 1 mau (o day khong co thanh do an). */
    private static final float REGEN_INTERVAL = 4f;
    /** Ra khoi nuoc thi lay lai hoi nhanh gap 5 lan luc mat. */
    private static final float AIR_REFILL_SPEED = 5f;

    private int health = MAX_HEALTH;
    private float air = MAX_AIR;
    private int food = MAX_FOOD;
    private float exhaustion;
    private float starveTimer;

    /** Cap va phan tram thanh kinh nghiem mau xanh. */
    private int level;
    private float progress;

    private boolean airborne;
    private float fallPeakY;
    private float drownTimer;
    private float regenTimer;
    /** Do do cua vet nhap nhay khi vua an don, 0..1. */
    private float damageFlash;
    private int lastDamage;

    /**
     * Cap nhat mot khung hinh.
     *
     * @param headUnderwater dau nguoi choi co ngap trong nuoc khong
     */
    public void update(float delta, GameMode mode, boolean onGround, float feetY,
                       boolean headUnderwater, boolean bodyInWater, boolean moving) {
        damageFlash = Math.max(0f, damageFlash - delta * 2f);

        if (mode.isCreative()) {
            // Sang tao: khong mat mau, khong doi, khong ngat.
            health = MAX_HEALTH;
            air = MAX_AIR;
            food = MAX_FOOD;
            airborne = false;
            return;
        }
        if (isDead()) {
            return;
        }

        updateAir(delta, headUnderwater);
        updateFall(onGround, feetY, bodyInWater);
        updateFood(delta, moving);
        updateRegen(delta);
    }

    /**
     * Do no tut dan khi di lai. Minecraft goi cai dong ho nay la "exhaustion":
     * cu tich du mot muc thi rot mot nac do no.
     */
    private void updateFood(float delta, boolean moving) {
        exhaustion += delta * (moving ? EXHAUSTION_WALKING : EXHAUSTION_IDLE);
        while (exhaustion >= EXHAUSTION_LIMIT) {
            exhaustion -= EXHAUSTION_LIMIT;
            food = Math.max(0, food - 1);
        }

        // Doi lat mat: het do no thi cu vai giay mat mot mau.
        if (food > 0) {
            starveTimer = 0f;
            return;
        }
        starveTimer += delta;
        if (starveTimer >= STARVE_INTERVAL) {
            starveTimer = 0f;
            damage(1);
        }
    }

    /** An mot mon: day do no len. */
    public void eat(int amount) {
        food = Math.min(MAX_FOOD, food + amount);
    }

    /**
     * Cong kinh nghiem. Cang len cap cao thi moi cap cang can nhieu diem hon,
     * giong Minecraft (o day dung cong thuc gon: 7 + cap * 2).
     */
    public void addExperience(int amount) {
        progress += amount / (float) experienceToNextLevel();
        while (progress >= 1f) {
            progress -= 1f;
            level++;
        }
    }

    public int experienceToNextLevel() {
        return 7 + level * 2;
    }

    /** Dem nguoc binh khi khi dau ngap nuoc, het thi bat dau sac. */
    private void updateAir(float delta, boolean headUnderwater) {
        if (headUnderwater) {
            air = Math.max(0f, air - delta);
            if (air > 0f) {
                drownTimer = 0f;
                return;
            }
            drownTimer += delta;
            while (drownTimer >= DROWN_DAMAGE_INTERVAL) {
                drownTimer -= DROWN_DAMAGE_INTERVAL;
                damage(DROWN_DAMAGE);
            }
            return;
        }
        air = Math.min(MAX_AIR, air + delta * AIR_REFILL_SPEED);
        drownTimer = 0f;
    }

    /**
     * Sat thuong roi nga: nho lai diem cao nhat luc dang o tren khong, cham dat thi
     * lay hieu do cao tru di 3 o an toan. Roi xuong nuoc thi khong sao.
     */
    private void updateFall(boolean onGround, float feetY, boolean bodyInWater) {
        if (bodyInWater) {
            airborne = false;
            return;
        }
        if (!onGround) {
            if (!airborne) {
                airborne = true;
                fallPeakY = feetY;
            }
            fallPeakY = Math.max(fallPeakY, feetY);
            return;
        }
        if (airborne) {
            airborne = false;
            int fallDamage = (int) Math.floor(fallPeakY - feetY - SAFE_FALL);
            if (fallDamage > 0) {
                damage(fallDamage);
            }
        }
    }

    private void updateRegen(float delta) {
        // Chi hoi mau khi con no bung va khong con ngat nuoc.
        if (health >= MAX_HEALTH || air < MAX_AIR || food < FOOD_TO_REGEN) {
            regenTimer = 0f;
            return;
        }
        regenTimer += delta;
        if (regenTimer >= REGEN_INTERVAL) {
            regenTimer = 0f;
            health = Math.min(MAX_HEALTH, health + 1);
        }
    }

    public void damage(int amount) {
        if (amount <= 0 || isDead()) {
            return;
        }
        health = Math.max(0, health - amount);
        lastDamage = amount;
        damageFlash = 1f;
        regenTimer = 0f;
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + Math.max(0, amount));
    }

    /** Hoi sinh: day lai mau va khi nhu luc moi vao game. */
    public void respawn() {
        health = MAX_HEALTH;
        air = MAX_AIR;
        food = MAX_FOOD;
        exhaustion = 0f;
        starveTimer = 0f;
        drownTimer = 0f;
        regenTimer = 0f;
        damageFlash = 0f;
        airborne = false;
    }

    public void kill() {
        health = 0;
        damageFlash = 1f;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public int health() {
        return health;
    }

    public int food() {
        return food;
    }

    public int level() {
        return level;
    }

    /** Phan tram da day cua thanh kinh nghiem, 0..1. */
    public float progress() {
        return progress;
    }

    public float air() {
        return air;
    }

    /** true khi binh khi chua day - luc do moi ve day bong bong len man hinh. */
    public boolean showAir() {
        return air < MAX_AIR - 0.01f;
    }

    public float damageFlash() {
        return damageFlash;
    }

    public int lastDamage() {
        return lastDamage;
    }
}
