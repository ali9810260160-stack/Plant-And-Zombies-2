package com.pvz2.shared.protocol.payload;

/** Reply for {@code RECOVER_RES}: the account's security question to answer. */
public class RecoverResponse {
    public String securityQuestion;   // SecurityQuestion enum name

    public RecoverResponse() { }
    public RecoverResponse(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }
}
