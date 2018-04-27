package com.zavijavasoft.jaacad;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
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
import com.zavijavasoft.jaacad.utils.JWTDecoder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class GalleryFragment extends Fragment {

    private ResultReceiver resultReceiver;


    private CacheManager cache;
    private AuthService authService;
    private RecyclerView recyclerView;
    private GalleryAdapter galleryAdapter;

    public GalleryFragment() {
        super();
        resultReceiver = new GalleryResultReceiver();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity().getApplicationContext();
        cache = CacheManager.getInstance(context);
        authService = AuthService.getInstance(context);
        galleryAdapter = new GalleryAdapter((ShowImageCallback) getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), CoreService.class);
        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_CACHED);
        getActivity().startService(intent);
    }

    @Override
    public void onStop() {
         super.onStop();
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
        Intent intent = new Intent(getActivity(), CoreService.class);
        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.CHECK_INTERNET_CONNECTION);
        getActivity().startService(intent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        Context context = getActivity().getApplicationContext();
        recyclerView = new RecyclerView(context);
        recyclerView.setBackgroundColor(Color.rgb(0xd2, 0xd2, 0xd2));


        int orientation = context.getResources().getConfiguration().orientation;
        int columnCount = orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        GridLayoutManager glm = new GridLayoutManager(context, columnCount);

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    private class AsyncJwtGetter extends AsyncTask<YandexAuthToken, Void, Void> {
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


    private class GalleryResultReceiver extends ResultReceiver {
        public GalleryResultReceiver() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (getActivity() == null)
                return;

            switch (resultCode) {
                case CoreService.NETWORK_EXCEPTION: {
                    String message = resultData.getString(CoreService.KEY_RESULT_NETWORK_EXCEPTION);
                    Intent intent = new Intent(getActivity(), NoInternetActivity.class);
                    intent.putExtra(CoreService.KEY_RESULT_NETWORK_EXCEPTION, message);
                    getActivity().startActivity(intent);
                    break;
                }

                case CoreService.INTERNET_LOST:
               {
                    Intent intent = new Intent(getActivity(), NoInternetActivity.class);
                    getActivity().startActivity(intent);
                    break;
                }
                case CoreService.CACHE_SIZE: {
                    int cacheSize = resultData.getInt(CoreService.KEY_RESULT_CACHE_SIZE);
                    if (cacheSize == 0) {
                        Intent intent = new Intent(getActivity(), CoreService.class);
                        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
                        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_LAST_100_AUTHORIZED);
                        getActivity().startService(intent);
                    }
                    break;
                }
                case CoreService.CACHED_ENTITY_LOADED: {
                    GalleryEntity ge = resultData.getParcelable(CoreService.KEY_RESULT_GALLERY_ENTITY);
                    galleryAdapter.update(ge);
                    if(ge.getState() == GalleryEntity.State.ONCE_LOADED){
                        Intent intent = new Intent(getActivity(), CoreService.class);
                        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
                        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_SINGLE_THUMBNAIL);
                        intent.putExtra(CoreService.KEY_REQUEST_ID, ge.getResourceId());
                        getActivity().startService(intent);
                    }
                    break;
                }
                case CoreService.THUMBNAIL_LOADED:
                    GalleryEntity ge = resultData.getParcelable(CoreService.KEY_RESULT_GALLERY_ENTITY);
                    galleryAdapter.update(ge);
                    break;

            }
            super.onReceiveResult(resultCode, resultData);
        }
    }
}
