package util;

import java.util.*;

/**
 * پارسر ساده JSON بدون کتابخانه خارجی.
 * از آرایه‌ها و آبجکت‌های JSON پشتیبانی می‌کند.
 */
public class SimpleJsonParser {

    private final String json;
    private int pos;

    public SimpleJsonParser(String json) {
        this.json = json.trim();
        this.pos = 0;
    }

    /** پارس کردن یک لیست از آبجکت‌های JSON */
    public static List<Map<String, String>> parseArray(String jsonStr) {
        List<Map<String, String>> result = new ArrayList<>();
        SimpleJsonParser parser = new SimpleJsonParser(jsonStr.trim());
        if (parser.pos >= parser.json.length()
                || parser.json.charAt(parser.pos) != '[') {
            return result;
        }
        parser.pos++;
        parser.skipWhitespace();
        while (parser.pos < parser.json.length()
                && parser.json.charAt(parser.pos) != ']') {
            parser.skipWhitespace();
            if (parser.json.charAt(parser.pos) == '{') {
                result.add(parser.parseObject());
            }
            parser.skipWhitespace();
            if (parser.pos < parser.json.length()
                    && parser.json.charAt(parser.pos) == ',') {
                parser.pos++;
            }
            parser.skipWhitespace();
        }
        return result;
    }

    private Map<String, String> parseObject() {
        Map<String, String> map = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        while (pos < json.length() && json.charAt(pos) != '}') {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ':') {
                pos++;
            }
            skipWhitespace();
            String value = parseValue();
            map.put(key, value);
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ',') {
                pos++;
            }
            skipWhitespace();
        }
        if (pos < json.length()) {
            pos++;
        }
        return map;
    }

    private String parseString() {
        if (pos >= json.length() || json.charAt(pos) != '"') {
            return "";
        }
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < json.length() && json.charAt(pos) != '"') {
            if (json.charAt(pos) == '\\' && pos + 1 < json.length()) {
                pos++;
                sb.append(json.charAt(pos));
            } else {
                sb.append(json.charAt(pos));
            }
            pos++;
        }
        if (pos < json.length()) {
            pos++;
        }
        return sb.toString();
    }

    private String parseValue() {
        if (pos >= json.length()) {
            return "";
        }
        char c = json.charAt(pos);
        if (c == '"') {
            return parseString();
        } else if (c == '{') {
            int start = pos;
            int depth = 0;
            while (pos < json.length()) {
                if (json.charAt(pos) == '{') {
                    depth++;
                } else if (json.charAt(pos) == '}') {
                    depth--;
                    if (depth == 0) {
                        pos++;
                        break;
                    }
                }
                pos++;
            }
            return json.substring(start, pos);
        } else if (c == '[') {
            int start = pos;
            int depth = 0;
            while (pos < json.length()) {
                if (json.charAt(pos) == '[') {
                    depth++;
                } else if (json.charAt(pos) == ']') {
                    depth--;
                    if (depth == 0) {
                        pos++;
                        break;
                    }
                }
                pos++;
            }
            return json.substring(start, pos);
        } else {
            StringBuilder sb = new StringBuilder();
            while (pos < json.length() && json.charAt(pos) != ','
                    && json.charAt(pos) != '}' && json.charAt(pos) != ']') {
                sb.append(json.charAt(pos));
                pos++;
            }
            return sb.toString().trim();
        }
    }

    private void skipWhitespace() {
        while (pos < json.length()
                && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }

    /** ساخت JSON آرایه از لیست map‌ها */
    public static String toJsonArray(List<Map<String, String>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJsonObject(list.get(i)));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /** ساخت JSON آبجکت از map */
    public static String toJsonObject(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            String val = entry.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val.startsWith("{") || val.startsWith("[")
                    || val.equals("true") || val.equals("false")
                    || isNumeric(val)) {
                sb.append(val);
            } else {
                sb.append("\"").append(escape(val)).append("\"");
            }
            if (i < map.size() - 1) {
                sb.append(",");
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
