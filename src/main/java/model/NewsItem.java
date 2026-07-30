package model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NewsItem {
    private String message;
    private long timestamp;
    private boolean read;

    public NewsItem(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getDate() {
        LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }
}
