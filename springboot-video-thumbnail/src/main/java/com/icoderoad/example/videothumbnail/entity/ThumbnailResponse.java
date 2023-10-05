package com.icoderoad.example.videothumbnail.entity;

public class ThumbnailResponse {

    private String thumbnailURL;

    public ThumbnailResponse(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }
}
