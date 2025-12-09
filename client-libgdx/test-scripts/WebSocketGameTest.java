package test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Automated WebSocket test for Tactics 5x5
 * Simulates two players completing a full game
 *
 * Run: java WebSocketGameTest.java
 * (Requires JDK 11+ with java.net.http)
 *
 * Note: Server assigns P1/P2 regardless of playerId sent in join_match
 */
public class WebSocketGameTest {

    private static final String SERVER_URL = "ws://localhost:8080/ws";
    // Use timestamp to ensure unique match for this test run
    private static final String MATCH_ID = "test-" + System.currentTimeMillis();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Tactics 5x5 WebSocket Test ===");
        System.out.println("Match ID: " + MATCH_ID + "\n");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch gameOver = new CountDownLatch(2);

        // Start P1 first
        Future<?> player1 = executor.submit(() -> runPlayer("P1", "WARRIOR", "TANK", "ARCHER", gameOver));

        // Wait for P1 to join, then start P2
        Thread.sleep(1000);

        Future<?> player2 = executor.submit(() -> runPlayer("P2", "MAGE", "TANK", "ASSASSIN", gameOver));

        // Wait for both players to finish (max 120 seconds)
        boolean completed = gameOver.await(120, TimeUnit.SECONDS);

        if (completed) {
            System.out.println("\n=== Test Complete ===");
        } else {
            System.out.println("\n=== Test Timeout ===");
        }

        executor.shutdownNow();
    }

    private static void runPlayer(String playerId, String heroClass, String minion1, String minion2, CountDownLatch gameOver) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicInteger actionCount = new AtomicInteger(0);
            AtomicReference<String> assignedPlayerId = new AtomicReference<>(playerId);
            BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
            AtomicReference<WebSocket> wsRef = new AtomicReference<>();

            WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(URI.create(SERVER_URL), new WebSocket.Listener() {
                    private StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        log(playerId, "Connected");
                        wsRef.set(webSocket);
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        buffer.append(data);
                        if (last) {
                            messageQueue.offer(buffer.toString());
                            buffer = new StringBuilder();
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log(playerId, "Disconnected: " + reason);
                        finished.set(true);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log(playerId, "Error: " + error.getMessage());
                        finished.set(true);
                    }
                }).join();

            wsRef.set(ws);

            // Wait for connection to be ready
            Thread.sleep(200);

            // Join match
            String joinMsg = String.format(
                "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"%s\",\"playerId\":\"%s\"}}",
                MATCH_ID, playerId);
            ws.sendText(joinMsg, true);
            log(playerId, "Sent: join_match (matchId=" + MATCH_ID + ")");

            // Process messages
            while (!finished.get() && actionCount.get() < 100) {
                String msg = messageQueue.poll(30, TimeUnit.SECONDS);
                if (msg == null) {
                    log(playerId, "Timeout waiting for message");
                    break;
                }

                String msgType = extractField(msg, "type");

                // Skip noisy timeout messages in log
                if ("timeout".equals(msgType)) {
                    continue;
                }

                log(playerId, "Received: " + msgType);

                if ("validation_error".equals(msgType)) {
                    String errorMessage = extractField(msg, "message");
                    log(playerId, "  Error: " + errorMessage);
                }
                else if ("match_joined".equals(msgType)) {
                    // Server assigns playerId - store it
                    String serverAssignedId = extractField(msg, "playerId");
                    if (serverAssignedId != null) {
                        assignedPlayerId.set(serverAssignedId);
                        log(playerId, "  Server assigned: " + serverAssignedId);
                    }
                }
                else if ("game_ready".equals(msgType)) {
                    log(playerId, "Game is ready - both players joined!");
                }
                else if ("your_turn".equals(msgType)) {
                    // It's our turn to act - send END_TURN
                    String myId = assignedPlayerId.get();
                    String actionMsg = String.format(
                        "{\"type\":\"action\",\"payload\":{\"matchId\":\"%s\",\"playerId\":\"%s\",\"action\":{\"type\":\"END_TURN\"}}}",
                        MATCH_ID, myId);
                    ws.sendText(actionMsg, true);
                    log(playerId, "Sent: END_TURN (#" + actionCount.incrementAndGet() + ")");
                }
                else if ("state_update".equals(msgType)) {
                    String currentPlayer = extractField(msg, "currentPlayerId");
                    log(playerId, "  State update, current player: " + currentPlayer);

                    // If it's our turn, send END_TURN
                    String myId = assignedPlayerId.get();
                    if (myId.equals(currentPlayer)) {
                        String actionMsg = String.format(
                            "{\"type\":\"action\",\"payload\":{\"matchId\":\"%s\",\"playerId\":\"%s\",\"action\":{\"type\":\"END_TURN\"}}}",
                            MATCH_ID, myId);
                        ws.sendText(actionMsg, true);
                        log(playerId, "Sent: END_TURN (#" + actionCount.incrementAndGet() + ")");
                    }
                }
                else if ("game_over".equals(msgType)) {
                    String winner = extractField(msg, "winner");
                    log(playerId, "=== GAME OVER! Winner: " + winner + " ===");
                    finished.set(true);
                }
            }

            ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");

        } catch (Exception e) {
            log(playerId, "Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            gameOver.countDown();
        }
    }

    private static String extractField(String json, String field) {
        // Simple JSON field extraction
        String search = "\"" + field + "\":";
        int start = json.indexOf(search);
        if (start < 0) return null;

        start += search.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;

        if (start >= json.length()) return null;

        char c = json.charAt(start);
        if (c == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        } else if (c == '{' || c == '[') {
            return null;
        } else {
            int end = start;
            while (end < json.length() && ",}] \n\r\t".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end);
        }
    }

    private static void log(String player, String message) {
        System.out.printf("[%s] %s%n", player, message);
    }
}
