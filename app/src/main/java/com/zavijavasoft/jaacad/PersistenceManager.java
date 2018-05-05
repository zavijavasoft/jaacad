package com.zavijavasoft.jaacad;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/***
 * Класс PersistenceManager отвечает за кэширование данных запросов.
 * Информация о кэше находится в JSON-файле конфигурации, сами изображения лежат
 * в файловой папке приложения.
 * Файлы изображений удаляются как по времени, так и при превышении определенного количества.
 */
public class PersistenceManager {

    public static final String JAACAD_GALLERY_JSON = "jaacad.gallery.json";// Имя файла конфигурации
    public static final int IMAGE_EXPIRY_PERIOD = 3; // Период существования в кэше полноразмерных изображений, в часах
    public static final int IMAGE_CACHE_SIZE = 100; // Максимальное количество полноразмерных изображений в кэше
    public static final int THUMBNAIL_CACHE_SIZE = 500; // Максимальное количество превью в кэше


    // Список с элементами кэша
    private final List<GalleryEntity> entities = new LinkedList<>();
    // Очередь с именами файлов изображений
    private final Deque<String> imageCache = new ArrayDeque<>(IMAGE_CACHE_SIZE);
    // Очередь с именами файлов превью
    private final Deque<String> thumbnailCache = new ArrayDeque<>(THUMBNAIL_CACHE_SIZE);
    // Множество имен файлов превью и изображений для избежания дублирования
    private final Set<String> cacheUniqueGuarantee = new HashSet<>();

    /**
     * Возвращает текущей список кэшированных элементов.
     *
     * @return связанный список кэшированных элементов.
     */
    public List<GalleryEntity> getEntities() {
        return entities;
    }

