package com.voxel.game.play;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.voxel.engine.VoxelEngine;
import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.combat.Attackable;
import com.voxel.game.combat.MeleeAim;
import com.voxel.game.mob.Monster;
import com.voxel.game.mob.MonsterManager;
import com.voxel.game.mob.PlayerTarget;

import java.util.Collection;
import java.util.Map;

/**
 * Phan "luat choi" nam tren engine: che do choi, mau, tui do, khung lenh va toan bo
 * giao dien 2D. Engine chi lo the gioi khoi vuong va vat ly, con lop nay quyet dinh
 * nguoi choi mat mau ra sao, dat duoc khoi gi.
 *
 * No cung la mot InputProcessor: dat TRUOC {@code PlayerController} trong
 * InputMultiplexer nen khi tui do hay khung chat dang mo thi phim bam thuoc ve giao
 * dien, khong lot xuong dieu khien nhan vat nua.
 */
public final class PlaySession extends InputAdapter implements Disposable, PlayerTarget {

    /** Tam voi cua cu dam tay khong (khoi), lay theo Minecraft. */
    private static final float ATTACK_REACH = 3.5f;
    /** Moi cu dam tru bay nhieu mau: zombie 20 mau -> bon cu la ha. */
    private static final int PUNCH_DAMAGE = 5;
    /** Nghi tay giua hai cu danh (giay) - khong cho bam lien tay. */
    private static final float ATTACK_COOLDOWN = 0.35f;
    /** Ha mot con quai duoc bay nhieu diem kinh nghiem. */
    private static final int KILL_EXPERIENCE = 5;
    /** Pha mot o la cay thi bao nhieu phan tram rot ra qua tao. */
    private static final float LEAF_DROP_CHANCE = 0.3f;
    /** Giu chuot phai bao lau thi an xong mot mon (giay). */
    private static final float EAT_TIME = 1.2f;

    private final VoxelEngine engine;
    private final Blocks blocks;
    private final PlayerStats stats = new PlayerStats();
    private final Inventory inventory = new Inventory();
    private final CommandConsole console = new CommandConsole();
    private final MinecraftUi ui = new MinecraftUi();
    private final Crafting crafting;
    private final ItemRenderer items;
    private final InventoryScreen inventoryScreen;
    private final SettingsScreen settingsScreen;
    private final Hud hud;
    /** He quai vat (chi hoat dong o sinh ton). PlaySession dong vai "nguoi choi" cho no danh. */
    private final MonsterManager monsters;
    /** Bo ngam cu dam: tim sinh vat gan nhat nam trong tam ngam. */
    private final MeleeAim aim = new MeleeAim();
    /** Vi tri ban chan nguoi choi, tinh lai moi lan quai vat hoi den. */
    private final Vector3 playerFeet = new Vector3();

    /** Bo sinh so ngau nhien cho ti le rot do khi pha khoi. */
    private final java.util.Random random = new java.util.Random();

    private GameMode mode = GameMode.CREATIVE;
    /** Con bao lau nua moi duoc danh cu tiep. */
    private float attackCooldown;
    /** Da giu chuot phai de an duoc bao lau. */
    private float eatTimer;
    private boolean debugVisible = true;
    /** Nuot ky tu ngay sau khi mo khung chat, keo phim mo lai lot vao o go chu. */
    private boolean swallowNextTyped;

    public PlaySession(VoxelEngine engine, Blocks blocks, TextureAtlas atlas, BitmapFont font) {
        this.engine = engine;
        this.blocks = blocks;
        this.crafting = new Crafting(blocks);
        this.items = new ItemRenderer(atlas, ui, font);
        this.inventoryScreen = new InventoryScreen(inventory, crafting, blocks.palette(), items, font);
        this.settingsScreen = new SettingsScreen(ui, font);
        this.hud = new Hud(inventory, stats, console, items, font);
        this.monsters = new MonsterManager(engine.world(), blocks, this);

        registerCommands();
        // Vao game la SINH TON, tui do trong tron - tu di dao lay khoi ma xay.
        setMode(GameMode.SURVIVAL);

        // Moc thu giao dien: dat VOXEL_UI_TEST=inventory de tui do tu mo ngay khi vao game
        // (dung de chup anh kiem tra giao dien tu dong, nguoi choi thuong khong dat bien nay).
        if ("inventory".equals(System.getenv("VOXEL_UI_TEST"))) {
            inventoryScreen.open();
            captureControls();
        }
        console.log("Type /help for commands. Press E to open your inventory.");
    }

