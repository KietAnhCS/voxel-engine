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
import com.voxel.game.mob.MonsterManager;
import com.voxel.game.mob.PlayerTarget;

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

    private final VoxelEngine engine;
    private final Blocks blocks;
    private final PlayerStats stats = new PlayerStats();
    private final Inventory inventory = new Inventory();
    private final CommandConsole console = new CommandConsole();
    private final MinecraftUi ui = new MinecraftUi();
    private final Crafting crafting;
    private final ItemRenderer items;
    private final InventoryScreen inventoryScreen;
    private final Hud hud;
    /** He quai vat (chi hoat dong o sinh ton). PlaySession dong vai "nguoi choi" cho no danh. */
    private final MonsterManager monsters;

    private GameMode mode = GameMode.CREATIVE;
    private boolean debugVisible = true;
    /** Nuot ky tu ngay sau khi mo khung chat, keo phim mo lai lot vao o go chu. */
    private boolean swallowNextTyped;

    public PlaySession(VoxelEngine engine, Blocks blocks, TextureAtlas atlas, BitmapFont font) {
        this.engine = engine;
        this.blocks = blocks;
        this.crafting = new Crafting(blocks);
        this.items = new ItemRenderer(atlas, ui, font);
        this.inventoryScreen = new InventoryScreen(inventory, crafting, blocks.palette(), items, font);
        this.hud = new Hud(inventory, stats, console, items, font);
        this.monsters = new MonsterManager(engine.world(), this);

        registerCommands();
        fillStarterHotbar();
        setMode(GameMode.CREATIVE);
        console.log("Go /help de xem cac lenh. Bam E de mo tui do.");
    }

    // ------------------------------------------------------------------ lenh

    private void registerCommands() {
        Command gamemode = new Command() {
            @Override
            public String run(String[] args) {
                if (args.length == 0) {
                    return "Dang o che do " + mode.label() + " (" + mode.id() + ")";
                }
                GameMode target = GameMode.parse(args[0]);
                if (target == null) {
                    return "Che do khong hop le: " + args[0] + " (dung 0 hoac 1)";
                }
                setMode(target);
                return "Da chuyen sang che do " + target.label();
            }

            @Override
            public String usage() {
                return "/gamemode <0|1>  - 0 sinh ton, 1 sang tao";
            }
        };
        console.register("gamemode", gamemode);
        console.register("gm", gamemode);

        console.register("heal", new Command() {
            @Override
            public String run(String[] args) {
                stats.heal(PlayerStats.MAX_HEALTH);
                return "Da hoi day mau";
            }

            @Override
            public String usage() {
                return "/heal  - hoi day mau";
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
                return "/kill  - tu ket lieu";
            }
        });

        console.register("clear", new Command() {
            @Override
            public String run(String[] args) {
                inventory.clear();
                return "Da do sach tui do";
            }

            @Override
            public String usage() {
                return "/clear  - do sach tui do";
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
                return "/help  - xem danh sach lenh";
            }
        });
    }

    private void fillStarterHotbar() {
        Block[] starter = {blocks.grass, blocks.dirt, blocks.stone, blocks.cobblestone,
                blocks.planks, blocks.brick, blocks.sand, blocks.lamp, blocks.torch};
        for (int slot = 0; slot < starter.length; slot++) {
            inventory.set(slot, new ItemStack(starter[slot], ItemStack.MAX_COUNT));
        }
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

        // Quai vat: chi lung suc o sinh ton, tu duoi va danh nguoi choi (xem MonsterManager).
        monsters.update(delta, mode.isSurvival());

        // Chet thi cung do nhu dang mo giao dien: khong di chuyen, hien con tro chuot.
        if (stats.isDead() && engine.controller().isEnabled()) {
            captureControls();
        }
    }

    /** Ve quai vat trong khong gian 3D - goi tu GameScreen sau khi engine ve xong the gioi. */
    public void renderMonsters(PerspectiveCamera camera) {
        monsters.render(camera);
    }

    // -------------------------------------------------- PlayerTarget (goc nhin cua quai vat)

    @Override
    public Vector3 position() {
        return engine.playerPosition();
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
    }

    /** Con tro ngam chi hien khi khong co giao dien nao che man hinh. */
    public boolean showCrosshair() {
        return !inventoryScreen.isOpen() && !stats.isDead();
    }

    // --------------------------------------------------------- dat / pha khoi

    /**
     * Pha mot khoi. O che do sinh ton thi khoi vua pha roi vao tui do
     * (tru chat long - khong ai nhat duoc nuoc bang tay).
     */
    public void onBreak(Block broken) {
        if (!mode.isSurvival() || broken.isAir() || broken.isLiquid()) {
            return;
        }
        if (!inventory.add(broken)) {
            console.log("Tui do da day!");
        }
        stats.addExperience(1);
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
        if (wanted == null) {
            return null;
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
        return console.isOpen() || inventoryScreen.isOpen();
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

        switch (keycode) {
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
            return true;
        }
        if (inventoryScreen.isOpen()) {
            int height = Gdx.graphics.getHeight();
            inventoryScreen.click(screenX, height - screenY, Gdx.graphics.getWidth(), height, mode);
            return true;
        }
        return console.isOpen();
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
        console.log("Da hoi sinh tai diem xuat phat");
    }

    @Override
    public void dispose() {
        monsters.dispose();
        ui.dispose();
    }
}