    /**
     * Загружает список кэшированных элементов из файла конфигурации.
     *
     * @param bAuthorized флаг того, что приложение авторизовано на Я.Диске. Его значение влияет на
     *                    то, будет ли кэш отфильтрован после загрузки по признаку авторизованности
     *                    изображений
     * @param filesDir    директория, где находится файл конфигурации
     */
    public void loadCachedGallery(boolean bAuthorized, File filesDir) {
        FileInputStream is = null;
        Date now = new Date();
        try {
            // Считываем файл
            File fJson = new File(filesDir, JAACAD_GALLERY_JSON);
            is = new FileInputStream(fJson);
            byte[] data = new byte[is.available()];
            is.read(data);


            // Парсим строку в объект
            JSONObject doc = new JSONObject(new String(data));
            boolean cacheAuthorized = doc.getBoolean("authorized");

            cacheUniqueGuarantee.clear();
            imageCache.clear();
            // Читаем имена файлов изображений
            JSONArray arrImgCache = (JSONArray) doc.get("imageCache");
            for (int i = 0; i < arrImgCache.length(); i++) {
                String e = (String) arrImgCache.get(i);
                imageCache.push(e);
                cacheUniqueGuarantee.add(e);
            }
            thumbnailCache.clear();
            // Читаем имена файлов превью
            JSONArray arrThumbCache = (JSONArray) doc.get("thumbnailsCache");
            for (int i = 0; i < arrThumbCache.length(); i++) {
                String e = (String) arrThumbCache.get(i);
                thumbnailCache.push(e);
                cacheUniqueGuarantee.add(e);

            }
            entities.clear();
            // Тут сверяются состояния авторизации приложения на момент сохранения кэша и текущее.
            // Если при сохранении приложение было авторизовано, то в кэше содержатся защищенные
            // ссылки на превью. Неавторизованное приложение их не сможет отобразить, поэтому смысла
            // их считывать нет.
            if (bAuthorized || !cacheAuthorized) {
                JSONArray arr = (JSONArray) doc.get("entities");
                for (int i = 0; i < arr.length(); i++) {
                    // Заполняем GalleryEntity из JSON
                    JSONObject ent = arr.getJSONObject(i);
                    GalleryEntity e = new GalleryEntity();
                    e.setPublicKey((String) ent.get("publicKey"));
                    e.setResourceId((String) ent.get("resourceId"));
                    e.setKeyColor((Integer) ent.get("keyColor"));
                    e.setPathToThumbnail((String) ent.get("pathToPreview"));
                    // optString() - потому что пути к имени изображения может и не быть,
                    // если пользователь его не открывал
                    e.setPathToImage(ent.optString("pathToImage"));
                    e.setThumbnailUrl((String) ent.get("thumbnailUrl"));
                    e.setImageUrl((String) ent.get("imageUrl"));
                    e.setFileName((String) ent.get("fileName"));

                    Date loadedDateTime = new Date((long) ent.get("loadedDateTime"));
                    e.setLoadedDateTime(loadedDateTime);
                    e.setState(GalleryEntity.State.IMAGE);

                    // Удаляем устаревшие файлы изображений
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(loadedDateTime);
                    cal.add(Calendar.HOUR, IMAGE_EXPIRY_PERIOD);
                    if (!e.getPathToImage().isEmpty() && now.after(cal.getTime())) {
                        File fImage = new File(e.getPathToImage());
                        fImage.delete();
                        e.setState(GalleryEntity.State.THUMBNAIL);
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
            // Закрываем файл в стиле Java 1.6 (черт меня дернул пожадничать и установить API 15)
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
     * Сохраняет список кэшированных элементов в файл конфигурации
     *
     * @param bAuthorized флаг того, что приложение авторизовано на Я.Диске
     * @param filesDir    директория, где находится файл конфигурации
     */
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
                // Если элемент был помечен как "мертвый", например, если файл превью не удалось скачать,
                //  он не будет записан в кэш. Это позволяет не изменять сам список.
                if (e.isMarkedAsDead())
                    continue;
                JSONObject ent = new JSONObject();
                ent.put("publicKey", e.getPublicKey());
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
                    // Закрываем файл в стиле Java 1.6 ((((
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Возвращает элемент списка по его идентификатору. Элементы, помеченные как "мертвые", не
     * возвращаются.
     *
     * @param id идентификатор элемента GalleryEntity
     * @return найденный элемент или null, если он отсутствует или "мертв"
     */
    public GalleryEntity findEntityById(String id) {
        for (GalleryEntity e : entities) {
            if (e.isMarkedAsDead())
                continue;
            if (id.equals(e.getResourceId()))
                return e;
        }
        return null;
    }

    /**
     * Проверяет состояния элементов списка в зависимости от наличия файлов изображений.
     * Если соответствущего файла нет, статус элемента понижается
     */
    private void checkStates() {
        for (GalleryEntity e : entities) {
            if (e.getState() == GalleryEntity.State.IMAGE) {
                if (new File(e.getPathToImage()).exists())
                    continue;
                e.setState(GalleryEntity.State.THUMBNAIL);
            }
            if (e.getState() == GalleryEntity.State.THUMBNAIL) {
                if (new File(e.getPathToThumbnail()).exists())
                    continue;
                e.setState(GalleryEntity.State.ONCE_LOADED);
            }
        }
    }

    /**
     * Чистит кэш. Удаляет все файлы превью и изображений (по маске),
     * чистит очереди. Список эелементов остается.
     *
     * @param directory директория, где находятся файлы
     */
    public void clearCaches(File directory) {

        String[] files = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith("jaacad_")) {
                    if (name.contains("_thumbnail_"))
                        return true;
                    if (name.contains("_image_"))
                        return true;
                }
                return false;
            }
        });

        for (String s : files) {
            File fl = new File(directory, s);
            fl.delete();
        }
        cacheUniqueGuarantee.clear();
        imageCache.clear();
        thumbnailCache.clear();
        checkStates();
    }

    /**
     * Добавляет файл превью или изображения в очередь. Если размер соответствующей очереди будет
     * превышен, первый файл будет удален.
     * @param fileName - имя файла, которое будет кэшировано
     * @param isThumbnail - флаг превью (для того, чтобы выбрать нужную очередь)
     */
    public void addFileToCache(String fileName, boolean isThumbnail) {
        // Выбираем очередь
        Deque<String> queue = imageCache;
        int max = IMAGE_CACHE_SIZE;
        if (isThumbnail) {
            queue = thumbnailCache;
            max = THUMBNAIL_CACHE_SIZE;
        }

        // Избегаем дублирования
        if (cacheUniqueGuarantee.contains(fileName))
            return;

        // Вставляем имя файла в очередь
        queue.addFirst(fileName);
        // .. и в множество имен
        cacheUniqueGuarantee.add(fileName);
        // Если очередь превысила максимум, удаляем первый файл
        if (queue.size() > max) {
            String sExceeded = queue.pollLast();
            cacheUniqueGuarantee.remove(sExceeded);
            File file = new File(sExceeded);
            file.delete();
            checkStates();
        }
    }

}
