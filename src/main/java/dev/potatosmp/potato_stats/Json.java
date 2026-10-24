package dev.potatosmp.potato_stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, dependency-free JSON serializer/parser.
 * Used to persist per-player counters to disk under plugins/PotatoStats/data/.
 */
public final class Json {
    private Json() {}

    public static String serialize(Object value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void append(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            appendString(sb, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                appendString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                append(sb, e.getValue());
            }
            sb.append('}');
        } else if (value instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) sb.append(',');
                first = false;
                append(sb, item);
            }
            sb.append(']');
        } else if (value.getClass().isArray()) {
            append(sb, Arrays.asList(value));
        } else {
            appendString(sb, value.toString());
        }
    }

    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    public static Object parse(String json) {
        Parser p = new Parser(json);
        Object value = p.parseValue();
        p.skipWhitespace();
        if (p.pos < p.len) {
            throw new IllegalArgumentException("Unexpected trailing character at position " + p.pos);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value) {
        return (T) value;
    }

    private static class Parser {
        private final String s;
        private final int len;
        private int pos;

        Parser(String s) {
            this.s = s;
            this.len = s.length();
            this.pos = 0;
        }

        private void skipWhitespace() {
            while (pos < len) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= len) throw new IllegalArgumentException("Unexpected end of input");
            char c = s.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWhitespace();
            if (pos < len && s.charAt(pos) == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                if (pos >= len || s.charAt(pos) != '"') throw new IllegalArgumentException("Expected string key in object");
                String key = parseString();
                skipWhitespace();
                if (pos >= len || s.charAt(pos) != ':') throw new IllegalArgumentException("Expected ':' after key");
                pos++; // :
                map.put(key, parseValue());
                skipWhitespace();
                if (pos >= len) throw new IllegalArgumentException("Unexpected end of input in object");
                char c = s.charAt(pos);
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; break; }
                throw new IllegalArgumentException("Expected ',' or '}' in object");
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWhitespace();
            if (pos < len && s.charAt(pos) == ']') { pos++; return list; }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (pos >= len) throw new IllegalArgumentException("Unexpected end of input in array");
                char c = s.charAt(pos);
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; break; }
                throw new IllegalArgumentException("Expected ',' or ']' in array");
            }
            return list;
        }

        String parseString() {
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < len) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= len) throw new IllegalArgumentException("Unexpected end of input in string");
                    char esc = s.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > len) throw new IllegalArgumentException("Invalid unicode escape");
                            int code = Integer.parseInt(s.substring(pos, pos + 4), 16);
                            pos += 4;
                            sb.append((char) code);
                            break;
                        default: throw new IllegalArgumentException("Invalid escape: " + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        boolean parseBoolean() {
            if (s.startsWith("true", pos)) { pos += 4; return true; }
            if (s.startsWith("false", pos)) { pos += 5; return false; }
            throw new IllegalArgumentException("Invalid boolean");
        }

        Object parseNull() {
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            throw new IllegalArgumentException("Invalid null");
        }

        Object parseNumber() {
            int start = pos;
            while (pos < len) {
                char c = s.charAt(pos);
                if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            String num = s.substring(start, pos);
            if (num.contains(".") || num.toLowerCase().contains("e")) {
                return Double.parseDouble(num);
            }
            return Long.parseLong(num);
        }
    }
}
