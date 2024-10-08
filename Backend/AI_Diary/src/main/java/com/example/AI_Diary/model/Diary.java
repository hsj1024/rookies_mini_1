package com.example.AI_Diary.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Transient;

@Entity
public class Diary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String emotion;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] image;

    @Transient
    private String imageBase64;
    
    

    private Long userId;
    
    @ElementCollection
    @CollectionTable(name = "diary_tags", joinColumns = @JoinColumn(name = "diary_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    private String errorMessage;

 // 기본 생성자 추가
    public Diary() {
    }
    // 문자열 인수를 받는 생성자 추가
    public Diary(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // 에러 메시지를 반환하는 getter 추가
    public String getErrorMessage() {
        return errorMessage;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<String> getTags() {
    	return tags;
    	
    }
    
    public void setTags(List<String> tags2) {
    	this.tags = tags2;
    }
}