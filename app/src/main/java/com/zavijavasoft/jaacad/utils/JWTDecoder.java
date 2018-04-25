package com.zavijavasoft.jaacad.utils;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

public class JWTDecoder {
    private String login = "anonymous";

    public JWTDecoder(String jwtToken){
        decodeToken(jwtToken);
    }

    private void decodeToken(String jwtToken){

        String[] split_string = jwtToken.split("\\.");
        // В split_string[0] у нас хранится заголовок JWT
        // В split_string[1] у нас хранится самое ценное: логин
        String decodedBody = new String(Base64.decode(split_string[1], 0));
        // В split_string[2] у нас хранится подпись JWT
        try {
            JSONObject json = new JSONObject(decodedBody);
            login = json.getString("login");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getLogin() {
        return login;
    }
}
