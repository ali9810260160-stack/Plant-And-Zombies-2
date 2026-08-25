package com.pvz2.shared.protocol.payload;

import java.util.List;

/** Reply for {@code ONLINE_LIST_RES}: usernames currently online (minus self). */
public class OnlineListResponse {
    public List<String> usernames;

    public OnlineListResponse() { }
    public OnlineListResponse(List<String> usernames) { this.usernames = usernames; }
}
