package com.zavijavasoft.jaacad;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class GalleryEntity implements Parcelable {

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
    private Date loadedDateTime = new Date();
    private String fileName;
    private String publicKey = "";
    transient private int progressValue;
    transient private boolean markedAsDead;


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

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isMarkedAsDead() {
        return markedAsDead;
    }

    public void setMarkedAsDead(boolean markedAsDead) {
        this.markedAsDead = markedAsDead;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.state == null ? -1 : this.state.ordinal());
        dest.writeString(this.resourceId);
        dest.writeString(this.thumbnailUrl);
        dest.writeString(this.imageUrl);
        dest.writeInt(this.keyColor);
        dest.writeString(this.pathToThumbnail);
        dest.writeString(this.pathToImage);
        dest.writeLong(this.loadedDateTime != null ? this.loadedDateTime.getTime() : -1);
        dest.writeString(this.fileName);
        dest.writeString(this.publicKey);
    }

    public GalleryEntity() {
    }

    protected GalleryEntity(Parcel in) {
        int tmpState = in.readInt();
        this.state = tmpState == -1 ? null : State.values()[tmpState];
        this.resourceId = in.readString();
        this.thumbnailUrl = in.readString();
        this.imageUrl = in.readString();
        this.keyColor = in.readInt();
        this.pathToThumbnail = in.readString();
        this.pathToImage = in.readString();
        long tmpLoadedDateTime = in.readLong();
        this.loadedDateTime = tmpLoadedDateTime == -1 ? null : new Date(tmpLoadedDateTime);
        this.fileName = in.readString();
        this.publicKey = in.readString();
    }

    public static final Creator<GalleryEntity> CREATOR = new Creator<GalleryEntity>() {
        @Override
        public GalleryEntity createFromParcel(Parcel source) {
            return new GalleryEntity(source);
        }

        @Override
        public GalleryEntity[] newArray(int size) {
            return new GalleryEntity[size];
        }
    };
}
