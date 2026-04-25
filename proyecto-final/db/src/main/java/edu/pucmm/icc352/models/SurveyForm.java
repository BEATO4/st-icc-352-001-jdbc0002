package edu.pucmm.icc352.models;

import org.bson.types.ObjectId;

import java.time.LocalDateTime;


public class SurveyForm {
    private ObjectId id;
    private String name;
    private String sector;
    private String educationalLevel;
    private Double latitude;
    private Double longitude;
    private String photoBase64;
    private String userId;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean synced;

    public SurveyForm() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.synced = false;
    }

    public SurveyForm(String name, String sector, String educationalLevel,
                      Double latitude, Double longitude, String photoBase64,
                      String userId, String username) {
        this();
        this.name = name;
        this.sector = sector;
        this.educationalLevel = educationalLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoBase64 = photoBase64;
        this.userId = userId;
        this.username = username;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getIdAsString() {
        return id != null ? id.toHexString() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
        this.updatedAt = LocalDateTime.now();
    }

    public String getEducationalLevel() {
        return educationalLevel;
    }

    public void setEducationalLevel(String educationalLevel) {
        this.educationalLevel = educationalLevel;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
        this.updatedAt = LocalDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SurveyForm{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sector='" + sector + '\'' +
                ", educationalLevel='" + educationalLevel + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", createdAt=" + createdAt +
                ", synced=" + synced +
                '}';
    }
}
