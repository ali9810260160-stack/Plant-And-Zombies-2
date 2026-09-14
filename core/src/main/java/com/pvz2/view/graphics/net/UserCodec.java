package com.pvz2.graphics.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pvz2.model.User;
import com.pvz2.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bridges the server's user document (a gson {@link JsonObject}) and the client's
 * {@link User} model, via {@link UserRepository}'s flat-map (de)serializer.
 *
 * <p>Flattening rule matches what {@code UserRepository.mapToUser} expects:
 * a JSON primitive becomes its string value; an array/object becomes its compact
 * JSON text (e.g. {@code ["PEASHOOTER"]}). This works whether the server stored
 * a field as a native JSON value (seeded from users.json) or as an encoded
 * string (pushed by a client) — both round-trip correctly.
 */
public final class UserCodec {

    private UserCodec() { }

    public static Map<String, String> flatten(JsonObject o) {
        Map<String, String> m = new LinkedHashMap<>();
        if (o == null) return m;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            JsonElement v = e.getValue();
            if (v == null || v.isJsonNull()) { m.put(e.getKey(), ""); continue; }
            m.put(e.getKey(), v.isJsonPrimitive() ? v.getAsString() : v.toString());
        }
        return m;
    }

    public static JsonObject toJson(Map<String, String> flat) {
        JsonObject o = new JsonObject();
        if (flat != null) {
            for (Map.Entry<String, String> e : flat.entrySet()) {
                o.addProperty(e.getKey(), e.getValue());
            }
        }
        return o;
    }

    /** Server document → client {@link User}. */
    public static User toUser(UserRepository repo, JsonObject doc) {
        return repo.fromFlatMap(flatten(doc));
    }

    /** Client {@link User} → document to push to the server (all-string values). */
    public static JsonObject toJson(UserRepository repo, User user) {
        return toJson(repo.toFlatMap(user));
    }
}
