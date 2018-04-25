package com.zavijavasoft.jaacad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncCheckInternetConnection extends AsyncTask<Activity, Void, Boolean> {

    private Activity activity;


    @Override
    protected Boolean doInBackground(Activity... activities) {

        activity = activities[0];
        Context context = activity.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(context.CONNECTIVITY_SERVICE);
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
                    return true;
                }
                // иначе проверка провалилась
                return false;

            } catch (IOException e) {
                Log.d("my_tag", "Ошибка проверки подключения к интернету", e);
                return false;
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {

        if (aBoolean) {
            Intent intent = new Intent(activity.getApplicationContext(), GalleryActivity.class);
            intent.putExtra(GalleryActivity.KEY_INTERNET_APPROVED, aBoolean);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        } else if (!(activity instanceof NoInternetActivity)) {
            Intent intent = new Intent(activity.getApplicationContext(), NoInternetActivity.class);
            activity.startActivity(intent);
        }
        super.onPostExecute(aBoolean);
    }
}
