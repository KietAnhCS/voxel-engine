package com.voxel.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.voxel.game.net.BackendClient;
import com.voxel.game.net.Session;

/**
 * Man hinh dang nhap / dang ky hien NGAY TRONG game (khong phai trang web). Nguoi choi
 * nhap ten + mat khau, game goi backend; dang nhap dung thi tai the gioi da xay va chuyen
 * sang {@link GameScreen}.
 *
 * Giao dien dung Scene2D voi "skin" dung bang code (font mac dinh + vai o mau) nen khong
 * can them file anh nao.
 */
public final class LoginScreen extends ScreenAdapter {

    private final Game game;
    private final BackendClient backend = new BackendClient();

    private Stage stage;
    private BitmapFont font;
    private Texture pixel;
    private Texture menuBg;
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
        pixel = whitePixel();
        addTiledBackground();
        Drawable dark = tint(0.16f, 0.17f, 0.20f, 1f);

        usernameField = textField("", dark);
        passwordField = textField("", dark);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        // Ma the gioi: ai go cung ma thi vao cung map choi chung. Mac dinh "123".
        codeField = textField("123", dark);

        statusLabel = new Label("", new Label.LabelStyle(font, new Color(1f, 0.8f, 0.4f, 1f)));
        statusLabel.setWrap(true);

        loginButton = textButton("Login", tint(0.20f, 0.55f, 0.30f, 1f), tint(0.14f, 0.40f, 0.22f, 1f));
        registerButton = textButton("Register", tint(0.24f, 0.40f, 0.62f, 1f), tint(0.16f, 0.28f, 0.46f, 1f));

        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                submit(false);
            }
        });
        registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                submit(true);
            }
        });

        Label title = new Label("VOXEL ENGINE", new Label.LabelStyle(font, Color.WHITE));
        title.setFontScale(2.4f);
        Label hint = new Label("Login to play together",
                new Label.LabelStyle(font, new Color(0.7f, 0.7f, 0.75f, 1f)));

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.add(title).padBottom(6f).row();
        table.add(hint).padBottom(28f).row();
        table.add(new Label("Username", labelStyle())).left().padBottom(4f).row();
        table.add(usernameField).width(360f).height(44f).padBottom(16f).row();
        table.add(new Label("Password", labelStyle())).left().padBottom(4f).row();
        table.add(passwordField).width(360f).height(44f).padBottom(16f).row();
        table.add(new Label("MMap Code", labelStyle())).left().padBottom(4f).row();
        table.add(codeField).width(360f).height(44f).padBottom(22f).row();

        Table buttons = new Table();
        buttons.add(loginButton).width(174f).height(48f).padRight(12f);
        buttons.add(registerButton).width(174f).height(48f);
        table.add(buttons).padBottom(18f).row();

        table.add(statusLabel).width(360f).height(60f).row();
        stage.addActor(table);

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
            setStatus("Please fill in all fields", true);
            return;
        }

        setBusy(true);
        setStatus("Connecting to server...", false);

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

    private TextField textField(String text, Drawable background) {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.background = pad(background);
        style.cursor = tint(1f, 1f, 1f, 1f);
        style.selection = tint(0.3f, 0.5f, 0.8f, 0.6f);
        return new TextField(text, style);
    }

    private TextButton textButton(String text, Drawable up, Drawable down) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.disabledFontColor = new Color(0.7f, 0.7f, 0.7f, 1f);
        style.up = up;
        style.down = down;
        style.disabled = tint(0.3f, 0.3f, 0.3f, 1f);
        return new TextButton(text, style);
    }

    private Texture whitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Drawable tint(float r, float g, float b, float a) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(pixel));
        return drawable.tint(new Color(r, g, b, a));
    }

    /** Them le trong cho o nhap chu de con tro khong dinh sat mep. */
    private Drawable pad(Drawable base) {
        base.setLeftWidth(10f);
        base.setRightWidth(10f);
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
        if (pixel != null) {
            pixel.dispose();
        }
        if (menuBg != null) {
            menuBg.dispose();
        }
    }
}
