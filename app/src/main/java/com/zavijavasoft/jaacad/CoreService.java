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
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import com.zavijavasoft.jaacad.auth.AuthService;
import com.zavijavasoft.jaacad.auth.Credentials;
import com.zavijavasoft.jaacad.auth.LoginApi;
import com.zavijavasoft.jaacad.auth.LoginResponse;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit.RestAdapter;

public class CoreService extends IntentService implements ProgressListener {

    public static final String TAG = CoreService.class.getCanonicalName();

    public static final String KEY_REQUEST_ID = "com.zavijavasoft.jaacad.REQUEST_ID";
    public static final String KEY_INTENT_RECEIVER = "com.zavijavasoft.jaacad.INTENT_RECEIVER";
    public static final String KEY_INTENT_QUERY_TYPE = "com.zavijavasoft.jaacad.INTENT_QUERY_TYPE";
    public static final String KEY_RESULT_GALLERY_ENTITY = "com.zavijavasoft.jaacad.RESULT_GALLERY_ENTITY";
    public static final String KEY_RESULT_SIZE = "com.zavijavasoft.jaacad.RESULT_CACHE_SIZE";
    public static final String KEY_RESULT_NETWORK_EXCEPTION = "com.zavijavasoft.jaacad.RESULT_NETWORK_EXCEPTION";
    public static final String KEY_RESULT_IMAGE_DOWNLOAD_PROGRESS = "com.zavijavasoft.jaacad.RESULT_IMAGE_DOWNLOAD_PROGRESS";
    public static final String KEY_INTENT_AUTH_TOKEN = "com.zavijavasoft.jaacad.INTENT_AUTH_TOKEN";
    public static final String KEY_RESULT_LOGIN_DISPLAY_NAME = "com.zavijavasoft.jaacad.RESULT_LOGIN_DISPLAY_NAME";
    public static final String KEY_RESULT_LOGIN_AVATAR_FILENAME = "com.zavijavasoft.jaacad.RESULT_AVATAR_FILENAME";
    public static final String KEY_RESULT_LOGIN_PRETTY_NAME = "com.zavijavasoft.jaacad.RESULT_PRETTY_NAME";
    public static final String KEY_RESULT_FILE_SIZE = "com.zavijavasoft.jaacad.RESULT_FILE_SIZE";

    // Запросы от активностей к службе
    public static final int CHECK_INTERNET_CONNECTION = 0;
    public static final int LOAD_LAST_100_AUTHORIZED = 1;
    public static final int LOAD_FIRST_100_AUTHORIZED = 2;
    public static final int LOAD_LAST_100_PUBLIC = 3;
    public static final int LOAD_FIRST_100_PUBLIC = 4;
    public static final int LOAD_RANDOM_100_AUTHORIZED = 5;
    public static final int LOAD_CACHED = 6;
    public static final int GET_LOGIN_INFO = 7;
    public static final int CLEAR_CACHE = 8;
    public static final int LOAD_SINGLE_THUMBNAIL = 50;
    public static final int LOAD_SINGLE_IMAGE = 51;
    public static final int CALCULATE_KEY_COLOR = 52;

    // Ответы службы
    public static final int INTERNET_OK = 200;
    public static final int INTERNET_LOST = 201;
    public static final int INTERNET_ERROR = 202;
    public static final int NETWORK_EXCEPTION = 400;
    public static final int RESULT_SIZE = 1;
    public static final int CACHED_ENTITY_LOADED = 2;
    public static final int THUMBNAIL_LOADED = 3;
    public static final int RESOURCE_MISSED = 4;
    public static final int IMAGE_LOADED = 5;
    public static final int IMAGE_LOADING_PROGRESS = 6;
    public static final int LOGIN_INFO = 7;
    public static final int IMAGE_UNAVAILABLE = 8;
    public static final int CHUNK_LOADED = 9;

    public static final Map<Integer, Integer> mapQueryCmd = new HashMap<>();
    public static final Map<Integer, Integer> mapReQueryCmd = new HashMap<>();