    // ------------------------------------------------------------------ lenh

    private void registerCommands() {
        Command gamemode = new Command() {
            @Override
            public String run(String[] args) {
                if (args.length == 0) {
                    return "Current mode: " + mode.label() + " (" + mode.id() + ")";
                }
                GameMode target = GameMode.parse(args[0]);
                if (target == null) {
                    return "Unknown mode: " + args[0] + " (use 0 or 1)";
                }
                setMode(target);
                return "Switched to " + target.label() + " mode";
            }

            @Override
            public String usage() {
                return "/gamemode <0|1>  - 0 survival, 1 creative";
            }
        };
        console.register("gamemode", gamemode);
        console.register("gm", gamemode);

        console.register("time", new Command() {
            @Override
            public String run(String[] args) {
                if (args.length == 0) {
                    return "The time is " + engine.dayCycle().clockLabel();
                }
                String when = args[0].toLowerCase();
                if (when.equals("day") || when.equals("ngay")) {
                    engine.dayCycle().setTime(0.25f);
                } else if (when.equals("night") || when.equals("dem")) {
                    engine.dayCycle().setTime(0.75f);
                } else if (when.equals("dawn") || when.equals("binhminh")) {
                    engine.dayCycle().setTime(0f);
                } else if (when.equals("dusk") || when.equals("hoanghon")) {
                    engine.dayCycle().setTime(0.5f);
                } else {
                    return "Usage: /time day|night|dawn|dusk";
                }
                return "Time set to " + engine.dayCycle().clockLabel();
            }

            @Override
            public String usage() {
                return "/time <day|night|dawn|dusk>  - change the time of day";
            }
        });

        console.register("weather", new Command() {
            @Override
            public String run(String[] args) {
                if (args.length == 0) {
                    return "Weather: " + (engine.weather().isRaining() ? "rain" : "clear");
                }
                String wanted = args[0].toLowerCase();
                if (wanted.equals("rain")) {
                    engine.weather().setRaining(true);
                    return "Rain is coming...";
                }
                if (wanted.equals("clear")) {
                    engine.weather().setRaining(false);
                    return "The sky is clearing up";
                }
                return "Usage: /weather rain|clear";
            }

            @Override
            public String usage() {
                return "/weather <rain|clear>  - change the weather";
            }
        });

        console.register("heal", new Command() {
            @Override
            public String run(String[] args) {
                stats.heal(PlayerStats.MAX_HEALTH);
                return "Fully healed";
            }

            @Override
            public String usage() {
                return "/heal  - restore full health";
            }
        });

        console.register("kill", new Command() {
            @Override
            public String run(String[] args) {
                stats.kill();
                return null;
            }

            @Override
            public String usage() {
                return "/kill  - end it all";
            }
        });

        console.register("clear", new Command() {
            @Override
            public String run(String[] args) {
                inventory.clear();
                return "Inventory cleared";
            }

            @Override
            public String usage() {
                return "/clear  - empty your inventory";
            }
        });

        console.register("help", new Command() {
            @Override
            public String run(String[] args) {
                for (Map.Entry<String, Command> entry : console.commands().entrySet()) {
                    // Lenh viet tat dung chung doi tuong voi lenh day du, khong in lai.
                    if (entry.getValue().usage().startsWith("/" + entry.getKey())) {
                        console.log(entry.getValue().usage());
                    }
                }
                return null;
            }

            @Override
            public String usage() {
                return "/help  - list all commands";
            }
        });
    }

