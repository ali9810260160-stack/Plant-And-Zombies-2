package com.pvz2.shared.protocol.payload;

/** Payload for {@code SET_SECURITY_REQ}: set the account's security question. */
public class SetSecurityRequest {
    public String token;        // authenticated caller
    public String question;     // SecurityQuestion enum name, e.g. "Q1"
    public String answer;
    public String confirmAnswer;
}
