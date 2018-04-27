package com.zavijavasoft.jaacad;

import android.content.Context;

import com.yandex.disk.rest.json.Resource;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CacheManager {

    public static final String JAACAD_GALLERY_JSON = "jaacad.gallery.json";
    public static final int THUMBNAIL_EXPIRY_PERIOD = 3; // hours
    public static final int IMAGE_EXPIRY_PERIOD = 1; // hour
    public static final int IMAGE_CACHE_SIZE = 100;
    public static final int THUMBNAIL_CACHE_SIZE = 500;

    private Context context;
    private final List<GalleryEntity> entities = new LinkedList<>();
    private final Deque<String> imageCache = new ArrayDeque<>(IMAGE_CACHE_SIZE);
    private final Deque<String> thumbnailCache = new ArrayDeque<>(THUMBNAIL_CACHE_SIZE);

    public static CacheManager service = null;
    private Random random;

    public static CacheManager getInstance(Context context) {
        if (service == null) {
            service = new CacheManager(context);
        }
        return service;
    }


    public CacheManager(Context context) {
        this.context = context;
        random = new Random(new Date().getTime());
    }


    /***
     * Сохраняет кэш галереи в JSON-документ
     * @param bAuthorized
     */
    public void saveCachedGallery(boolean bAuthorized) {
        FileOutputStream os = null;
        try {
            JSONObject doc = new JSONObject();

            doc.put("authorized", bAuthorized);

            JSONArray arrImgCache = new JSONArray();
            for(String s : imageCache){
                arrImgCache.put(s);
            }
            doc.put("imageCache", arrImgCache);

            JSONArray arrThumbCache = new JSONArray();
            for(String s : thumbnailCache){
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

            File fJson = new File(context.getFilesDir(), JAACAD_GALLERY_JSON);
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

    /***
     * Загружает кэш галереи из JSON-документа
     * @param bAuthorized Флаг, указывающий, что приложение авторизовано/не авторизовано в момент
     *                    вызова метода.
     */
    public void loadCachedGallery(boolean bAuthorized) {
        FileInputStream is = null;
        Date now = new Date();
        try {
            File fJson = new File(context.getFilesDir(), JAACAD_GALLERY_JSON);
            is = new FileInputStream(fJson);
            byte [] data = new byte[is.available()];
            is.read(data);


            JSONObject doc = new JSONObject(new String(data));
            boolean cacheAuthorized = doc.getBoolean("authorized");
            imageCache.clear();
            JSONArray arrImgCache = (JSONArray)doc.get("imageCache");
            for(int i = 0; i < arrImgCache.length(); i++){
                imageCache.push((String) arrImgCache.get(i));
            }
            thumbnailCache.clear();
            JSONArray arrThumbCache = (JSONArray)doc.get("thumbnailsCache");

            for(int i = 0; i < arrThumbCache.length(); i++){
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
                    e.setPathToImage( ent.optString("pathToImage"));
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

    /**
     *  Фильтрация потока входящей метаинформации о файлах.
     *  Метод получает список всех полученных запросом API Диска файлов и отбрасывает те, для которых не
     *  нужно загружать превью (у них просто обновляется время скачивания)
     *  Кроме того, этот метод удаляет из галереи ссылки на те изображения, которых нет в этом запросе.
     *  Особенность в следующем: удаление ссылок на изображения не приводит к удалению самих файлов превью
     *  или полного изображения. Сделано это по следующим соображениям - если пользователь захочет повторить
     *  предыдущий запрос, нет смысла заново грузить картинки. Если не захочет -- закэшированные изображения
     *  удалятся механизмом очередей кэшей.
     * */
    public List<Resource> filterNeedToBeCached(List<Resource> lstAll){

        // Результирующий список отфильтрованных ресурсов
        List<Resource> lstFiltered = new LinkedList<>();

        // Формируем отображение записей галереи по ключу - идентификатору изображения
        Map<String, GalleryEntity> map = new HashMap<>();
        for (GalleryEntity e: entities){
            map.put(e.getResourceId(), e);
        }

        // Проходим по всем ресурсам
        for(Resource r : lstAll){
            String id = r.getMd5();
            // Если в галерее есть картинка с таким идентификатором, проверяем, чтобы ее статус
            // был ONCE_LOADED, то есть картинка когда-то была загружена, но ее изображения удалены из
            // кэша. Такую картинку надо заново перезакачивать.
            if (map.containsKey(id)){
                if (map.get(id).getState() == GalleryEntity.State.ONCE_LOADED)
                    lstFiltered.add(r);
                else
                    // в картинках с другими статусами обновляем время скачивания
                    map.get(id).setLoadedDateTime(new Date());
            }else
                // Если картинки в галерее нет, добавляем ее в список на закачку
                lstFiltered.add(r);
        }

        // Для нахождения картинок, которые есть в галерее, но отсутствуют в запросе, создаем
        // отображение уже ресурсов запроса по ключу идентификатора
        Map<String, Resource> mapRes = new HashMap<>();
        for (Resource res : lstAll){
            mapRes.put(res.getMd5(), res);
        }


        // Удаляем те картинки из галереи, идентификаторы которых отсутствуют в запросе
        boolean bChanged;
        do{
            bChanged = false;
            for (GalleryEntity e : entities) {
                if (!mapRes.containsKey(e.getResourceId())) {
                    entities.remove(e);
                    bChanged = true;
                }
            }
        }while (bChanged);

        return lstFiltered;
    }


    public List<GalleryEntity> getEntities() {
        return entities;
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