    public void setMode(GameMode mode) {
        this.mode = mode;
        // Chi che do sang tao moi bay duoc; doi ve sinh ton la roi xuong dat ngay.
        engine.setFlightAllowed(mode.isCreative());
        if (mode.isCreative()) {
            stats.respawn();
        }
    }

    public GameMode mode() {
        return mode;
    }

    /** Khung chat / lenh - de ben ngoai noi chat mang va dang ky them lenh (vd /list). */
    public CommandConsole console() {
        return console;
    }

    /** Cho GameScreen dang ky duong bao "khoi vua bi no pha" de dong bo len server. */
    public void setExplosionListener(MonsterManager.ExplosionListener listener) {
        monsters.setExplosionListener(listener);
    }

    /** Mot diem anh trang de ben ngoai ve nen chu. */
    public com.badlogic.gdx.graphics.Texture pixel() {
        return ui.white;
    }

    public boolean isDebugVisible() {
        return debugVisible;
    }

    // ------------------------------------------------------------- moi khung

    public void update(float delta) {
        console.update(delta);
        stats.update(delta, mode,
                engine.playerOnGround(),
                engine.playerPosition().y,
                engine.isSubmerged(),
                engine.playerInWater(),
                engine.isPlayerMoving());

        attackCooldown = Math.max(0f, attackCooldown - delta);
        updateEating(delta);

        // Quai vat: chi lung suc o sinh ton va khi troi toi (xem MonsterManager).
        monsters.update(delta, mode.isSurvival(), engine.dayCycle().isNight());

        // Ha duoc con nao thi bao va cong kinh nghiem.
        int killed = monsters.takeKills();
        for (int i = 0; i < killed; i++) {
            stats.addExperience(KILL_EXPERIENCE);
            console.log("Monster slain! +" + KILL_EXPERIENCE + " XP");
        }

        // Chet thi cung do nhu dang mo giao dien: khong di chuyen, hien con tro chuot.
        if (stats.isDead() && engine.controller().isEnabled()) {
            captureControls();
        }
    }

    /** Ve quai vat trong khong gian 3D - goi tu GameScreen sau khi engine ve xong the gioi. */
    public void renderMonsters(PerspectiveCamera camera) {
        monsters.render(camera);
    }

    // ------------------------------------------------------------------ danh nhau

    /**
     * Chuot trai o che do SINH TON: dam sinh vat gan nhat dang nam trong tam ngam - quai vat
     * hoac nguoi choi khac deu duoc, ai dung gan hon thi an don.
     *
     * <p>Che do sang tao thi khong danh ai (chi pha khoi), giong Minecraft.
     *
     * @param others nhung nguoi choi khac dang o quanh day
     * @return true neu danh trung - luc do engine khong pha khoi nua
     */
    public boolean attack(Vector3 origin, Vector3 direction, float reach,
                          Collection<? extends Attackable> others) {
        if (!mode.isSurvival() || stats.isDead() || attackCooldown > 0f) {
            return false;
        }

        aim.aimFrom(origin, direction, Math.min(reach, ATTACK_REACH));
        for (Monster monster : monsters.alive()) {
            aim.consider(monster);
        }
        for (Attackable other : others) {
            aim.consider(other);
        }

        Attackable target = aim.target();
        if (target == null) {
            return false;
        }
        attackCooldown = ATTACK_COOLDOWN;
        target.takeHit(PUNCH_DAMAGE);
        return true;
    }

    /** Minh vua bi mot nguoi choi khac danh (tin bao ve tu server). */
    public void hurtBy(String attacker, int damage) {
        if (!mode.isSurvival() || stats.isDead() || damage <= 0) {
            return;
        }
        stats.damage(damage);
        console.log(attacker + " hit you for " + damage + " damage!");
    }

    // -------------------------------------------------- PlayerTarget (goc nhin cua quai vat)

    /**
     * Vi tri BAN CHAN nguoi choi. Quai vat cung lay moc o ban chan, nho vay so sanh cao do
     * giua hai ben moi dung - va bo tim duong A* cung nham dung o khoi nguoi choi dang dung.
     */
    @Override
    public Vector3 position() {
        Vector3 body = engine.playerPosition();
        playerFeet.set(body.x, engine.playerFeetY(), body.z);
        return playerFeet;
    }

