package com.zavijavasoft.jaacad;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GalleryFragment extends Fragment {

    private RecyclerView recyclerView;
    private GalleryAdapter galleryAdapter;

    public GalleryFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        galleryAdapter = new GalleryAdapter((OperationalDelegate) getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        Context context = getActivity().getApplicationContext();
        recyclerView = new RecyclerView(context);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.photo_neutral_gray));

        int orientation = context.getResources().getConfiguration().orientation;
        int columnCount = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 3 : 2;
        GridLayoutManager glm = new GridLayoutManager(context, columnCount);

        recyclerView.setLayoutManager(glm);
        recyclerView.setAdapter(galleryAdapter);

        return recyclerView;
    }

    public void performUpdate(GalleryEntity ge) {
        galleryAdapter.update(ge);
    }


    public void performClear() {
        galleryAdapter.clear();
    }

    public void performRemove(String id) {
        galleryAdapter.remove(id);
    }

}
