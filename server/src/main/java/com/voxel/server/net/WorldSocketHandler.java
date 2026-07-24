package com.voxel.server.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxel.server.world.WorldRoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trai tim cua che do choi chung: giu danh sach nguoi dang online va chuyen tiep tin nhan
 * giua ho theo thoi gian thuc.
 *
 * Day la MAU MEDIATOR (dieu phoi vien): cac client KHONG biet nhau va khong noi truc tiep
 * voi nhau - moi tin deu di qua handler nay, no quyet dinh phat lai cho ai (cung phong).
 * Nho vay them/bot mot nguoi choi khong lam anh huong nhung nguoi con lai.
 *
 * Tin nhan la JSON co truong "t" (type):
 *   - Nhan tu game:  {"t":"move", x,y,z,yaw,pitch}   ->  phat lai cho nguoi khac kem ten
 *                    {"t":"edit", x,y,z,b}            ->  luu vao the gioi chung + phat lai
 *                    {"t":"hit",  name, dmg}          ->  danh trung ai do: bao RIENG nguoi do
 *                    {"t":"swing"}                    ->  vua quo tay: phat lai cho nguoi khac
 *                    {"t":"chat", msg}                ->  chat: phat lai cho nguoi khac kem ten
 *   - Gui toi game:  {"t":"player", name, x,y,z,yaw,pitch}  ->  ai do di chuyen
 *                    {"t":"edit",   name, x,y,z,b}           ->  ai do dat/pha khoi
 *                    {"t":"swing",  name}                    ->  ai do quo tay
 *                    {"t":"chat",   name, msg}               ->  ai do vua chat
 *                    {"t":"hurt",   from, dmg}               ->  MINH vua bi danh mat mau
 *                    {"t":"leave",  name}                    ->  ai do thoat
 *
 * Toa do o khoi va vi tri nguoi choi la hai he KHAC nhau: "edit" dung so nguyen (o khoi),
 * "move"/"player" dung so thuc (vi tri chan nguoi choi).
 */
@Component
public class WorldSocketHandler extends TextWebSocketHandler {

    private final WorldRoomService world;
    private final ObjectMapper json;

    /** Moi ket noi dang mo, khoa theo id phien WebSocket. */
    private final Map<String, Presence> online = new ConcurrentHashMap<>();

    public WorldSocketHandler(WorldRoomService world, ObjectMapper json) {
        this.world = world;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String username = (String) session.getAttributes().get(JwtHandshakeInterceptor.USERNAME);
        String code = (String) session.getAttributes().get(JwtHandshakeInterceptor.CODE);
        online.put(session.getId(), new Presence(session, username, code));

        // Cho nguoi vua vao thay ngay nhung ai CUNG PHONG da tung di chuyen.
        for (Presence other : online.values()) {
            if (other.session != session && other.code.equals(code) && other.hasState) {
                send(session, playerMessage(other));
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Presence me = online.get(session.getId());
        if (me == null) {
            return;
        }
        JsonNode node = json.readTree(message.getPayload());
        String type = node.path("t").asText();

        if ("move".equals(type)) {
            me.set((float) node.path("x").asDouble(), (float) node.path("y").asDouble(),
                    (float) node.path("z").asDouble(), (float) node.path("yaw").asDouble(),
                    (float) node.path("pitch").asDouble());
            broadcast(session, playerMessage(me));
        } else if ("edit".equals(type)) {
            int x = node.path("x").asInt();
            int y = node.path("y").asInt();
            int z = node.path("z").asInt();
            int b = node.path("b").asInt();
            world.applyEdit(me.code, x, y, z, b);
            broadcast(session, write(Map.of("t", "edit", "name", me.username,
                    "x", x, "y", y, "z", z, "b", b)));
        } else if ("hit".equals(type)) {
            // Danh nhau: mau nam tren may cua NGUOI BI DANH, nen chi chuyen tin cho rieng ho.
            String victim = node.path("name").asText();
            int damage = node.path("dmg").asInt();
            sendToPlayer(me.code, victim,
                    write(Map.of("t", "hurt", "from", me.username, "dmg", damage)));
        } else if ("swing".equals(type)) {
            broadcast(session, write(Map.of("t", "swing", "name", me.username)));
        } else if ("chat".equals(type)) {
            // Chat: cat bot cho khoi spam roi phat cho ca phong (nguoi gui tu in dong cua minh).
            String msg = node.path("msg").asText().trim();
            if (!msg.isEmpty()) {
                if (msg.length() > 100) {
                    msg = msg.substring(0, 100);
                }
                broadcast(session, write(Map.of("t", "chat", "name", me.username, "msg", msg)));
            }
        }
    }

    /** Gui tin cho DUNG MOT nguoi trong phong, tim theo ten tai khoan. */
    private void sendToPlayer(String code, String username, String payload) {
        for (Presence p : online.values()) {
            if (p.code.equals(code) && p.username.equals(username)) {
                send(p.session, payload);
                return;
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Presence gone = online.remove(session.getId());
        if (gone != null) {
            // Dung ma da luu trong Presence: luc nay phien da bi go khoi danh sach online roi.
            broadcastToRoom(gone.code, session, write(Map.of("t", "leave", "name", gone.username)));
        }
    }

    /** Gui tin toi moi nguoi CUNG PHONG voi nguoi gui, tru chinh nguoi gui. */
    private void broadcast(WebSocketSession from, String payload) {
        Presence sender = online.get(from.getId());
        if (sender != null) {
            broadcastToRoom(sender.code, from, payload);
        }
    }

    /** Gui tin toi moi nguoi trong phong {@code code}, tru phien {@code except}. */
    private void broadcastToRoom(String code, WebSocketSession except, String payload) {
        for (Presence p : online.values()) {
            if (p.session != except && p.code.equals(code)) {
                send(p.session, payload);
            }
        }
    }

    /** Gui mot chuoi toi mot phien; sendMessage khong an toan da luong nen phai dong bo. */
    private void send(WebSocketSession session, String payload) {
        if (payload == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException ignore) {
            // Ket noi hong - se don khi afterConnectionClosed chay.
        }
    }

    private String playerMessage(Presence p) {
        return write(Map.of("t", "player", "name", p.username,
                "x", p.x, "y", p.y, "z", p.z, "yaw", p.yaw, "pitch", p.pitch));
    }

    private String write(Map<String, ?> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (IOException impossible) {
            return null;
        }
    }

    /** Mot nguoi dang online: phien ket noi, ten, ma phong, va vi tri/huong nhin gan nhat. */
    private static final class Presence {
        final WebSocketSession session;
        final String username;
        final String code;
        volatile float x, y, z, yaw, pitch;
        volatile boolean hasState;

        Presence(WebSocketSession session, String username, String code) {
            this.session = session;
            this.username = username;
            this.code = code;
        }

        void set(float x, float y, float z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.hasState = true;
        }
    }
}