    @Override
    public void hit(int damage) {
        stats.damage(damage);
    }

    @Override
    public boolean isDead() {
        return stats.isDead();
    }

    public void draw(SpriteBatch batch, int width, int height) {
        hud.draw(batch, width, height, mode, inventoryScreen.isOpen());
        inventoryScreen.draw(batch, width, height, mode);
        inventoryScreen.drawCarried(batch, Gdx.input.getX(), height - Gdx.input.getY());
        settingsScreen.draw(batch, width, height, Gdx.input.getX(), height - Gdx.input.getY());
    }

    /** Con tro ngam chi hien khi khong co giao dien nao che man hinh. */
    public boolean showCrosshair() {
        return !inventoryScreen.isOpen() && !stats.isDead();
    }

    // --------------------------------------------------------- dat / pha khoi

    /**
     * Pha mot khoi. O che do sinh ton thi khoi vua pha roi vao tui do
     * (tru chat long - khong ai nhat duoc nuoc bang tay).
     *
     * <p>Rieng LA CAY thi khong nhat duoc la: chi {@link #LEAF_DROP_CHANCE} co hoi rot ra
     * mot qua tao, con lai la tan bien - giong Minecraft.
     */
    public void onBreak(Block broken) {
        if (!mode.isSurvival() || broken.isAir() || broken.isLiquid()) {
            return;
        }

        Block drop = dropFor(broken);
        if (drop == null) {
            return;
        }
        if (!inventory.add(drop)) {
            console.log("Inventory is full!");
        }
        stats.addExperience(1);
    }

    /** Pha khoi nay thi nhat duoc gi? null la khong duoc gi ca. */
    private Block dropFor(Block broken) {
        if (broken != blocks.leaves && broken != blocks.pineLeaves) {
            return broken;
        }
        return random.nextFloat() < LEAF_DROP_CHANCE ? blocks.apple : null;
    }

    /**
     * Giu CHUOT PHAI de an mon dang cam. Sau {@link #EAT_TIME} giay thi mon do bien mat va
     * do no day len. Bo tay ra giua chung thi phai an lai tu dau.
     */
    private void updateEating(float delta) {
        Block held = inventory.selectedBlock();
        boolean canEat = mode.isSurvival()
                && !stats.isDead()
                && !uiOpen()
                && engine.controller().isEnabled()
                && blocks.isFood(held)
                && stats.food() < PlayerStats.MAX_FOOD
                && Gdx.input.isButtonPressed(Input.Buttons.RIGHT);

        if (!canEat) {
            eatTimer = 0f;
            return;
        }
        eatTimer += delta;
        if (eatTimer < EAT_TIME) {
            return;
        }
        eatTimer = 0f;
        stats.eat(blocks.foodValue(held));
        inventory.consumeSelected();
        console.log("Ate one " + held.name());
    }

    /**
     * Khoi se duoc dat xuong, hoac null neu khong dat duoc (tay khong / het do).
     *
     * @param alternate dang giu CTRL - dat nhanh mot cay duoc
     */
    public Block blockToPlace(boolean alternate) {
        if (stats.isDead()) {
            return null;
        }
        Block wanted = alternate ? blocks.torch : inventory.selectedBlock();
        if (wanted == null || blocks.isFood(wanted)) {
            return null;  // do an thi de an, khong dat xuong dat duoc
        }
        if (mode.isCreative()) {
            return wanted;
        }
        // Sinh ton: phai co san khoi do trong tui moi dat duoc.
        if (alternate) {
            return inventory.consume(wanted) ? wanted : null;
        }
        inventory.consumeSelected();
        return wanted;
    }

    // ------------------------------------------------------------------ input

    private void captureControls() {
        engine.controller().setEnabled(false);
        Gdx.input.setCursorCatched(false);
    }

    private void resumeControls() {
        if (stats.isDead()) {
            return;
        }
        engine.controller().setEnabled(true);
        Gdx.input.setCursorCatched(true);
    }

