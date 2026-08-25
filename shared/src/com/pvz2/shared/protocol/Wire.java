package com.pvz2.shared.protocol;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Length-prefixed JSON framing codec, shared by server and client so both
 * sides frame packets identically.
 *
 * <p>Wire format per packet: a 4-byte big-endian unsigned length {@code N},
 * then {@code N} bytes of UTF-8 JSON encoding a {@link Packet}.
 *
 * <p>All methods are static and thread-agnostic; callers must serialize writes
 * on a single stream themselves (see the server's per-connection write lock).
 */
public final class Wire {

    private Wire() { }

    /** A single shared Gson instance (thread-safe once configured). */
    public static final Gson GSON = new Gson();

    /** Hard cap on a single frame to guard against corrupt/hostile length prefixes. */
    public static final int MAX_FRAME_BYTES = 8 * 1024 * 1024; // 8 MB

    /**
     * Read exactly one packet from {@code in}, blocking until a full frame
     * arrives.
     *
     * @return the decoded packet, or {@code null} on a clean end-of-stream
     *         (peer closed between frames).
     * @throws IOException on truncated frames, oversize frames, or IO errors.
     */
    public static Packet read(DataInputStream in) throws IOException {
        int len;
        try {
            len = in.readInt();
        } catch (EOFException eof) {
            return null; // peer closed cleanly at a frame boundary
        }
        if (len < 0 || len > MAX_FRAME_BYTES) {
            throw new IOException("Illegal frame length: " + len);
        }
        byte[] buf = new byte[len];
        in.readFully(buf); // throws EOFException if the frame is truncated
        String json = new String(buf, StandardCharsets.UTF_8);
        Packet p = GSON.fromJson(json, Packet.class);
        if (p == null) throw new IOException("Empty/blank frame");
        return p;
    }

    /**
     * Write one packet to {@code out} and flush. The caller must hold whatever
     * lock guards this stream (a socket's output must not be written by two
     * threads at once).
     */
    public static void write(DataOutputStream out, Packet p) throws IOException {
        String json = GSON.toJson(p);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FRAME_BYTES) {
            throw new IOException("Frame too large to send: " + bytes.length + " bytes");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }
}
