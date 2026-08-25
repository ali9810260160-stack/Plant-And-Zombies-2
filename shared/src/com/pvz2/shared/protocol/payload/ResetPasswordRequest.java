package com.pvz2.shared.protocol.payload;

/**
 * Payload for {@code RESET_PW_REQ}: verify the security answer and set a new
 * password. The server checks {@link #answer} against the stored hash.
 */
public class ResetPasswordRequest {
    public String username;
    public String answer;
    public String newPassword;
}
