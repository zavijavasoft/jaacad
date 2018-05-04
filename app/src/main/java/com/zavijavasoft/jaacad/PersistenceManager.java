package com.zavijavasoft.jaacad.utils;

import com.zavijavasoft.jaacad.GalleryEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

public class PersistenceManager {

    public static final String JAACAD_GALLERY_JSON = "jaacad.gallery.json";
    public static final int THUMBNAIL_EXPIRY_PERIOD = 3; // hours
    public static final int IMAGE_EXPIRY_PERIOD = 1; // hour
    public static final int IMAGE_CACHE_SIZE = 100;
    public static final int THUMBNAIL_CACHE_SIZE = 500;

    private final List<GalleryEntity> entities = new LinkedList<>();
    private final Deque<String> imageCache = new ArrayDeque<>(IMAGE_CACHE_SIZE);
    private final Deque<String> thumbnailCache = new ArrayDeque<>(THUMBNAIL_CACHE_SIZE);


    public List<GalleryEntity> getEntities() {
        return entities;
    }

    public void loadCachedGallery(boolean bAuthorized, File filesDir) {
        FileInputStream is = null;
        Date now = new Date();
        try {
            File fJson = new File(filesDir, JAACAD_GALLERY_JSON);
            is = new FileInputStream(fJson);
            byte[] data = new byte[is.available()];
            is.read(data);


            JSONObject doc = new JSONObject(new String(data));
            boolean cacheAuthorized = doc.getBoolean("authorized");
            imageCache.clear();
            JSONArray arrImgCache = (JSONArray) doc.get("imageCache");
            for (int i = 0; i < arrImgCache.length(); i++) {
                imageCache.push((String) arrImgCache.get(i));
            }
            thumbnailCache.clear();
            JSONArray arrThumbCache = (JSONArray) doc.get("thumbnailsCache");

            for (int i = 0; i < arrThumbCache.length(); i++) {
                thumbnailCache.push((String) arrThumbCache.get(i));
            }
            entities.clear();
            // Тут сверяются состояния авторизации приложения на момент сохранения кэша и текущее.
            // Если при сохранении приложение было авторизовано, то в кэше содержатся защищенные
            // ссылки на превью. Неавторизованное приложение их не сможет отобразить, поэтому смысла
            // их считывать нет.
            if (bAuthorized || !cacheAuthorized) {
                JSONArray arr = (JSONArray) doc.get("entities");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject ent = arr.getJSONObject(i);
                    GalleryEntity e = new GalleryEntity();
                    e.setResourceId((String) ent.get("resourceId"));
                    e.setKeyColor((Integer) ent.get("keyColor"));
                    e.setPathToThumbnail((String) ent.get("pathToPreview"));
                    e.setPathToImage(ent.optString("pathToImage"));
                    e.setThumbnailUrl((String) ent.get("thumbnailUrl"));
                    e.setImageUrl((String) ent.get("imageUrl"));
                    e.setFileName((String) ent.get("fileName"));

                    Date loadedDateTime = new Date((long) ent.get("loadedDateTime"));
                    e.setLoadedDateTime(loadedDateTime);
                    e.setState(GalleryEntity.State.IMAGE);

                    Calendar cal = new GregorianCalendar();
                    cal.setTime(loadedDateTime);
                    cal.add(Calendar.HOUR, IMAGE_EXPIRY_PERIOD);
                    if (!e.getPathToImage().isEmpty() && now.after(cal.getTime())) {
                        File fImage = new File(e.getPathToImage());
                        fImage.delete();
                        e.setState(GalleryEntity.State.THUMBNAIL);
                    }
                    cal.setTime(loadedDateTime);
                    cal.add(Calendar.HOUR, THUMBNAIL_EXPIRY_PERIOD);
                    if (now.after(cal.getTime())) {
                        File fThumbnail = new File(e.getPathToThumbnail());
                        fThumbnail.delete();
                        e.setState(GalleryEntity.State.ONCE_LOADED);
                    }
                    entities.add(e);
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveCachedGallery(boolean bAuthorized, File filesDir) {
        FileOutputStream os = null;
        try {
            JSONObject doc = new JSONObject();

            doc.put("authorized", bAuthorized);

            JSONArray arrImgCache = new JSONArray();
            for (String s : imageCache) {
                arrImgCache.put(s);
            }
            doc.put("imageCache", arrImgCache);

            JSONArray arrThumbCache = new JSONArray();
            for (String s : thumbnailCache) {
                arrThumbCache.put(s);
            }
            doc.put("thumbnailsCache", arrThumbCache);

            JSONArray arr = new JSONArray();
            for (GalleryEntity e : entities) {
                JSONObject ent = new JSONObject();
                ent.put("resourceId", e.getResourceId());
                ent.put("keyColor", e.getKeyColor());
                ent.put("pathToPreview", e.getPathToThumbnail());
                ent.put("pathToImage", e.getPathToImage());
                ent.put("thumbnailUrl", e.getThumbnailUrl());
                ent.put("imageUrl", e.getImageUrl());
                ent.put("fileName", e.getFileName());
                ent.put("loadedDateTime", e.getLoadedDateTime().getTime());
                arr.put(ent);
            }
            doc.put("entities", arr);

            File fJson = new File(filesDir, JAACAD_GALLERY_JSON);
            os = new FileOutputStream(fJson);
            os.write(doc.toString().getBytes());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void checkStates(){
        for(GalleryEntity e : entities){
            if (e.getState() == GalleryEntity.State.IMAGE){
                if (new File(e.getPathToImage()).exists())
                    continue;
                e.setState(GalleryEntity.State.THUMBNAIL);
            }
            if (e.getState() == GalleryEntity.State.THUMBNAIL){
                if (new File(e.getPathToThumbnail()).exists())
                    continue;
                e.setState(GalleryEntity.State.ONCE_LOADED);
            }
        }
    }

    public void clearCaches(){
        while(!imageCache.isEmpty()){
            String toDelete = imageCache.pollLast();
            File file = new File(toDelete);
            file.delete();
        }
        while(!thumbnailCache.isEmpty()){
            String toDelete = thumbnailCache.pollLast();
            File file = new File(toDelete);
            file.delete();
        }
    }

    private void removeDuplicate(String filename, Deque<String> queue){
        for (String s : queue){
            if (s.equals(filename))
                queue.remove(s);
        }
    }

    public void addFileToCache(String fileName, boolean isThumbnail){
        Deque<String> queue = imageCache;
        int max = IMAGE_CACHE_SIZE;
        if (isThumbnail) {
            queue = thumbnailCache;
            max = THUMBNAIL_CACHE_SIZE;
        }

        removeDuplicate(fileName, queue);
        queue.addFirst(fileName);
        if (queue.size() > max){
            String sExceeded = queue.pollLast();
            File file = new File(sExceeded);
            file.delete();
            checkStates();
        }
    }

}
