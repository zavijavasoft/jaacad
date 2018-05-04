package com.zavijavasoft.jaacad.auth;



import retrofit.http.GET;
import retrofit.http.Header;

public interface LoginApi {
    @GET("/")
    LoginResponse getLoginInfo(@Header(AuthService.AUTHORIZATION_HEADER) String authorization,
                               @Header(AuthService.USER_AGENT_HEADER) String userAgent);

}