    private boolean uiOpen() {
        return console.isOpen() || inventoryScreen.isOpen() || settingsScreen.isOpen();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (stats.isDead()) {
            if (keycode == Input.Keys.R) {
                respawn();
            }
            return true;
        }

        if (console.isOpen()) {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                console.submit();
                resumeControls();
            } else if (keycode == Input.Keys.ESCAPE) {
                console.close();
                resumeControls();
            } else if (keycode == Input.Keys.BACKSPACE) {
                console.backspace();
            }
            return true;
        }

        if (inventoryScreen.isOpen()) {
            if (keycode == Input.Keys.E || keycode == Input.Keys.ESCAPE) {
                inventoryScreen.close();
                resumeControls();
            }
            return true;
        }

        if (settingsScreen.isOpen()) {
            if (keycode == Input.Keys.ESCAPE) {
                settingsScreen.close();
                resumeControls();
            }
            return true;
        }

        switch (keycode) {
            case Input.Keys.ESCAPE:
                settingsScreen.open();
                captureControls();
                return true;
            case Input.Keys.E:
                inventoryScreen.open();
                captureControls();
                return true;
            case Input.Keys.SLASH:
                console.open("/");
                swallowNextTyped = true;
                captureControls();
                return true;
            case Input.Keys.T:
                console.open("");
                swallowNextTyped = true;
                captureControls();
                return true;
            case Input.Keys.F3:
                debugVisible = !debugVisible;
                return true;
            case Input.Keys.F5:
                console.log("Goc nhin: " + engine.cycleViewMode().label());
                return true;
            default:
                break;
        }

        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            inventory.select(keycode - Input.Keys.NUM_1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        // Chan luon phim nha ra, neu khong PlayerController se bat duoc ESC / Q / F
        // trong luc dang go chu.
        return uiOpen() || stats.isDead();
    }

    @Override
    public boolean keyTyped(char character) {
        if (!console.isOpen()) {
            return uiOpen() || stats.isDead();
        }
        if (swallowNextTyped) {
            swallowNextTyped = false;
            return true;
        }
        console.type(character);
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (stats.isDead()) {
            // Dang chet thi ca man hinh chi con mot cho bam duoc: o HOI SINH.
            if (hud.respawnButtonHit(screenX, Gdx.graphics.getHeight() - screenY)) {
                respawn();
            }
            return true;
        }
        if (settingsScreen.isOpen()) {
            if (settingsScreen.touchDown(screenX, Gdx.graphics.getHeight() - screenY)) {
                settingsScreen.close();
                resumeControls();
            }
            return true;
        }
        if (inventoryScreen.isOpen()) {
            int height = Gdx.graphics.getHeight();
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            inventoryScreen.touchDown(screenX, height - screenY, Gdx.graphics.getWidth(), height,
                    mode, button == Input.Buttons.RIGHT, shift);
            return true;
        }
        return console.isOpen();
    }

    /** Keo chuot: truot thanh chinh trong settings, hoac chia deu chong do trong tui. */
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (settingsScreen.isOpen()) {
            settingsScreen.touchDragged(screenX);
            return true;
        }
        if (!inventoryScreen.isOpen()) {
            return uiOpen() || stats.isDead();
        }
        int height = Gdx.graphics.getHeight();
        inventoryScreen.touchDragged(screenX, height - screenY, Gdx.graphics.getWidth(), height, mode);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (settingsScreen.isOpen()) {
            settingsScreen.touchUp();
            return true;
        }
        if (!inventoryScreen.isOpen()) {
            return uiOpen() || stats.isDead();
        }
        inventoryScreen.touchUp();
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (uiOpen() || stats.isDead()) {
            return true;
        }
        inventory.scroll(amountY > 0f ? 1 : -1);
        return true;
    }

    private void respawn() {
        stats.respawn();
        engine.respawnPlayer();
        engine.controller().setEnabled(true);
        Gdx.input.setCursorCatched(true);
        console.log("Respawned at the spawn point");
    }

    @Override
    public void dispose() {
        monsters.dispose();
        ui.dispose();
    }
}