    static {
        mapQueryCmd.put(LOAD_FIRST_100_AUTHORIZED, R.string.query_100_firstauthorized);
        mapQueryCmd.put(LOAD_LAST_100_AUTHORIZED, R.string.query_100_lastauthorized);
        mapQueryCmd.put(LOAD_FIRST_100_PUBLIC, R.string.query_100_firstpublic);
        mapQueryCmd.put(LOAD_LAST_100_PUBLIC, R.string.query_100_lastpublic);
        mapQueryCmd.put(LOAD_RANDOM_100_AUTHORIZED, R.string.query_100_randomauthorized);

        mapReQueryCmd.put(R.string.query_100_firstauthorized, LOAD_FIRST_100_AUTHORIZED);
        mapReQueryCmd.put(R.string.query_100_lastauthorized, LOAD_LAST_100_AUTHORIZED);
        mapReQueryCmd.put(R.string.query_100_firstpublic, LOAD_FIRST_100_PUBLIC);
        mapReQueryCmd.put(R.string.query_100_lastpublic, LOAD_LAST_100_PUBLIC);
        mapReQueryCmd.put(R.string.query_100_randomauthorized, LOAD_RANDOM_100_AUTHORIZED);
    }

    private final PersistenceManager persistenceManager = new PersistenceManager();
    private boolean checkInternetPending = false;
    private ResultReceiver resultReceiver;
    private File filesDirectory;
    private Credentials credentials;

    public CoreService() {
        super(TAG);

    }

    public static boolean queryNeedAuthorization(int cmd) {
        switch (cmd) {
            case LOAD_FIRST_100_AUTHORIZED:
            case LOAD_LAST_100_AUTHORIZED:
            case LOAD_RANDOM_100_AUTHORIZED:
                return true;
        }
        return false;
    }

    public static int getQueryResourceIdByCmd(int queryCmd) {
        Integer i = mapQueryCmd.get(queryCmd);
        if (i != null)
            return i;
        return 0;
    }

    public static int getQueryCmdByResourceId(int queryResourceId) {
        Integer i = mapReQueryCmd.get(queryResourceId);
        if (i != null)
            return i;
        return 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        filesDirectory = context.getFilesDir();
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

        credentials = AuthService.getInstance(getApplicationContext()).getCredentials();
        resultReceiver = intent.getParcelableExtra(KEY_INTENT_RECEIVER);
        int queryType = intent.getIntExtra(KEY_INTENT_QUERY_TYPE, CHECK_INTERNET_CONNECTION);

        switch (queryType) {
            case CHECK_INTERNET_CONNECTION:
                if (!checkInternetPending) {
                    checkInternetPending = true;
                    checkInternetConnection(resultReceiver);
                }
                break;
            case GET_LOGIN_INFO:
                String token = intent.getStringExtra(KEY_INTENT_AUTH_TOKEN);
                requreLoginInfo(resultReceiver, token);
                break;
            case LOAD_LAST_100_AUTHORIZED:
                loadLastAuthorized(100, resultReceiver);
                break;
            case LOAD_FIRST_100_AUTHORIZED:
                loadFirstAuthorized(100, resultReceiver);
                break;
            case LOAD_RANDOM_100_AUTHORIZED:
                loadRandomAuthorized(100, resultReceiver);
                break;
            case LOAD_LAST_100_PUBLIC:
                loadPublic(100, resultReceiver, true);
                break;
            case LOAD_FIRST_100_PUBLIC:
                loadPublic(100, resultReceiver, false);
                break;
            case LOAD_CACHED:
                loadCache(resultReceiver);
                break;
            case LOAD_SINGLE_THUMBNAIL: {
                String resourceId = intent.getStringExtra(KEY_REQUEST_ID);
                downloadThumbnail(resultReceiver, resourceId);
                break;
            }
            case LOAD_SINGLE_IMAGE: {
                String resourceId = intent.getStringExtra(KEY_REQUEST_ID);
                downloadImage(resultReceiver, resourceId);
                break;
            }
            case CLEAR_CACHE: {
                persistenceManager.clearCaches(getFilesDir());
            }
        }

    }

