package com.zavijavasoft.jaacad;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class GalleryEntityLoader extends AsyncTaskLoader<GalleryEntity> {

    public enum Mode{
        SCAN_DISK,
        LOAD_PREVIEW,
        CLEAR
    }

    private Mode mode;


    GalleryEntityLoader(Context context, Mode mode){
        super(context);
        this.mode = mode;
    }

    GalleryEntity entity;

    @Override
    public GalleryEntity loadInBackground() {
        return entity;
    }


}
