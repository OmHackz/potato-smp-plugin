package dev.potatosmp.potato_stats.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dev.potatosmp.potato_stats.data.DataStore;
import dev.potatosmp.potato_stats.Json;
import dev.potatosmp.potato_stats.data.PlayerData;
import dev.potatosmp.potato_stats.PotatoStatsPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional read-only HTTP server the website can poll for live stats.
 *
 * <p>Only started when {@code http.enabled} is true in config.yml (default off). Binds
 * {@code 0.0.0.0:<port>} so it is reachable from the same network; never leaks the push secret.
 *
 * Endpoints:
 *   GET /v1/players                                  -> { players: [...] }
 *   GET /v1/players/{uuid}                           -> full stat block
 *   GET /v1/stats/leaderboard/{type}                 -> ranked list
 */
public class HttpApi {
    private final PotatoStatsPlugin plugin;
    private final DataStore dataStore;
    private final int port;
    private HttpServer server;

    public HttpApi(PotatoStatsPlugin plugin, DataStore dataStore, int port) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/v1/players", this::handlePlayers);
            server.createContext("/v1/players/", this::handlePlayer);
            server.createContext("/v1/stats/leaderboard/", this::handleLeaderboard);
            server.setExecutor(null); // use the default executor
            server.start();
            plugin.getLogger().info("PotatoStats HTTP API listening on 0.0.0.0:" + port);
        } catch (Exception e) {
            plugin.getLogger().warning("PotatoStats: failed to start HTTP API on port " + port + ": " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handlePlayers(HttpExchange exchange) {
        try {
            List<Map<String, Object>> players = new ArrayList<>();
            for (PlayerData data : dataStore.getAll().values()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("name", data.name);
                p.put("uuid", data.uuid.toString());
                p.put("online", plugin.getServer().getPlayer(data.uuid) != null);
                p.put("rank", data.rank);
                p.put("firstJoin", data.firstJoin);
                p.put("lastSeen", data.lastSeen);
                p.put("playtime", data.playtimeMinutes);
                players.add(p);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("players", players);
            sendJson(exchange, 200, body);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handlePlayer(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String uuidStr = path.substring("/v1/players/".length());
        try {
            UUID uuid = UUID.fromString(uuidStr);
            PlayerData data = dataStore.get(uuid);
            if (data == null) {
                sendError(exchange, 404, "player not found");
                return;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("uuid", data.uuid.toString());
            body.put("name", data.name);
            body.put("online", plugin.getServer().getPlayer(data.uuid) != null);
            body.put("rank", data.rank);
            body.put("firstJoin", data.firstJoin);
            body.put("lastSeen", data.lastSeen);
            body.put("playtime", data.playtimeMinutes);
            body.put("stats", new LinkedHashMap<>(data.stats));
            sendJson(exchange, 200, body);
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "invalid uuid");
        }
    }

    private void handleLeaderboard(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String type = path.substring("/v1/stats/leaderboard/".length());
        List<Map<String, Object>> ranked = new ArrayList<>();
        for (PlayerData data : dataStore.getAll().values()) {
            Long value = data.stats.get(type);
            if (value == null) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", data.name);
            entry.put("uuid", data.uuid.toString());
            entry.put("value", value);
            ranked.add(entry);
        }
        ranked.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
        sendJson(exchange, 200, ranked);
    }

    private void sendJson(HttpExchange exchange, int code, Object body) {
        try {
            byte[] bytes = Json.serialize(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("PotatoStats: failed to write HTTP response: " + e.getMessage());
        }
    }

    private void sendError(HttpExchange exchange, int code, String msg) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", msg);
            sendJson(exchange, code, body);
        } catch (Exception e) {
            plugin.getLogger().warning("PotatoStats: failed to send error response: " + e.getMessage());
        }
    }
}
