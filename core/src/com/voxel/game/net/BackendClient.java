package com.voxel.game.net;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Cau noi giua game va backend. Goi cac diem cuoi REST bang HTTP va doc JSON tra ve.
 *
 * Dia chi may chu lay tu bien moi truong VOXEL_SERVER_URL, mac dinh la localhost:8080
 * (khop voi cong ma docker-compose mo ra).
 */
public final class BackendClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final JsonReader json = new JsonReader();

    public BackendClient() {
        String url = System.getenv("VOXEL_SERVER_URL");
        this.baseUrl = (url == null || url.isBlank()) ? "http://localhost:8080" : url.trim();
    }

    // ------------------------------------------------------------- dang nhap

    /** Tao tai khoan moi roi vao the gioi co ma {@code code}. */
    public Session register(String username, String password, String code) {
        return authenticate("/api/auth/register", username, password, code);
    }

    /** Dang nhap tai khoan co san roi vao the gioi co ma {@code code}. */
    public Session login(String username, String password, String code) {
        return authenticate("/api/auth/login", username, password, code);
    }

    private Session authenticate(String path, String username, String password, String code) {
        String body = "{\"username\":" + quote(username) + ",\"password\":" + quote(password) + "}";
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));

        if (response.statusCode() != 200) {
            throw new BackendException(readError(response));
        }

        JsonValue root = json.parse(response.body());
        String token = root.getString("token");
        String name = root.getString("username");
        return new Session(token, name, code, loadWorld(token, code));
    }

    // -------------------------------------------------------------- the gioi

    /** Tai the gioi co ma {@code code} (nguoi dung giu the {@code token}). */
    public WorldSnapshot loadWorld(String token, String code) {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(uri("/api/world?code=" + encode(code)))
                .header("Authorization", "Bearer " + token)
                .GET());

        if (response.statusCode() != 200) {
            throw new BackendException(readError(response));
        }

        JsonValue root = json.parse(response.body());
        long seed = root.getLong("seed");

        JsonValue p = root.get("player");
        PlayerState player = new PlayerState(
                p.getFloat("x"), p.getFloat("y"), p.getFloat("z"),
                p.getFloat("yaw"), p.getFloat("pitch"), p.getInt("mode"));

        List<Edit> edits = new ArrayList<Edit>();
        JsonValue array = root.get("edits");
        for (JsonValue e = array.child; e != null; e = e.next) {
            edits.add(new Edit(e.getInt("x"), e.getInt("y"), e.getInt("z"), e.getInt("b")));
        }
        return new WorldSnapshot(seed, player, edits);
    }

    // ----------------------------------------------------------------- ho tro

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    /** Gui request, gói loi mang thanh thong bao de hieu. */
    private HttpResponse<String> send(HttpRequest.Builder builder) {
        try {
            return http.send(builder.timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception failure) {
            throw new BackendException(
                    "Could not reach the server (" + baseUrl + ").\nDid you run 'docker compose up'?");
        }
    }

    /** Doc thong bao loi tu than JSON cua Spring, neu khong co thi bao chung. */
    private String readError(HttpResponse<String> response) {
        try {
            JsonValue root = json.parse(response.body());
            String message = root.getString("message", null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (Exception ignore) {
            // than khong phai JSON - roi xuong thong bao chung.
        }
        return "Server error (code " + response.statusCode() + ")";
    }

    /** Ma hoa gia tri cho chuoi truy van URL (vd ma the gioi co dau cach). */
    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Boc chuoi trong dau nhay va thoat ky tu dac biet cho JSON. */
    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    out.append(c);
            }
        }
        out.append('"');
        return out.toString();
    }
}
