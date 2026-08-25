package com.pvz2.shared.protocol.payload;

/**
 * Payload for {@code REACTION_EVT}: an in-match reaction one player sends to the
 * other (doc §"سیستم ارسال واکنش"). Relayed by the server to the opponent and
 * shown graphically in a corner of their screen.
 */
public class ReactionEvent {
    public String matchId;
    /** "TEXT" | "EMOJI" | "STICKER". */
    public String kind;
    /** The message text, emoji glyph, or sticker id/glyph. */
    public String value;

    public ReactionEvent() { }
    public ReactionEvent(String matchId, String kind, String value) {
        this.matchId = matchId;
        this.kind = kind;
        this.value = value;
    }
}
