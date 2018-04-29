package com.zavijavasoft.jaacad;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.CancelledDownloadException;
import com.yandex.disk.rest.exceptions.DownloadNoSpaceAvailableException;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import com.zavijavasoft.jaacad.utils.PersistenceManager;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;

public class CoreService extends IntentService implements ProgressListener {

    public static final String TAG = CoreService.class.getCanonicalName();

    public static final String KEY_REQUEST_ID = "com.zavijavasoft.jaacad.REQUEST_ID";
    public static final String KEY_INTENT_RECEIVER = "com.zavijavasoft.jaacad.INTENT_RECEIVER";
    public static final String KEY_INTENT_QUERY_TYPE = "com.zavijavasoft.jaacad.INTENT_QUERY_TYPE";
    public static final String KEY_INTENT_CACHELIST = "com.zavijavasoft.jaacad.INTENT_CACHELIST";
    public static final String KEY_REQUEST_RESULT = "com.zavijavasoft.jaacad.REQUEST_RESULT";
    public static final String KEY_RESULT_GALLERY_ENTITY = "com.zavijavasoft.jaacad.RESULT_GALLERY_ENTITY";
    public static final String KEY_RESULT_CACHE_SIZE = "com.zavijavasoft.jaacad.RESULT_CACHE_SIZE";
    public static final String KEY_RESULT_NETWORK_EXCEPTION = "com.zavijavasoft.jaacad.RESULT_NETWORK_EXCEPTION";
    public static final String KEY_RESULT_IMAGE_DOWNLOAD_PROGRESS = "com.zavijavasoft.jaacad.RESULT_IMAGE_DOWNLOAD_PROGRESS";
    public static final String KEY_RESULT_IMAGE_DOWNLOAD_ID = "com.zavijavasoft.jaacad.RESULT_IMAGE_DOWNLOAD_ID";
    public static final int CHECK_INTERNET_CONNECTION = 0;
    public static final int LOAD_LAST_100_AUTHORIZED = 1;
    public static final int LOAD_FIRST_100_AUTHORIZED = 2;
    public static final int LOAD_LAST_100_PUBLIC = 3;
    public static final int LOAD_FIRST_100_PUBLIC = 4;
    public static final int LOAD_RANDOM_100_AUTHORIZED = 5;
    public static final int LOAD_CACHED = 6;
    public static final int LOAD_SINGLE_THUMBNAIL = 50;
    public static final int LOAD_SINGLE_IMAGE = 51;
    public static final int CALCULATE_KEY_COLOR = 52;
    public static final int INTERNET_OK = 200;
    public static final int INTERNET_LOST = 201;
    public static final int INTERNET_ERROR = 202;
    public static final int NETWORK_EXCEPTION = 400;
    public static final int CACHE_SIZE = 1;
    public static final int CACHED_ENTITY_LOADED = 2;
    public static final int THUMBNAIL_LOADED = 3;
    public static final int RESOURCE_MISSED = 4;
    public static final int IMAGE_LOADED = 5;
    public static final int IMAGE_LOADING_PROGRESS = 6;

    private final PersistenceManager persistenceManager = new PersistenceManager();
    private boolean checkInternetPending = false;
    private ResultReceiver resultReceiver;



