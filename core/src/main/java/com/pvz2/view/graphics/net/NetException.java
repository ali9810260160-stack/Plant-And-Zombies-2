package com.pvz2.graphics.net;

/** Unchecked failure of a network operation (timeout, disconnect, IO). */
public class NetException extends RuntimeException {
    public NetException(String message) { super(message); }
    public NetException(String message, Throwable cause) { super(message, cause); }
}
