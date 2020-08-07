package com.simrin.scorebox.Model;

public class Chat {
    private String sender;
    private String receiver;
    private String message;
    private String timestamp;
    private String type;
    private String id;
    private String img_place;
    private boolean isseen;
    private boolean imageExists;

    public Chat(){

    }

    public Chat(String sender, String receiver, String message, String timestamp, boolean isseen,
                String img_place, String id, String type, boolean imageExists) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.timestamp = timestamp;
        this.isseen = isseen;
        this.id = id;
        this.type = type;
        this.img_place = img_place;
        this.imageExists = imageExists;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isIsseen() {
        return isseen;
    }

    public void setIsseen(boolean isseen) {
        this.isseen = isseen;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImg_place() {
        return img_place;
    }

    public void setImg_place(String img_place) {
        this.img_place = img_place;
    }

    public boolean isImageExists() {
        return imageExists;
    }

    public void setImageExists(Boolean imageExists) {
        this.imageExists = imageExists;
    }
}