    private void loadCache(ResultReceiver receiver) {

        List<GalleryEntity> entities = persistenceManager.getEntities();
        int size = entities.size();
        sendQueryResultSize(receiver, size);

        for (GalleryEntity ge : entities) {
            if (ge.isMarkedAsDead())
                continue;
            Bundle bundleResource = new Bundle();
            bundleResource.putParcelable(KEY_RESULT_GALLERY_ENTITY, ge);
            receiver.send(CACHED_ENTITY_LOADED, bundleResource);
        }
    }

    private void sendQueryResultSize(ResultReceiver receiver, int size) {
        Bundle bundleMeta = new Bundle();
        bundleMeta.putInt(KEY_RESULT_SIZE, size);
        receiver.send(RESULT_SIZE, bundleMeta);
    }

    private void loadPublic(final int limit, ResultReceiver receiver, boolean last) {

        RestClient restClient = RestClientUtil.getInstance(AuthService.defaultCredentials());

        try {
            Resource resource = restClient.listPublicResources(new ResourcesArgs.Builder()
                    .setPublicKey(getResources().getString(R.string.default_public_url))
                    .setSort(last ? "-created" : "created")
                    .setPreviewSize("M")
                    .setMediaType("image")
                    .setLimit(limit)
                    .build());
            sendQueryResultSize(receiver, resource.getResourceList().getItems().size());
            sendThumbnailsInfo(receiver, resource.getResourceList().getItems(), true);
        } catch (Exception e) {
            handleException(e, receiver);
        }
    }

    private void loadRandomAuthorized(final int limit, ResultReceiver receiver) {

        Random random = new Random();
        random.setSeed(new Date().getTime());

        RestClient restClient = RestClientUtil.getInstance(credentials);
        final int CHUNK_LIMIT = 100;
        int offset = 0;
        List<Resource> listAll = new LinkedList<>();
        sendQueryResultSize(receiver, 1000);
        Bundle bundleMeta = new Bundle();
        try {

            while (true) {
                ResourceList list = restClient.getFlatResourceList(new ResourcesArgs.Builder()
                        .setMediaType("image")
                        .setLimit(CHUNK_LIMIT)
                        .setOffset(offset)
                        .setPreviewSize("M")
                        .build());
                listAll.addAll(list.getItems());

                offset += list.getItems().size();
                bundleMeta.putInt(KEY_RESULT_SIZE, 1);
                receiver.send(CHUNK_LOADED, bundleMeta);
                if (list.getItems().size() < CHUNK_LIMIT)
                    break;
            }
            bundleMeta.putInt(KEY_RESULT_SIZE, -1);
            receiver.send(CHUNK_LOADED, bundleMeta);

            int realSize = Math.min(limit, listAll.size());
            LinkedList<Resource> listOut = new LinkedList<>();

            for (int i = 0; i < realSize; i++) {
                int rand = random.nextInt(listAll.size());
                listOut.add(listAll.remove(rand));
            }


            sendQueryResultSize(receiver, listOut.size());
            sendThumbnailsInfo(receiver, listOut, false);

        } catch (Exception e) {
            handleException(e, receiver);
        }


    }


    private void loadFirstAuthorized(final int limit, ResultReceiver receiver) {

        RestClient restClient = RestClientUtil.getInstance(credentials);

        try {

            ResourceList list = restClient.getFlatResourceList(new ResourcesArgs.Builder()
                    .setMediaType("image")
                    .setLimit(limit)
                    .setSort("created")
                    .setPreviewSize("M")
                    .build());

            sendQueryResultSize(receiver, list.getItems().size());
            sendThumbnailsInfo(receiver, list.getItems(), false);

        } catch (Exception e) {
            handleException(e, receiver);
        }
    }


