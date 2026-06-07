package model;

/**
 * یک خبر/اطلاعیه برای کاربر.
 * در منوی اخبار نمایش داده می‌شود.
 */
public class NewsItem {

    /** متن خبر */
    private String message;

    /** زمان ایجاد خبر (timestamp) */
    private long timestamp;

    /** آیا کاربر این خبر را خوانده */
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
}
