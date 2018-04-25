package com.zavijavasoft.jaacad;

import java.util.Date;

public class GalleryEntity {

    public enum State{
        NONE,
        NOT_LOADED,
        ONCE_LOADED,
        THUMBNAIL,
        IMAGE
    }

    private State state;

    private String resourceId;
    private String thumbnailUrl;
    private String imageUrl;

    private int keyColor;
    private String pathToThumbnail;
    private String pathToImage;
    private Date loadedDateTime;
    private String fileName;
    private int progressValue;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getKeyColor() {
        return keyColor;
    }

    public void setKeyColor(int keyColor) {
        this.keyColor = keyColor;
    }

    public String getPathToThumbnail() {
        return pathToThumbnail;
    }

    public void setPathToThumbnail(String pathToThumbnail) {
        this.pathToThumbnail = pathToThumbnail;
    }

    public String getPathToImage() {
        return pathToImage;
    }

    public void setPathToImage(String pathToImage) {
        this.pathToImage = pathToImage;
    }

    public Date getLoadedDateTime() {
        return loadedDateTime;
    }

    public void setLoadedDateTime(Date loadedDateTime) {
        this.loadedDateTime = loadedDateTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(int progressValue) {
        this.progressValue = progressValue;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
