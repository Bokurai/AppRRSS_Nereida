package com.example.p10_nere;

import java.util.HashMap;
import java.util.Map;

public class Post {
    public String uid;
    public String author;
    public String authorPhotoUrl;
    public String content;
    public String mediaUrl;
    public String mediaType;

    public long timeStamp;

    public Map<String, Boolean> likes = new HashMap<>();

    public Map<String, Boolean> favorites = new HashMap<>();
    // Constructor vacio requerido por Firestore
    public Post() {}
    public Post(String uid, String author, String authorPhotoUrl, String
            content, long timeStamp, String mediaUrl, String mediaType) {
        this.uid = uid;
        this.author = author;
        this.authorPhotoUrl = authorPhotoUrl;
        this.content = content;
        this.timeStamp = timeStamp;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
    }
}
