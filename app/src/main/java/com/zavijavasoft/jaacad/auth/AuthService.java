package com.zavijavasoft.jaacad.auth;

import android.content.Context;

import com.yandex.authsdk.YandexAuthOptions;
import com.yandex.authsdk.YandexAuthSdk;

public class AuthService {
    // create your own client id/secret pair with callback url on oauth.yandex.ru
    public static final String CLIENT_ID = "042e1aa627b847369a3d041d25647c8b";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String USER_AGENT = "Jaacad PhotoGallery Application/1.0";

    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id="+CLIENT_ID;

    public static final String USERNAME = "example.username";
    public static final String TOKEN = "example.token";
    public static final String CURRENT_DIR_TAG = "jaacad.current.dir";

    public YandexAuthSdk getYandexAuthSdk() {
        return yandexAuthSdk;
    }

    private YandexAuthSdk yandexAuthSdk;
    private boolean authorized;
    private Credentials credentials;

    public static AuthService service = null;

    public static AuthService getInstance(Context context){
        if (service == null){
            service = new AuthService(context);
        }
        return service;
    }

    public static Credentials defaultCredentials(){
        return new Credentials(USERNAME, TOKEN);
    }

    public AuthService(Context context){
        yandexAuthSdk = new YandexAuthSdk(context, new YandexAuthOptions(context, true));
    }


    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public void setDefaultCredentials(){
        setCredentials(defaultCredentials());
    }

    public void setCredentials(String username, String token){
        this.credentials = new Credentials(username, token);
    }
}
