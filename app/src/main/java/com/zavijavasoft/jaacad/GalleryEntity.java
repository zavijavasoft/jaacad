package com.zavijavasoft.jaacad;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Класс, реализующий сущность "изображения". Содержит информацию:
 * имя файла,
 * URL изображения,
 * URL превью,
 * Публичный ключ, который является URL для открытых ресурсов
 * идентификатор ресурса (используется  MD5  хеш картинки)
 * дату и время скачивания изображения приложением
 * случайный цвет подложки.
 * <p>
 * Является Parcelable POJO, для передачи в интентах, сам ничего не делает
 */
public class GalleryEntity implements Parcelable {

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
    private State state = State.NONE;

    private String resourceId;
    private String thumbnailUrl = "";
    private String imageUrl = "";

    private int keyColor;
    private String pathToThumbnail = "";
    private String pathToImage = "";
    private Date loadedDateTime = new Date();
    private String fileName = "";
    private String publicKey = "";
    transient private int progressValue;
    transient private boolean markedAsDead;


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

    /**
     * @return идентификатор ресурса ( MD5 хеш изображения)
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Устанавливает идентификатор ресурса ( MD5 хеш изображения)
     *
     * @param resourceId идентификатор ресурса
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * @return случайный цвет, который показывается на подложке до того, как скачается превью
     */
    public int getKeyColor() {
        return keyColor;
    }

    /**
     * @param keyColor случайный цвет, который показывается на подложке до того, как скачается превью
     */
    public void setKeyColor(int keyColor) {
        this.keyColor = keyColor;
    }

    /**
     * @return путь (в файловой системе) к файлу превью
     */
    public String getPathToThumbnail() {
        return pathToThumbnail;
    }

    /**
     * @param pathToThumbnail путь (в файловой системе) к файлу превью
     */
    public void setPathToThumbnail(String pathToThumbnail) {
        this.pathToThumbnail = pathToThumbnail;
    }

    /**
     * @return путь (в файловой системе) к файлу изображения
     */
    public String getPathToImage() {
        return pathToImage;
    }

    /**
     * @param pathToImage путь (в файловой системе) к файлу изображения
     */
    public void setPathToImage(String pathToImage) {
        this.pathToImage = pathToImage;
    }

    /**
     * @return Дата и время загрузки изображения приложением
     */

    public Date getLoadedDateTime() {
        return loadedDateTime;
    }

    /**
     * @param loadedDateTime Дата и время загрузки изображения приложением
     */

    public void setLoadedDateTime(Date loadedDateTime) {
        this.loadedDateTime = loadedDateTime;
    }

    /**
     * @return Имя файла изображения
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName Имя файла изображения
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return Статус изображения из перечисления {@link GalleryEntity.State}
     */
    public State getState() {
        return state;
    }

    /**
     * @param state Статус изображения из перечисления {@link GalleryEntity.State}
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * @return URL к файлу превью (не оборачивается в Cloud-api, хотя, согласно документации, требует
     * токена авторизации для защищенных файлов)
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * @param thumbnailUrl URL к файлу превью
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(int progressValue) {
        this.progressValue = progressValue;
    }


    /**
     * @return URL к файлу изображения (должен быть обернут в Cloud-api)
     */
    public String getImageUrl() {
        return imageUrl;
    }


    /**
     * @param imageUrl URL к файлу превью (должен быть обернут в Cloud-api)
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * @return Публичный ключ открытых ресурсов
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * @param publicKey Публичный ключ открытых ресурсов
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * @return Флаг, помечающий сущность к удалению. Помеченная этим флагом сущность
     * не будет возвращаться при поиске по идентификатору и будет удалена при сохранении кэша
     */
    public boolean isMarkedAsDead() {
        return markedAsDead;
    }

    /**
     * @param markedAsDead Флаг, помечающий сущность к удалению. Помеченная этим флагом сущность
     *                     не будет возвращаться при поиске по идентификатору и будет удалена при сохранении кэша
     */
    public void setMarkedAsDead(boolean markedAsDead) {
        this.markedAsDead = markedAsDead;
    }

    // Далее идут стандартные методы реализации Parcelable
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

    /**
     * Перечисление, определяющее, состояние сущности при ее жизненных циклах
     */
    public enum State {
        NONE,
        NOT_LOADED,
        /**
         * Сущность была загружена когда-либо, однако и превью, и полноразмерное изображение были
         * удалены по таймауту либо при очистке кеша.
         */
        ONCE_LOADED,
        /**
         * Сущность имеет кешированное превью, но полноразмерное изображение было удалено по таймауту
         */
        THUMBNAIL,
        /**
         * Сущность имеет кешированное превью и полноразмерное изображение
         */
        IMAGE
    }
}
