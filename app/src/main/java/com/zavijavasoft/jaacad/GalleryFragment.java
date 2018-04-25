package com.zavijavasoft.jaacad;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yandex.authsdk.YandexAuthException;
import com.yandex.authsdk.YandexAuthSdk;
import com.yandex.authsdk.YandexAuthToken;
import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import com.zavijavasoft.jaacad.utils.JWTDecoder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class GalleryFragment extends Fragment {

    private CacheManager cache;
    private AuthService authService;
    private RecyclerView recyclerView;
    private GalleryAdapter galleryAdapter;

    public GalleryFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity().getApplicationContext();
        cache = CacheManager.getInstance(context);
        authService = AuthService.getInstance(context);
        cache.loadCachedGallery();
        galleryAdapter = new GalleryAdapter(cache.getEntities(), (ShowImageCallback) getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (authService.isAuthorized()) {
            //AsyncGetLastUploaded async = new AsyncGetLastUploaded();
            AsyncScanDisk async = new AsyncScanDisk();
            async.execute();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cache.loadCachedGallery();
    }

    @Override
    public void onPause() {
        super.onPause();
        cache.saveCachedGallery();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Context context = getActivity().getApplicationContext();

        Set<String> scopes = new HashSet<>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String username = preferences.getString(AuthService.USERNAME, null);
        String token = preferences.getString(AuthService.TOKEN, null);
        if (token == null)
            startActivityForResult(authService.getYandexAuthSdk().createLoginIntent(context, scopes), 1);
        else {
            authService.setCredentials(username, token);
            authService.setAuthorized(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        Context context = getActivity().getApplicationContext();
        recyclerView = new RecyclerView(context);
        recyclerView.setBackgroundColor(Color.rgb(0xd2,0xd2,0xd2));


        int orientation = context.getResources().getConfiguration().orientation;
        int columnCount = orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        GridLayoutManager glm = new GridLayoutManager(context, columnCount );

        recyclerView.setLayoutManager(glm);

        recyclerView.setAdapter(galleryAdapter);
        return recyclerView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        final YandexAuthSdk sdk = authService.getYandexAuthSdk();
        if (requestCode == 1) {
            try {
                final YandexAuthToken yandexAuthToken =
                       sdk.extractToken(resultCode, data);
                if (yandexAuthToken != null) {
                    AsyncJwtGetter getter = new AsyncJwtGetter();
                    getter.execute(yandexAuthToken);
                    // В данном случае используется заведомо неправильный подход -- имитация параллельного
                    // исполнения, дабы обмануть исключение про сеть в основном потоке.
                    // Однако операция извлечения JWT довольно быстрая, и пользователь вряд ли заметит
                    // зависание интерфейса после закрытия диалога OAuth
                    getter.get();
                    authService.setAuthorized(true);
                }
            } catch (YandexAuthException e) {
                Log.d("auth.err", "ayth.ere", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void loadThumbnails(List<Resource> resourceList){
        List<Resource> cut = new LinkedList<>();
        for (int i = 0; i < Math.min(resourceList.size(), 200); i++){
            cut.add(resourceList.get(i));
        }
        // TODO Сопоставляем ID ресурса с имеющимся.... Если не надо скачивать, игнорируем файл
        List<Resource> filtered = cache.filterNeedToBeCached(cut);


        List<GalleryEntity> galleryItems = new LinkedList<>();
        galleryAdapter.notifyDataSetChanged();
        for (Resource res : filtered) {

            GalleryEntity entity = new GalleryEntity();
            entity.setThumbnailUrl(res.getPreview());
            entity.setImageUrl(res.getPath().getPath());
            entity.setState(GalleryEntity.State.ONCE_LOADED);
            entity.setResourceId(res.getMd5());
            entity.setFileName(res.getName());
            entity.setLoadedDateTime(new Date());
            galleryAdapter.update(entity);
            galleryItems.add(entity);
        }

        for(GalleryEntity entity : galleryItems){
            AsyncLoadSingleThumbnail async = new AsyncLoadSingleThumbnail();
            async.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, entity);
        }

    }

    private class AsyncLoadSingleThumbnail extends AsyncTask<GalleryEntity, Void, GalleryEntity>
        implements ProgressListener
    {

        private String currentId = "";

        @Override
        protected GalleryEntity doInBackground(GalleryEntity... entities) {
            Context context = getActivity().getApplicationContext();
            RestClient restClient = RestClientUtil.getInstance(authService.getCredentials());


            GalleryEntity entity = entities[0];
            currentId = entity.getResourceId();

            try {

                File result = new File(context.getFilesDir(),
                        new File("jaacad_" + entity.getResourceId()
                                + "_thumbnail_" + entity.getFileName()).getName());
                if(!result.exists())
                    restClient.downloadUrl(entity.getThumbnailUrl(), result, AsyncLoadSingleThumbnail.this );

                entity.setPathToThumbnail(result.getAbsolutePath());
                entity.setLoadedDateTime(new Date());
                entity.setState(GalleryEntity.State.THUMBNAIL);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServerIOException e) {
                e.printStackTrace();
            } catch (ServerException e) {
                e.printStackTrace();
            }
            return entity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(GalleryEntity entity) {
            super.onPostExecute(entity);
            if (entity != null){
                galleryAdapter.update(entity);
                cache.addFileToCache(entity.getPathToThumbnail(), true);
            }
        }

        @Override
        public void updateProgress(long loaded, long total) {
            final long loaded_ = loaded;
            final long total_ = total;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    galleryAdapter.updateProgress(currentId, loaded_, total_);
                }
            });
        }

        @Override
        public boolean hasCancelled() {
            return false;
        }
    }


    private class AsyncGetLastUploaded extends AsyncTask<Void, Integer, List<Resource>>{

        @Override
        protected List<Resource> doInBackground(Void... voids) {
            RestClient restClient = RestClientUtil.getInstance(authService.getCredentials());
            final int CHUNK_SIZE = 20;


            List<Resource> outList = new LinkedList<>();
            ResourceList list = null;
            try {
                int nOffset = 0;
                for (int i = 0; i < 10; i++){
                    list = restClient.getLastUploadedResources(new ResourcesArgs.Builder()
                            .setMediaType("image")
                            .setLimit(CHUNK_SIZE)
                            .setOffset(nOffset)
                            .setPreviewSize("M")
                            .build());
                    outList.addAll(list.getItems());
                    nOffset += CHUNK_SIZE;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServerIOException e) {
                e.printStackTrace();
            }

            return outList;
        }

        @Override
        protected void onPreExecute() {
            //Тут нужно запустить песочные часы
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(List<Resource> resourceList) {
            super.onPostExecute(resourceList);
            // завершаем песочные часы
            loadThumbnails(resourceList);
        }

    }

    private class AsyncScanDisk extends AsyncTask<Void, Integer, List<Resource>>{

        @Override
        protected List<Resource> doInBackground(Void... voids) {
            RestClient restClient = RestClientUtil.getInstance(authService.getCredentials());
            final int CHUNK_SIZE = 100;


            List<Resource> outList = new LinkedList<>();
            ResourceList list = null;
            try {
                int nOffset = 0;
                do {
                    list = restClient.getFlatResourceList(new ResourcesArgs.Builder()
                            .setMediaType("image")
                            .setLimit(100)
                            .setOffset(nOffset)
                            .setSort("-modified")
                            .setPreviewSize("M")
                            .build());
                    outList.addAll(list.getItems());
                    nOffset += CHUNK_SIZE;
                }while(list.getItems().size() >= 100);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServerIOException e) {
                e.printStackTrace();
            }

            return outList;
        }

        @Override
        protected void onPreExecute() {
            //Тут нужно запустить песочные часы
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(List<Resource> resourceList) {
            super.onPostExecute(resourceList);
            // завершаем песочные часы
            loadThumbnails(resourceList);
        }


    }

    private class AsyncJwtGetter extends AsyncTask<YandexAuthToken, Void, Void>{
        @Override
        protected Void doInBackground(YandexAuthToken... yandexAuthTokens) {
            YandexAuthToken yandexAuthToken = yandexAuthTokens[0];
            String token = yandexAuthToken.getValue();
            try {
                String jwtToken = authService.getYandexAuthSdk().getJwt(yandexAuthToken);

                JWTDecoder decoder = new JWTDecoder(jwtToken);
                String username = decoder.getLogin();

                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit();
                editor.putString(AuthService.USERNAME, username);
                editor.putString(AuthService.TOKEN, token);
                editor.apply();
                authService.setCredentials(username, token);
            } catch (YandexAuthException e) {
                Log.d("auth.err", "auth.err", e);
                e.printStackTrace();
            }
            return null;
        }
    }


}
