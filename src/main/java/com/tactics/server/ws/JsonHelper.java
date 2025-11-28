package com.tactics.server.ws;

import com.tactics.server.dto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON helper for WebSocket message serialization/deserialization.
 * Handles only the specific structures needed by WS_PROTOCOL_V1.
 */
public class JsonHelper {

    /**
     * Parse a JSON string into an IncomingMessage.
     * Returns null if parsing fails.
     */
    public static IncomingMessage parseIncomingMessage(String json) {
        try {
            Map<String, Object> map = parseJsonObject(json.trim());
            if (map == null) {
                return null;
            }
            String type = (String) map.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) map.get("payload");
            return new IncomingMessage(type, payload != null ? payload : new HashMap<>());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialize an OutgoingMessage to JSON string.
     */
    public static String toJson(OutgoingMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":").append(quote(message.getType()));
        sb.append(",\"payload\":");
        sb.append(serializeValue(message.getPayload()));
        sb.append("}");
        return sb.toString();
    }

    /**
     * Parse a JSON object string into a Map.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String json) {
        if (json == null || !json.startsWith("{") || !json.endsWith("}")) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        String content = json.substring(1, json.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }

        int i = 0;
        while (i < content.length()) {
            // Skip whitespace
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }
            if (i >= content.length()) break;

            // Parse key
            if (content.charAt(i) != '"') {
                return null;
            }
            int keyStart = i + 1;
            int keyEnd = findStringEnd(content, keyStart);
            if (keyEnd < 0) return null;
            String key = unescapeString(content.substring(keyStart, keyEnd));
            i = keyEnd + 1;

            // Skip whitespace and colon
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }
            if (i >= content.length() || content.charAt(i) != ':') {
                return null;
            }
            i++;
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }

            // Parse value
            ParseResult valueResult = parseValue(content, i);
            if (valueResult == null) return null;
            result.put(key, valueResult.value);
            i = valueResult.endIndex;

            // Skip whitespace
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }

            // Check for comma or end
            if (i < content.length()) {
                if (content.charAt(i) == ',') {
                    i++;
                } else if (content.charAt(i) != '}') {
                    // Might be at end of content
                }
            }
        }
        return result;
    }

    private static ParseResult parseValue(String content, int start) {
        while (start < content.length() && Character.isWhitespace(content.charAt(start))) {
            start++;
        }
        if (start >= content.length()) return null;

        char c = content.charAt(start);

        if (c == '"') {
            // String
            int strStart = start + 1;
            int strEnd = findStringEnd(content, strStart);
            if (strEnd < 0) return null;
            return new ParseResult(unescapeString(content.substring(strStart, strEnd)), strEnd + 1);
        } else if (c == '{') {
            // Object
            int depth = 1;
            int i = start + 1;
            while (i < content.length() && depth > 0) {
                char ch = content.charAt(i);
                if (ch == '{') depth++;
                else if (ch == '}') depth--;
                else if (ch == '"') {
                    int end = findStringEnd(content, i + 1);
                    if (end < 0) return null;
                    i = end;
                }
                i++;
            }
            String objJson = content.substring(start, i);
            Map<String, Object> obj = parseJsonObject(objJson);
            return new ParseResult(obj, i);
        } else if (c == '[') {
            // Array
            int depth = 1;
            int i = start + 1;
            while (i < content.length() && depth > 0) {
                char ch = content.charAt(i);
                if (ch == '[') depth++;
                else if (ch == ']') depth--;
                else if (ch == '"') {
                    int end = findStringEnd(content, i + 1);
                    if (end < 0) return null;
                    i = end;
                }
                i++;
            }
            String arrJson = content.substring(start, i);
            List<Object> arr = parseJsonArray(arrJson);
            return new ParseResult(arr, i);
        } else if (c == 'n' && content.substring(start).startsWith("null")) {
            return new ParseResult(null, start + 4);
        } else if (c == 't' && content.substring(start).startsWith("true")) {
            return new ParseResult(true, start + 4);
        } else if (c == 'f' && content.substring(start).startsWith("false")) {
            return new ParseResult(false, start + 5);
        } else if (c == '-' || Character.isDigit(c)) {
            // Number
            int i = start;
            boolean hasDecimal = false;
            if (content.charAt(i) == '-') i++;
            while (i < content.length()) {
                char ch = content.charAt(i);
                if (Character.isDigit(ch)) {
                    i++;
                } else if (ch == '.' && !hasDecimal) {
                    hasDecimal = true;
                    i++;
                } else {
                    break;
                }
            }
            String numStr = content.substring(start, i);
            if (hasDecimal) {
                return new ParseResult(Double.parseDouble(numStr), i);
            } else {
                return new ParseResult(Integer.parseInt(numStr), i);
            }
        }
        return null;
    }

    private static List<Object> parseJsonArray(String json) {
        List<Object> result = new ArrayList<>();
        if (json == null || !json.startsWith("[") || !json.endsWith("]")) {
            return result;
        }
        String content = json.substring(1, json.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }

        int i = 0;
        while (i < content.length()) {
            ParseResult valueResult = parseValue(content, i);
            if (valueResult == null) break;
            result.add(valueResult.value);
            i = valueResult.endIndex;

            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }
            if (i < content.length() && content.charAt(i) == ',') {
                i++;
            }
        }
        return result;
    }

    private static int findStringEnd(String s, int start) {
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }

    private static String unescapeString(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(next); break;
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String serializeValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return quote((String) value);
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            return serializeMap((Map<String, Object>) value);
        } else if (value instanceof List) {
            return serializeList((List<Object>) value);
        } else if (value instanceof MatchJoinedPayload) {
            return serializeMatchJoinedPayload((MatchJoinedPayload) value);
        } else if (value instanceof StateUpdatePayload) {
            return serializeStateUpdatePayload((StateUpdatePayload) value);
        } else if (value instanceof ValidationErrorPayload) {
            return serializeValidationErrorPayload((ValidationErrorPayload) value);
        } else if (value instanceof GameOverPayload) {
            return serializeGameOverPayload((GameOverPayload) value);
        } else if (value instanceof ActionPayload) {
            return serializeActionPayload((ActionPayload) value);
        }
        return "null";
    }

    private static String serializeMap(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(quote(entry.getKey())).append(":").append(serializeValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String serializeList(List<Object> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append(serializeValue(item));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String serializeMatchJoinedPayload(MatchJoinedPayload payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"matchId\":").append(quote(payload.getMatchId()));
        sb.append(",\"playerId\":").append(quote(payload.getPlayerId()));
        sb.append(",\"state\":").append(serializeValue(payload.getState()));
        sb.append("}");
        return sb.toString();
    }

    private static String serializeStateUpdatePayload(StateUpdatePayload payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"state\":").append(serializeValue(payload.getState()));
        sb.append("}");
        return sb.toString();
    }

    private static String serializeValidationErrorPayload(ValidationErrorPayload payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"message\":").append(quote(payload.getMessage()));
        sb.append(",\"action\":").append(serializeValue(payload.getAction()));
        sb.append("}");
        return sb.toString();
    }

    private static String serializeGameOverPayload(GameOverPayload payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"winner\":").append(quote(payload.getWinner()));
        sb.append(",\"state\":").append(serializeValue(payload.getState()));
        sb.append("}");
        return sb.toString();
    }

    private static String serializeActionPayload(ActionPayload payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"type\":").append(quote(payload.getType()));
        if (payload.getTargetX() != null) {
            sb.append(",\"targetX\":").append(payload.getTargetX());
        }
        if (payload.getTargetY() != null) {
            sb.append(",\"targetY\":").append(payload.getTargetY());
        }
        if (payload.getTargetUnitId() != null) {
            sb.append(",\"targetUnitId\":").append(quote(payload.getTargetUnitId()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static class ParseResult {
        final Object value;
        final int endIndex;

        ParseResult(Object value, int endIndex) {
            this.value = value;
            this.endIndex = endIndex;
        }
    }
}
