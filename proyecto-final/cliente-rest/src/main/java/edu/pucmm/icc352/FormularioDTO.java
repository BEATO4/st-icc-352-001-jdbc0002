package edu.pucmm.icc352;

public class FormularioDTO {
    private String id;
    private String name;
    private String sector;
    private String educationalLevel;
    private double latitude;
    private double longitude;
    private String photoBase64;
    private String createdAt;
    private String userId;
    private String username;

    public FormularioDTO(String id, String name, String sector, String educationalLevel,
                         double latitude, double longitude, String photoBase64,
                         String createdAt, String userId, String username) {
        this.id = id;
        this.name = name;
        this.sector = sector;
        this.educationalLevel = educationalLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoBase64 = photoBase64;
        this.createdAt = createdAt;
        this.userId = userId;
        this.username = username;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public String getEducationalLevel() { return educationalLevel; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPhotoBase64() { return photoBase64; }
    public String getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
}

