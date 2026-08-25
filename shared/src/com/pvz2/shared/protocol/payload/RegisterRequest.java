package com.pvz2.shared.protocol.payload;

/** Payload for {@code REGISTER_REQ}. Enum-valued fields travel as their names. */
public class RegisterRequest {
    public String username;
    public String password;
    public String confirmPassword;
    public String nickname;
    public String email;
    public String gender;         // Gender enum name, e.g. "MALE"
    public boolean stayLoggedIn;
}