    public CoreService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        AuthService authService = AuthService.getInstance(context);
        persistenceManager.loadCachedGallery(authService.isAuthorized(), context.getFilesDir());
    }

    @Override
    public void onDestroy() {
        Context context = getApplicationContext();
        AuthService authService = AuthService.getInstance(context);
        persistenceManager.saveCachedGallery(authService.isAuthorized(), context.getFilesDir());
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        resultReceiver = intent.getParcelableExtra(KEY_INTENT_RECEIVER);
        int queryType = intent.getIntExtra(KEY_INTENT_QUERY_TYPE, CHECK_INTERNET_CONNECTION);

        switch (queryType) {
            case CHECK_INTERNET_CONNECTION:
                if (!checkInternetPending) {
                    checkInternetPending = true;
                    checkInternetConnection(resultReceiver);
                }
                break;
            case LOAD_LAST_100_AUTHORIZED:
                loadLastAuthorized(100, resultReceiver);
                break;
            case LOAD_CACHED:
                loadCache(resultReceiver);
                break;
            case LOAD_SINGLE_THUMBNAIL: {
                String resourceId = intent.getStringExtra(KEY_REQUEST_ID);
                downloadAuthorizedThumbnail(resultReceiver, resourceId);
                break;
            }
            case LOAD_SINGLE_IMAGE: {
                String resourceId = intent.getStringExtra(KEY_REQUEST_ID);
                downloadAuthorizedImage(resultReceiver, resourceId);
                break;
            }
        }

    }


    private void loadCache(ResultReceiver receiver) {

        Bundle bundleMeta = new Bundle();
        List<GalleryEntity> entities = persistenceManager.getEntities();
        bundleMeta.putInt(KEY_RESULT_CACHE_SIZE, entities.size());
        receiver.send(CACHE_SIZE, bundleMeta);

        for (GalleryEntity ge : entities) {
            Bundle bundleResource = new Bundle();
            bundleResource.putParcelable(KEY_RESULT_GALLERY_ENTITY, ge);
            receiver.send(CACHED_ENTITY_LOADED, bundleResource);
        }
    }

    private void loadLastAuthorized(final int limit, ResultReceiver receiver) {
        Context context = getApplicationContext();
        AuthService authService = AuthService.getInstance(context);
        RestClient restClient = RestClientUtil.getInstance(authService.getCredentials());

        try {

            ResourceList list = restClient.getLastUploadedResources(new ResourcesArgs.Builder()
                    .setMediaType("image")
                    .setLimit(limit)
                    .setPreviewSize("M")
                    .build());

            sendThumbnailsInfo(receiver, list);

        } catch (IOException e) {
            sendNetworkException(receiver, e);
        } catch (ServerIOException e) {
            sendNetworkException(receiver, e);
        } catch (DownloadNoSpaceAvailableException e) {
            sendNetworkException(receiver, e);
        } catch (CancelledDownloadException e) {
            sendNetworkException(receiver, e);
        }


    }


    private void downloadAuthorizedImage(ResultReceiver receiver, String resourceId){
        Context context = getApplicationContext();
        AuthService authService = AuthService.getInstance(context);
        RestClient restClient = RestClientUtil.getInstance(authService.getCredentials());
        List<GalleryEntity> entities = persistenceManager.getEntities();
        GalleryEntity geFound = null;
        for (GalleryEntity ge : entities) {
            if (ge.getResourceId().equals(resourceId)) {
                geFound = ge;
            }
        }
        Bundle bundle = new Bundle();
        if (geFound == null) {
            bundle.putString(KEY_REQUEST_ID, resourceId);
            receiver.send(RESOURCE_MISSED, bundle);
            return;
        }

        try {

            File result = new File(context.getFilesDir(),
                    new File("jaacad_" + resourceId + "_image_" + geFound.getFileName()).getName());
            if (!result.exists())
                restClient.downloadFile(geFound.getImageUrl(), result, this);

            geFound.setPathToImage(result.getAbsolutePath().toString());
            bundle.putParcelable(KEY_RESULT_GALLERY_ENTITY, geFound);
            receiver.send(IMAGE_LOADED, bundle);

        } catch (ServerIOException e) {
            sendNetworkException(receiver, e);
        } catch (DownloadNoSpaceAvailableException e) {
            sendNetworkException(receiver, e);
        } catch (CancelledDownloadException e) {
            sendNetworkException(receiver, e);
        } catch (ServerException e) {
            sendNetworkException(receiver, e);
        } catch (IOException e) {
            sendNetworkException(receiver, e);
        }

    }

    private void downloadAuthorizedThumbnail(ResultReceiver receiver, String resourceId) {
        Context context = getApplicationContext();
        AuthService authService = AuthService.getInstance(context);
        RestClient restClient = RestClientUtil.getInstance(authService.getCredentials());

        List<GalleryEntity> entities = persistenceManager.getEntities();
        GalleryEntity geFound = null;
        for (GalleryEntity ge : entities) {
            if (ge.getResourceId().equals(resourceId)) {
                geFound = ge;
            }
        }
        Bundle bundle = new Bundle();
        if (geFound == null) {
            bundle.putString(KEY_REQUEST_ID, resourceId);
            receiver.send(RESOURCE_MISSED, bundle);
            return;
        }

        try {
            Resource r = restClient.getResources(new ResourcesArgs.Builder()
                    .setPath(geFound.getImageUrl())
                    .setPreviewSize("M")
                    .build());
            if (r.getMd5() == null) {
                bundle.putString(KEY_REQUEST_ID, resourceId);
                receiver.send(RESOURCE_MISSED, bundle);
                return;
            }
            if (!r.getMd5().equals(resourceId)) {
                bundle.putString(KEY_REQUEST_ID, resourceId);
                receiver.send(RESOURCE_MISSED, bundle);
            }

            GalleryEntity ge = loadAuthorizedGalleryEntity(geFound, receiver, r);

            bundle.putParcelable(KEY_RESULT_GALLERY_ENTITY, ge);
            receiver.send(THUMBNAIL_LOADED, bundle);

        } catch (ServerIOException e) {
            sendNetworkException(receiver, e);
        } catch (DownloadNoSpaceAvailableException e) {
            sendNetworkException(receiver, e);
        } catch (CancelledDownloadException e) {
            sendNetworkException(receiver, e);
        } catch (IOException e) {
            sendNetworkException(receiver, e);
        }
    }

    private void sendThumbnailsInfo(ResultReceiver receiver, ResourceList list)
            throws CancelledDownloadException, DownloadNoSpaceAvailableException, HttpCodeException, IOException {
        List<GalleryEntity> entities = persistenceManager.getEntities();
        entities.clear();

        for (Resource r : list.getItems()) {
            Bundle bundleResource = new Bundle();
            GalleryEntity ge = loadAuthorizedGalleryEntity(null, receiver, r);
            bundleResource.putParcelable(KEY_RESULT_GALLERY_ENTITY, ge);
            receiver.send(THUMBNAIL_LOADED, bundleResource);
            entities.add(ge);
        }
    }

    private GalleryEntity loadAuthorizedGalleryEntity(GalleryEntity ge, ResultReceiver receiver, Resource r)
            throws CancelledDownloadException, DownloadNoSpaceAvailableException, HttpCodeException, IOException {
        Context context = getApplicationContext();
        AuthService authService = AuthService.getInstance(context);
        RestClient restClient = RestClientUtil.getInstance(authService.getCredentials());
        File result = new File(context.getFilesDir(),
                new File("jaacad_" + r.getMd5()
                        + "_thumbnail_" + r.getName()).getName());
        if (!result.exists())
            restClient.downloadUrl(r.getPreview(), result, this);

        if (ge == null)
            ge = new GalleryEntity();
        ge.setState(GalleryEntity.State.THUMBNAIL);
        ge.setLoadedDateTime(new Date());
        ge.setResourceId(r.getMd5());
        ge.setImageUrl(r.getPath().getPath());
        ge.setThumbnailUrl(r.getPreview());
        ge.setFileName(r.getName());
        ge.setPathToThumbnail(result.getAbsolutePath().toString());

        return ge;
    }

    private void sendNetworkException(ResultReceiver receiver, Exception e) {
        Bundle bundleError = new Bundle();
        bundleError.putString(KEY_RESULT_NETWORK_EXCEPTION, e.getLocalizedMessage());
        e.printStackTrace();
        receiver.send(NETWORK_EXCEPTION, bundleError);
    }


    private void checkInternetConnection(ResultReceiver receiver) {

        Context context = getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        // проверка подключения
        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                // тест доступности внешнего ресурса
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setRequestProperty("User-Agent", "test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1000); // Timeout в секундах
                urlc.connect();
                int nResponse = urlc.getResponseCode();
                // статус ресурса OK
                if (nResponse == 200) {
                    receiver.send(INTERNET_OK, null);
                    checkInternetPending = false;
                    return;
                }
                // иначе проверка провалилась
            } catch (IOException e) {
                Log.d("my_tag", "Ошибка проверки подключения к интернету", e);
                sendNetworkException(receiver, e);
                checkInternetPending = false;
                return;
            }
        }
        checkInternetPending = false;
        receiver.send(INTERNET_LOST, null);
    }

    @Override
    public void updateProgress(long loaded, long total) {
        if (total != 0){
            Bundle bundle = new Bundle();
            bundle.putLong(KEY_RESULT_IMAGE_DOWNLOAD_PROGRESS, (100 * loaded) / total);
            resultReceiver.send(IMAGE_LOADING_PROGRESS, bundle);
        }
    }

    @Override
    public boolean hasCancelled() {
        return false;
    }


}
