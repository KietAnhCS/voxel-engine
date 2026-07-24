package com.voxel.game.net;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Ket noi WebSocket toi the gioi chung. Gui vi tri + o khoi cua NGUOI CHOI NAY len server,
 * va nhan ve dong tac cua nguoi khac.
 *
 * Hai loai du lieu nhan ve xu ly khac nhau:
 *   - Vi tri nguoi choi khac -> ghi thang vao {@link RemotePlayers} (chi la vai con so, an toan).
 *   - O khoi nguoi khac dat/pha, hoac cu danh minh vua an -> XEP HANG doi, vi sua the gioi va
 *     tru mau deu phai lam tren luong game, khong lam duoc tren luong WebSocket.
 *
 * Neu ket noi that bai (server tat, mat mang) thi client chay o che do "cam" - game van choi
 * mot minh binh thuong, chi la khong thay ai khac.
 */
public final class WorldClient implements HitSender {

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonReader json = new JsonReader();
    private final URI uri;

    private final RemotePlayers players = new RemotePlayers(this);
    private final ConcurrentLinkedQueue<int[]> pendingEdits = new ConcurrentLinkedQueue<int[]>();
    private final ConcurrentLinkedQueue<Hurt> pendingHurts = new ConcurrentLinkedQueue<Hurt>();

    private volatile WebSocket socket;
    /** Cac lan gui noi tiep nhau: WebSocket cam goi sendText khi lan truoc chua xong. */
    private CompletableFuture<WebSocket> sendChain = CompletableFuture.completedFuture(null);

    public WorldClient(Session session) {
        String code = java.net.URLEncoder.encode(session.code, java.nio.charset.StandardCharsets.UTF_8);
        this.uri = URI.create(socketBase() + "/ws/world?token=" + session.token + "&code=" + code);
    }

    /**
     * Mo ket noi (cho toi 5 giay). Neu that bai chi in canh bao roi bo qua - game van chay,
     * chi la khong co ban be.
     */
    public void connect() {
        try {
            socket = http.newWebSocketBuilder()
                    .buildAsync(uri, new Listener())
                    .get(5, TimeUnit.SECONDS);
            sendChain = CompletableFuture.completedFuture(socket);
        } catch (Exception failed) {
            socket = null;
            System.err.println("Khong ket noi duoc the gioi chung: " + failed.getMessage());
        }
    }

    /** Gui vi tri chan + huong nhin cua nguoi choi nay len server (goi deu dan, da throttle o GameScreen). */
    public void sendMove(float x, float y, float z, float yaw, float pitch) {
        send("{\"t\":\"move\",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z
                + ",\"yaw\":" + yaw + ",\"pitch\":" + pitch + "}");
    }

    /** Gui mot o khoi vua dat/pha len server (blockId = 0 la pha thanh khong khi). */
    public void sendEdit(int x, int y, int z, int blockId) {
        send("{\"t\":\"edit\",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z + ",\"b\":" + blockId + "}");
    }

    /**
     * Gui cu danh len server: minh vua dam trung {@code victim}. Server chuyen tiep cho dung
     * may cua nguoi do tru mau, va bao ca phong biet minh vua quo tay.
     */
    @Override
    public void sendHit(String victim, int damage) {
        send("{\"t\":\"hit\",\"name\":\"" + escape(victim) + "\",\"dmg\":" + damage + "}");
    }

    /** Bao ca phong: minh vua quo tay danh mot cai (de avatar cua minh ben may ho vung tay theo). */
    public void sendSwing() {
        send("{\"t\":\"swing\"}");
    }

    /** Cac o khoi nguoi khac vua sua, cho luong game lay ra ap vao the gioi. Tra ve null khi het. */
    public int[] pollRemoteEdit() {
        return pendingEdits.poll();
    }

    /** Cu danh minh vua an tu nguoi choi khac, cho luong game lay ra tru mau. Null khi het. */
    public Hurt pollHurt() {
        return pendingHurts.poll();
    }

    public RemotePlayers players() {
        return players;
    }

    public void close() {
        WebSocket ws = socket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    /** Noi lan gui moi vao cuoi chuoi de khong bao gio goi sendText khi lan truoc chua xong. */
    private synchronized void send(final String text) {
        if (socket == null) {
            return;
        }
        sendChain = sendChain.thenCompose(new java.util.function.Function<WebSocket, CompletableFuture<WebSocket>>() {
            @Override
            public CompletableFuture<WebSocket> apply(WebSocket ws) {
                return ws == null ? CompletableFuture.completedFuture(null) : ws.sendText(text, true);
            }
        }).exceptionally(new java.util.function.Function<Throwable, WebSocket>() {
            @Override
            public WebSocket apply(Throwable error) {
                return socket; // Gui loi mot lan thi bo qua, giu chuoi song cho lan sau.
            }
        });
    }

    /** Doc tin JSON tu server va cap nhat trang thai. Cac callback chay tren luong WebSocket. */
    private void handle(String message) {
        JsonValue root = json.parse(message);
        String type = root.getString("t", "");
        if ("player".equals(type)) {
            players.update(root.getString("name"),
                    root.getFloat("x"), root.getFloat("y"), root.getFloat("z"), root.getFloat("yaw"));
        } else if ("edit".equals(type)) {
            pendingEdits.add(new int[]{root.getInt("x"), root.getInt("y"), root.getInt("z"), root.getInt("b")});
            // Ai dat/pha khoi thi avatar cua ho quo tay mot cai cho song dong.
            players.swing(root.getString("name", ""));
        } else if ("swing".equals(type)) {
            players.swing(root.getString("name", ""));
        } else if ("hurt".equals(type)) {
            pendingHurts.add(new Hurt(root.getString("from", "Ai do"), root.getInt("dmg", 0)));
        } else if ("leave".equals(type)) {
            players.remove(root.getString("name"));
        }
    }

    /** Ten tai khoan co the chua dau nhay - boc lai cho chuoi JSON khong vo. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Dia chi WebSocket suy ra tu VOXEL_SERVER_URL (http -> ws, https -> wss). */
    private static String socketBase() {
        String url = System.getenv("VOXEL_SERVER_URL");
        String base = (url == null || url.isBlank()) ? "http://localhost:8080" : url.trim();
        if (base.startsWith("https://")) {
            return "wss://" + base.substring("https://".length());
        }
        if (base.startsWith("http://")) {
            return "ws://" + base.substring("http://".length());
        }
        return base;
    }

    /**
     * Nhan du lieu tu server. Tin nhan co the toi thanh nhieu manh, nen ta gom lai den khi
     * {@code last} la true roi moi doc tron mot tin.
     */
    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String complete = buffer.toString();
                buffer.setLength(0);
                try {
                    handle(complete);
                } catch (RuntimeException ignore) {
                    // Tin loi thi bo qua, khong lam sap game.
                }
            }
            webSocket.request(1);
            return null;
        }
    }
}
