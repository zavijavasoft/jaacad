package com.zavijavasoft.jaacad;


import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zavijavasoft.jaacad.utils.ColorSelector;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ThumbnailViewHolder> {

    private List<GalleryEntity> entities;
    private OperationalDelegate callback;

    public static class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        OperationalDelegate callback;
        CardView cv;
        LinearLayout thumbnailCarcass;
        FrameLayout thumbnailBkGround;
        ProgressBar thumbnailProgress;
        TextView thumbnailFileName;
        ImageView thumbnailImage;
        String url;
        String thumbnailPath;
        String fileName;
        String imageId;


        ThumbnailViewHolder(View itemView, final OperationalDelegate callback) {
            super(itemView);
            this.callback = callback;
            cv = itemView.findViewById(R.id.thumbnail_cardview);
            thumbnailCarcass = itemView.findViewById(R.id.thumbnail_carcass);
            thumbnailBkGround = itemView.findViewById(R.id.thumbnail_bkground);
            thumbnailProgress = itemView.findViewById(R.id.thumbnail_progress);
            thumbnailProgress.setMax(100);
            thumbnailFileName = itemView.findViewById(R.id.thumbnail_file_name);
            thumbnailImage = itemView.findViewById(R.id.thumbnail_image);
            cv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.showImage(thumbnailPath, fileName, imageId);
                }
            });
        }
    }

    GalleryAdapter(OperationalDelegate callback) {
        this.entities = new LinkedList<>();
        this.callback = callback;
    }

    @NonNull
    @Override
    public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.thumbnail, parent, false);
        ThumbnailViewHolder pvh = new ThumbnailViewHolder(v, callback);
        return pvh;
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position) {
        GalleryEntity entity = entities.get(position);
        holder.thumbnailPath = entity.getPathToThumbnail();
        holder.url = entity.getImageUrl();
        holder.fileName = entity.getFileName();
        holder.imageId = entity.getResourceId();
        switch (entity.getState()) {
            case NONE:
            case NOT_LOADED:
            case ONCE_LOADED:
                holder.thumbnailCarcass.setVisibility(View.VISIBLE);
                holder.thumbnailBkGround.setBackgroundColor(entity.getKeyColor());
                holder.thumbnailProgress.setProgress(entity.getProgressValue());
                break;
            case THUMBNAIL:
            case IMAGE:
                holder.thumbnailCarcass.setVisibility(View.INVISIBLE);
                holder.thumbnailFileName.setText(entity.getFileName());
                holder.thumbnailImage.setImageDrawable(Drawable.createFromPath(entity.getPathToThumbnail()));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return entities.size();
    }

    public void remove(String id) {
        for (GalleryEntity e : entities) {
            if (e.getResourceId().equals(id)) {
                entities.remove(e);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void clear(){
        entities.clear();
        notifyDataSetChanged();
    }

     public void update(GalleryEntity entity) {
        int fixed = entities.size();
        int keyColor = ColorSelector.getColor();
        for (int pos = 0; pos < entities.size(); pos++) {
            GalleryEntity e = entities.get(pos);
            if (e.getResourceId().equals(entity.getResourceId())) {
                entities.remove(pos);
                keyColor = e.getKeyColor();
                fixed = pos;
                break;
            }
        }

        entity.setKeyColor(keyColor);
        entities.add(fixed, entity);
        notifyDataSetChanged();
    }

    public List<GalleryEntity> getEntities() {
        return entities;
    }

    public void updateProgress(String id, long loaded, long total) {
        for (int pos = 0; pos < entities.size(); pos++) {
            GalleryEntity e = entities.get(pos);
            if (id.equals(e.getResourceId())) {
                e.setProgressValue((int) ((100 * loaded) / total));
                notifyItemChanged(pos);
            }
        }
    }
}
