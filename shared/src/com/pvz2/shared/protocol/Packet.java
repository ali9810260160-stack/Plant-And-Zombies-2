package com.pvz2.shared.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The single message envelope exchanged on the wire.
 *
 * <p>Serialized as one JSON object: {@code {"v":1,"type":"LOGIN_REQ",
 * "id":"<uuid>","data":{...}}}. {@link Wire} frames it with a 4-byte
 * big-endian length prefix.
 *
 * <ul>
 *   <li>{@link #v} — protocol version ({@link MessageType#VERSION}).</li>
 *   <li>{@link #type} — one of the {@link MessageType} constants.</li>
 *   <li>{@link #id} — correlation id: a request and its reply share it;
 *       unsolicited events may leave it null.</li>
 *   <li>{@link #data} — the typed payload as a {@link JsonObject}; may be null.</li>
 * </ul>
 *
 * <p>This class is dependency-light (only gson, which both sides have) and holds
 * no game logic. Payload POJOs are parsed out of {@link #data} with
 * {@link #payload(Gson, Class)}.
 */
public class Packet {

    public int v = MessageType.VERSION;
    public String type;
    public String id;
    public JsonObject data;

    public Packet() { }

    public Packet(String type, String id, JsonObject data) {
        this.v = MessageType.VERSION;
        this.type = type;
        this.id = id;
        this.data = data;
    }

    /** Build a packet whose payload is any POJO, serialized with {@code gson}. */
    public static Packet of(Gson gson, String type, String id, Object payload) {
        JsonObject obj = null;
        if (payload != null) {
            JsonElement el = gson.toJsonTree(payload);
            if (el.isJsonObject()) obj = el.getAsJsonObject();
        }
        return new Packet(type, id, obj);
    }

    /** Deserialize {@link #data} into {@code clazz}; returns null if data is absent. */
    public <T> T payload(Gson gson, Class<T> clazz) {
        if (data == null) return null;
        return gson.fromJson(data, clazz);
    }

    /** Convenience: read a single string field from {@link #data} (or null). */
    public String str(String field) {
        if (data == null || !data.has(field) || data.get(field).isJsonNull()) return null;
        return data.get(field).getAsString();
    }

    @Override
    public String toString() {
        return "Packet{v=" + v + ", type=" + type + ", id=" + id
                + ", data=" + (data == null ? "null" : data) + '}';
    }
}
