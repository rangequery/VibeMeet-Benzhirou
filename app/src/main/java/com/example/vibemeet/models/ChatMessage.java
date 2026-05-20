package com.example.vibemeet.models;

public class ChatMessage {
    private String id;
    private String content;
    private boolean isUserMessage; // true if user sent it, false if bot
    private long timestamp;

    public ChatMessage(String content, boolean isUserMessage) {
        this.content = content;
        this.isUserMessage = isUserMessage;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isUserMessage() { return isUserMessage; }
    public void setUserMessage(boolean userMessage) { isUserMessage = userMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
