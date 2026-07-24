package com.voxel.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.voxel.game.net.BackendClient;
import com.voxel.game.net.Session;

/**
 * Man hinh dang nhap / dang ky hien NGAY TRONG game (khong phai trang web). Nguoi choi
 * nhap ten + mat khau, game goi backend; dang nhap dung thi tai the gioi da xay va chuyen
 * sang {@link GameScreen}.
 *
 * Giao dien ve theo dung "chat" Minecraft va cung tong mau voi giao dien trong game
 * (than chi + diem nhan): tam PANEL vat canh noi len giua nen gach toi, o nhap chu
 * LOM xuong nhu o do trong tui, nut co ba trang thai thuong / re chuot / dang bam.
 * Moi thu deu dung bang code tu Pixmap - khong can them file anh nao.
 */
public final class LoginScreen extends ScreenAdapter {

    // Bang mau dung chung voi MinecraftUi trong game cho dong bo.
    private static final int PANEL_BG = 0x2E2C37;
    private static final int PANEL_LIGHT = 0x76747C;
    private static final int PANEL_DARK = 0x131319;
    private static final int SLOT_BG = 0x1B1A21;
    private static final int GREEN = 0x2C8A42;
    private static final int GREEN_LIGHT = 0x49B267;
    private static final int GREEN_DARK = 0x1D5E2C;
    private static final int BLUE = 0x3D66A8;
    private static final int BLUE_LIGHT = 0x5C89D0;
    private static final int BLUE_DARK = 0x274471;

    private final Game game;
    private final BackendClient backend = new BackendClient();

    private Stage stage;
    private BitmapFont font;
    private Texture menuBg;
    /** Moi texture ve bang code gom vao day de dispose mot the. */
    private final Array<Texture> generated = new Array<Texture>();
    private TextField usernameField;
    private TextField passwordField;
    private TextField codeField;
    private Label statusLabel;
    private TextButton loginButton;
    private TextButton registerButton;
    private boolean busy;

    public LoginScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(false);
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        font = new BitmapFont();
        font.getData().setScale(1.4f);
        addTiledBackground();

        usernameField = textField("");
        passwordField = textField("");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        // Ma the gioi: ai go cung ma thi vao cung map choi chung. Mac dinh "123".
        codeField = textField("123");