    private void loadLastAuthorized(final int limit, ResultReceiver receiver) {

        RestClient restClient = RestClientUtil.getInstance(credentials);

        try {

            ResourceList list = restClient.getLastUploadedResources(new ResourcesArgs.Builder()
                    .setMediaType("image")
                    .setLimit(limit)
                    .setPreviewSize("M")
                    .build());

            sendQueryResultSize(receiver, list.getItems().size());
            sendThumbnailsInfo(receiver, list.getItems(), false);

        } catch (Exception e) {
            handleException(e, receiver);
        }


    }


    private void downloadImage(ResultReceiver receiver, String resourceId) {

        RestClient restClient = RestClientUtil.getInstance(credentials);
        GalleryEntity geFound = persistenceManager.findEntityById(resourceId);
        Bundle bundle = new Bundle();

        try {

            File result = new File(filesDirectory,
                    new File("jaacad_" + resourceId + "_image_" + geFound.getFileName()).getName());
            if (!result.exists())
                if (geFound.getPublicKey().isEmpty())
                    restClient.downloadFile(geFound.getImageUrl(), result, this);
                else
                    restClient.downloadPublicResource(geFound.getPublicKey(), geFound.getImageUrl(), result, this);


            geFound.setLoadedDateTime(new Date());
            geFound.setPathToImage(result.getAbsolutePath().toString());
            persistenceManager.addFileToCache(geFound.getPathToImage(), false);
            bundle.putParcelable(KEY_RESULT_GALLERY_ENTITY, geFound);
            receiver.send(IMAGE_LOADED, bundle);

        } catch (Exception e) {
            receiver.send(IMAGE_UNAVAILABLE, bundle);
        }

    }

    private void sendMissingResource(ResultReceiver receiver) {
        Bundle bundle = new Bundle();
        receiver.send(RESOURCE_MISSED, bundle);
    }

    private void downloadThumbnail(ResultReceiver receiver, String resourceId) {

        RestClient restClient = RestClientUtil.getInstance(credentials);

        GalleryEntity geFound = persistenceManager.findEntityById(resourceId);

        Bundle bundle = new Bundle();

        try {
            Resource r = restClient.getResources(new ResourcesArgs.Builder()
                    .setPath(geFound.getImageUrl())
                    .setPreviewSize("M")
                    .build());
            if (r.getMd5() == null) {
                geFound.setMarkedAsDead(true);
                sendMissingResource(receiver);
                return;
            }
            if (!r.getMd5().equals(resourceId)) {
                sendMissingResource(receiver);
            }

            GalleryEntity ge = loadGalleryEntity(geFound, r, false);

            bundle.putParcelable(KEY_RESULT_GALLERY_ENTITY, ge);
            receiver.send(THUMBNAIL_LOADED, bundle);

        } catch (Exception e) {
            handleException(e, receiver);
        }
    }

    private void sendThumbnailsInfo(ResultReceiver receiver, List<Resource> list, boolean bPublic)
            throws CancelledDownloadException, DownloadNoSpaceAvailableException, HttpCodeException, IOException {
        List<GalleryEntity> entities = persistenceManager.getEntities();
        entities.clear();

        for (Resource r : list) {
            Bundle bundleResource = new Bundle();
            GalleryEntity ge;
            ge = loadGalleryEntity(null, r, bPublic);

            bundleResource.putParcelable(KEY_RESULT_GALLERY_ENTITY, ge);
            receiver.send(THUMBNAIL_LOADED, bundleResource);
            entities.add(ge);
        }
    }

