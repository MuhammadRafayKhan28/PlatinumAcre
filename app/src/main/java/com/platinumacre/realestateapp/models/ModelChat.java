package com.platinumacre.realestateapp.models;

public class ModelChat {
    /*---Variables. spellings and case should be same as in firebase db---*/
    String messageId;
    String messageType;
    String message;
    String fromUid;
    String toUid;
    long timestamp;

    /*---Empty constructor require for firebase db---*/
    public ModelChat() {

    }

    /*---Constructor with all params---*/
    public ModelChat(String messageId, String messageType, String message, String fromUid, String toUid, long timestamp) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.message = message;
        this.fromUid = fromUid;
        this.toUid = toUid;
        this.timestamp = timestamp;
    }

    /*---Getter & Setters---*/
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFromUid() {
        return fromUid;
    }

    public void setFromUid(String fromUid) {
        this.fromUid = fromUid;
    }

    public String getToUid() {
        return toUid;
    }

    public void setToUid(String toUid) {
        this.toUid = toUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
