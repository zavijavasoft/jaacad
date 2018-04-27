package com.zavijavasoft.jaacad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class ImageViewer extends View {

    private Bitmap image = null;
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private final Paint paint = new Paint();

    private float scaleFactorOnLoad;
    private float scaleFactor;
    private float verticalRatio;
    private float horizontalRatio;
    private boolean isPortraitOrientation;



    private int getScaledWidth() {
        return (int) (image.getWidth() * scaleFactor);
    }

    private int getScaledHeight() {
        return (int) (image.getHeight() * scaleFactor);
    }

    public ImageViewer(Context context) {
        super(context);
        gestureDetector = new GestureDetector(context, new ImageGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new ImageScaleGestureListener());
        paint.setFilterBitmap(true);
        paint.setDither(false);
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect dst = new Rect(0, 0, getScaledWidth(), getScaledHeight());
        canvas.drawBitmap(image, null, dst, paint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (gestureDetector.onTouchEvent(event)) {
            invalidate();
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                ((View)getParent()).performClick();
                invalidate();
                break;
            default:
                break;
        }
        if (scaleGestureDetector.onTouchEvent(event)) {
            invalidate();
            return true;
        }

        return false;
    }

    public void loadImage(@NonNull String fileName) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        image = BitmapFactory.decodeFile(fileName, options);
        if (image == null) throw new NullPointerException("The image can't be decoded.");

        isPortraitOrientation = getHeight() > getWidth();
        verticalRatio = (float) getHeight() / (float) image.getHeight();
        horizontalRatio = (float) getWidth() / (float) image.getWidth();
        scaleFactor = scaleFactorOnLoad = Math.min(verticalRatio, horizontalRatio);
        centerImage();


    }

    private void centerImage() {
        // center image on the screen
        int width = getWidth();
        int height = getHeight();

        if ((width != 0) || (height != 0)) {

            int scrollX = 0;
            int scrollY = 0;

            if (isPortraitOrientation) {
                if (scaleFactorOnLoad == verticalRatio) {
                    scrollX = -(width - getScaledWidth()) / 2;
                } else {
                    scrollY = -(height - getScaledHeight()) / 2;
                }
            } else {
                if (scaleFactorOnLoad == horizontalRatio) {
                    scrollY = -(height - getScaledHeight()) / 2;
                } else {
                    scrollX = -(width - getScaledWidth()) / 2;
                }
            }
            scrollTo(scrollX, scrollY);
        }
    }


    private class ImageGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            scaleFactor = scaleFactorOnLoad;
            centerImage();
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }
    }

    private class ImageScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            scaleFactor = Math.max(scaleFactorOnLoad,
                    Math.min(scaleFactor, scaleFactorOnLoad * 3));

            if (scaleFactor > scaleFactorOnLoad) {
                int newScrollX = (int) ((getScrollX() + detector.getFocusX()) * detector.getScaleFactor() - detector.getFocusX());
                int newScrollY = (int) ((getScrollY() + detector.getFocusY()) * detector.getScaleFactor() - detector.getFocusY());
                scrollTo(newScrollX, newScrollY);
            } else {
                centerImage();
            }
            invalidate();
            return true;


        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }
}