    private GalleryEntity loadGalleryEntity(GalleryEntity ge, Resource r, boolean bPublic)
            throws CancelledDownloadException, DownloadNoSpaceAvailableException, HttpCodeException, IOException {

        RestClient restClient = RestClientUtil.getInstance(credentials);
        File result = new File(filesDirectory,
                new File("jaacad_" + r.getMd5()
                        + "_thumbnail_" + r.getName()).getName());
        if (!result.exists())
            restClient.downloadUrl(r.getPreview(), result, this);

        if (ge == null)
            ge = new GalleryEntity();
        if (bPublic)
            ge.setPublicKey(r.getPublicKey());

        ge.setState(GalleryEntity.State.THUMBNAIL);
        ge.setLoadedDateTime(new Date());
        ge.setResourceId(r.getMd5());
        ge.setImageUrl(r.getPath().getPath());
        ge.setThumbnailUrl(r.getPreview());
        ge.setFileName(r.getName());
        ge.setPathToThumbnail(result.getAbsolutePath().toString());
        persistenceManager.addFileToCache(ge.getPathToThumbnail(), true);
        return ge;
    }


    private void handleException(Exception e, ResultReceiver receiver) {
        if (e instanceof HttpCodeException) {
            HttpCodeException httpCodeException = (HttpCodeException) e;
            switch (httpCodeException.getCode()) {
                case 401:
                case 400: {
                    sendMissingResource(receiver);
                    return;
                }
            }
            Bundle bundleError = new Bundle();
            bundleError.putString(KEY_RESULT_NETWORK_EXCEPTION, e.getLocalizedMessage());
            e.printStackTrace();
            receiver.send(NETWORK_EXCEPTION, bundleError);
        }
    }

        private void requreLoginInfo (ResultReceiver receiver,final String token){

            RestClient restClient = RestClientUtil.getInstance(AuthService.defaultCredentials());

            try {
                RestAdapter.Builder restAdapter = new RestAdapter.Builder()
                        .setEndpoint("https://login.yandex.ru/info");

                LoginApi api = restAdapter.build().create(LoginApi.class);

                LoginResponse response = api.getLoginInfo("OAuth " + token,
                        AuthService.USER_AGENT);
                // https://avatars.yandex.net/get-yapic/<идентификатор портрета>/<размер>
                String url;
                if (response.isAvatarEmpty())
                    url = "https://avatars.mds.yandex.net/get-yapic/0/0-0/islands-200";
                else
                    url = String.format("https://avatars.yandex.net/get-yapic/%s/%s/",
                            response.getDefaultAvatarId(), "islands-200");
                File avatarFileName = new File(filesDirectory,
                        new File("jaacad_avatar_" + response.getDisplayName() + ".png").getName());
                if (avatarFileName.exists())
                    avatarFileName.delete();
                restClient.downloadUrl(url, avatarFileName, this);

                Bundle bundle = new Bundle();
                String sPrettyName = response.getRealName();
                if (sPrettyName == null)
                    sPrettyName = response.getLastName();
                if (sPrettyName == null)
                    sPrettyName = response.getFirstName();
                if (sPrettyName == null)
                    sPrettyName = response.getDisplayName();

                bundle.putString(KEY_RESULT_LOGIN_PRETTY_NAME, sPrettyName);
                bundle.putString(KEY_RESULT_LOGIN_DISPLAY_NAME, response.getDisplayName());
                bundle.putString(KEY_RESULT_LOGIN_AVATAR_FILENAME, avatarFileName.getAbsolutePath());
                bundle.putString(KEY_INTENT_AUTH_TOKEN, token);
                receiver.send(LOGIN_INFO, bundle);
            } catch (Exception e) {
                handleException(e, receiver);
            }


        }

        private void checkInternetConnection (ResultReceiver receiver){

            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
                } catch (Exception e) {
                    Log.d("my_tag", "Ошибка проверки подключения к интернету", e);
                    handleException(e, receiver);
                    checkInternetPending = false;
                    return;
                }
            }
            checkInternetPending = false;
            receiver.send(INTERNET_LOST, null);
        }

        @Override
        public void updateProgress ( long loaded, long total){
            if (total != 0) {
                Bundle bundle = new Bundle();
                bundle.putLong(KEY_RESULT_IMAGE_DOWNLOAD_PROGRESS, (100 * loaded) / total);
                resultReceiver.send(IMAGE_LOADING_PROGRESS, bundle);
            }
        }

        @Override
        public boolean hasCancelled () {
            return false;
        }


    }