        statusLabel = new Label("", new Label.LabelStyle(font, new Color(1f, 0.8f, 0.4f, 1f)));
        statusLabel.setWrap(true);
        statusLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        loginButton = textButton("Play", GREEN, GREEN_LIGHT, GREEN_DARK);
        registerButton = textButton("Create account", BLUE, BLUE_LIGHT, BLUE_DARK);

        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submit(false);
            }
        });
        registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submit(true);
            }
        });

        Label title = new Label("VOXEL ENGINE", new Label.LabelStyle(font, Color.WHITE));
        title.setFontScale(2.6f);
        Label hint = new Label("Survive - Build - Play with friends",
                new Label.LabelStyle(font, new Color(0.72f, 0.70f, 0.80f, 1f)));

        // Tam panel vat canh o giua: moi thu trong form nam tren tam nay.
        Table panel = new Table();
        panel.setBackground(bevelPanel(PANEL_BG, PANEL_LIGHT, PANEL_DARK));
        panel.pad(28f, 34f, 24f, 34f);
        panel.add(new Label("Username", labelStyle())).left().padBottom(4f).row();
        panel.add(usernameField).width(360f).height(44f).padBottom(14f).row();
        panel.add(new Label("Password", labelStyle())).left().padBottom(4f).row();
        panel.add(passwordField).width(360f).height(44f).padBottom(14f).row();
        panel.add(new Label("Map code (friends type the same code to play together)", labelStyle()))
                .left().padBottom(4f).row();
        panel.add(codeField).width(360f).height(44f).padBottom(20f).row();

        Table buttons = new Table();
        buttons.add(loginButton).width(174f).height(48f).padRight(12f);
        buttons.add(registerButton).width(174f).height(48f);
        panel.add(buttons).padBottom(12f).row();
        panel.add(statusLabel).width(360f).height(52f).row();

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.add(title).padBottom(4f).row();
        root.add(hint).padBottom(24f).row();
        root.add(panel).row();
        stage.addActor(root);

        // Chan trang: ten ban dung + huong dan nho, nhu man hinh chinh cua Minecraft.
        Table footer = new Table();
        footer.setFillParent(true);
        footer.bottom();
        Label.LabelStyle small = new Label.LabelStyle(font, new Color(0.55f, 0.55f, 0.62f, 1f));
        footer.add(new Label("Voxel Engine 1.0 - DSA Project", small)).left().expandX().pad(8f);
        footer.add(new Label("Press ENTER to log in", small)).right().pad(8f);
        stage.addActor(footer);

        // Vao man hinh la hien dan len cho diu mat.
        root.getColor().a = 0f;
        root.addAction(Actions.fadeIn(0.35f));

        // Bam ENTER o bat ky o nao la dang nhap luon, khong phai voi chuot toi nut.
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                    submit(false);
                    return true;
                }
                return false;
            }
        });

        stage.setKeyboardFocus(usernameField);
        autoLoginIfConfigured();
    }

    /**
     * Che do DEMO: neu co bien moi truong VOXEL_AUTO_USER thi tu dien va tu vao game luon
     * (khong can go tay) - tien de mo nhieu cua so cung vao mot map. Nguoi thuong khong dat
     * bien nay nen luon phai dang nhap binh thuong.
     */
    private void autoLoginIfConfigured() {
        final String user = System.getenv("VOXEL_AUTO_USER");
        if (user == null || user.isBlank()) {
            return;
        }
        final String pass = System.getenv("VOXEL_AUTO_PASS");
        String envCode = System.getenv("VOXEL_AUTO_CODE");
        final String code = (envCode == null || envCode.isBlank()) ? "123" : envCode.trim();

        usernameField.setText(user);
        passwordField.setText(pass == null ? "" : pass);
        codeField.setText(code);
        setBusy(true);
        setStatus("Auto-joining map " + code + " (demo)...", false);

        // Thu dang ky truoc; neu tai khoan da co thi chuyen sang dang nhap.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Session s;
                try {
                    s = backend.register(user, pass, code);
                } catch (RuntimeException notNew) {
                    try {
                        s = backend.login(user, pass, code);
                    } catch (final RuntimeException failed) {
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                setBusy(false);
                                setStatus(failed.getMessage(), true);
                            }
                        });
                        return;
                    }
                }
                final Session session = s;
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        enterGame(session);
                    }
                });
            }
        }, "voxel-auto").start();
    }

    /** Gui yeu cau dang nhap (register=false) hoac dang ky (register=true). */
    private void submit(final boolean register) {
        if (busy) {
            return;
        }
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText();
        String typedCode = codeField.getText().trim();
        final String code = typedCode.isEmpty() ? "123" : typedCode;
        if (username.isEmpty() || password.isEmpty()) {
            setStatus("Please fill in both username and password", true);
            return;
        }

        setBusy(true);
        setStatus("Connecting to the server...", false);

        // Goi mang tren luong rieng de KHONG treo khung hinh. Xong thi quay lai luong game
        // bang postRunnable de doi man hinh mot cach an toan.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Session session = register
                            ? backend.register(username, password, code)
                            : backend.login(username, password, code);
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            enterGame(session);
                        }
                    });
                } catch (final RuntimeException failure) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            setBusy(false);
                            setStatus(failure.getMessage(), true);
                        }
                    });
                }
            }
        }, "voxel-login").start();
    }

    private void enterGame(Session session) {
        game.setScreen(new GameScreen(session));
        dispose();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        loginButton.setDisabled(busy);
        registerButton.setDisabled(busy);
    }

    private void setStatus(String message, boolean warning) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.getStyle().fontColor = warning ? new Color(1f, 0.5f, 0.4f, 1f)
                : new Color(0.6f, 0.85f, 0.6f, 1f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.09f, 0.10f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    // ----------------------------------------------------------- dung giao dien

    /**
     * Nen menu lat gach toi - hoc tu goi Better MC (Mandalas GUI Background). Them TRUOC form
     * nen no nam duoi cung; lam toi lai (~30%) cho khop tong charcoal cua giao dien va de doc chu.
     */
    private void addTiledBackground() {
        menuBg = new Texture(Gdx.files.internal("data/menu_bg.png"));
        menuBg.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        menuBg.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        TiledDrawable tiled = new TiledDrawable(new TextureRegion(menuBg))
                .tint(new Color(0.30f, 0.29f, 0.34f, 1f));
        tiled.setScale(3f);

        Table background = new Table();
        background.setFillParent(true);
        background.setBackground(tiled);
        stage.addActor(background);
    }

    private Label.LabelStyle labelStyle() {
        return new Label.LabelStyle(font, new Color(0.8f, 0.8f, 0.85f, 1f));
    }

    /** O nhap chu: nen toi LOM xuong (canh tren toi, canh duoi sang) nhu o do trong tui. */
    private TextField textField(String text) {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.background = pad(bevelPanel(SLOT_BG, PANEL_DARK, PANEL_LIGHT));
        style.cursor = solid(0xFFFFFF);
        style.selection = solidAlpha(0x4D80CC, 0.6f);
        return new TextField(text, style);
    }

    /**
     * Nut kieu Minecraft: than mau chinh vat canh (tren sang duoi toi), re chuot vao thi
     * SANG len, bam xuong thi dao nguoc canh nhu bi an lom, bi khoa thi xam lai.
     */
    private TextButton textButton(String text, int body, int light, int dark) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.overFontColor = new Color(1f, 1f, 0.85f, 1f);
        style.disabledFontColor = new Color(0.7f, 0.7f, 0.7f, 1f);
        style.up = bevelPanel(body, light, dark);
        style.over = bevelPanel(lighten(body), lighten(light), dark);
        style.down = bevelPanel(dark, PANEL_DARK, light);
        style.disabled = bevelPanel(0x4A4A50, 0x5C5C62, 0x333338);
        return new TextButton(text, style);
    }

    /**
     * Tam panel vat canh kieu Minecraft ve bang mot Pixmap 8x8 keo gian (NinePatch):
     * vien den ngoai cung, canh tren-trai mau {@code light}, canh duoi-phai mau {@code dark}
     * nen trong nhu mieng kim loai noi len. Dao light/dark cho nhau thi thanh o LOM xuong.
     */
    private Drawable bevelPanel(int fill, int light, int dark) {
        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        pixmap.setColor(rgb(fill));
        pixmap.fillRectangle(1, 1, 6, 6);
        pixmap.setColor(rgb(light));
        pixmap.fillRectangle(1, 1, 6, 1);
        pixmap.fillRectangle(1, 1, 1, 6);
        pixmap.setColor(rgb(dark));
        pixmap.fillRectangle(1, 6, 6, 1);
        pixmap.fillRectangle(6, 1, 1, 6);

        Texture texture = upload(pixmap);
        return new NinePatchDrawable(new NinePatch(texture, 3, 3, 3, 3));
    }

    private Drawable solid(int hex) {
        return solidAlpha(hex, 1f);
    }

    private Drawable solidAlpha(int hex, float alpha) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        Color color = rgb(hex);
        pixmap.setColor(color.r, color.g, color.b, alpha);
        pixmap.fill();
        return new TextureRegionDrawable(new TextureRegion(upload(pixmap)));
    }

    private Texture upload(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        generated.add(texture);
        return texture;
    }

    /** Pha them trang cho mau (dung cho trang thai re chuot vao nut). */
    private static int lighten(int hex) {
        int r = Math.min(255, ((hex >> 16) & 0xFF) + 28);
        int g = Math.min(255, ((hex >> 8) & 0xFF) + 28);
        int b = Math.min(255, (hex & 0xFF) + 28);
        return (r << 16) | (g << 8) | b;
    }

    private static Color rgb(int hex) {
        return new Color(((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f, 1f);
    }

    /** Them le trong cho o nhap chu de con tro khong dinh sat mep. */
    private Drawable pad(Drawable base) {
        base.setLeftWidth(12f);
        base.setRightWidth(12f);
        base.setTopHeight(10f);
        base.setBottomHeight(10f);
        return base;
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (menuBg != null) {
            menuBg.dispose();
        }
        for (Texture texture : generated) {
            texture.dispose();
        }
        generated.clear();
    }
}
