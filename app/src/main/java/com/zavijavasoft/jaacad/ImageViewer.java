package com.zavijavasoft.jaacad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class ImageViewer extends View {

    private Drawable image_ = null;
    private Bitmap image = null;
    //private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private final Paint paint = new Paint();

    private float scaleFactor;


    private int getScaledWidth()
    {
        return (int)(image.getWidth() * scaleFactor);
    }

    private int getScaledHeight()
    {
        return (int)(image.getHeight() * scaleFactor);
    }

    public ImageViewer(Context context) {
        super(context);
        //gestureDetector = new GestureDetector(context, new ImageGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new ImageScaleGestureListener());
        paint.setFilterBitmap(true);
        paint.setDither(false);
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (image_ != null){
           canvas.save();
           canvas.scale(scaleFactor, scaleFactor);
           image_.draw(canvas);
           canvas.restore();
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scaleGestureDetector.onTouchEvent(event)){

            invalidate();
            return  true;
        }
        return super.onTouchEvent(event);
    }

    public void loadImage(String fileName)
    {
        image_ = Drawable.createFromPath(fileName);
        image_.setBounds(0, 0, image_.getIntrinsicWidth(), image_.getIntrinsicHeight());
   /*     BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        image = BitmapFactory.decodeFile(fileName, options);
        if (image == null) throw new NullPointerException("The image can't be decoded.");
*/
        scaleFactor = getWidth() / image_.getIntrinsicWidth();

        /*// center image on the screen
        int width = getWidth();
        int height = getHeight();
        if ((width != 0) || (height != 0))
        {
            int scrollX = (image.getWidth() < width ? -(width - image.getWidth()) / 2 : image.getWidth() / 2);
            int scrollY = (image.getHeight() < height ? -(height - image.getHeight()) / 2 : image.getHeight() / 2);
            //scrollTo(scrollX, scrollY);
        }
        */
    }


    private class ImageGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            scrollBy((int)distanceX, (int)distanceY);
            return true;
        }
    }

    private class ImageScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener
    {
        public boolean onScale(ScaleGestureDetector detector)
        {
            scaleFactor *= detector.getScaleFactor();

            scaleFactor = Math.max(19.0f, Math.min(scaleFactor, 40.0f));
            invalidate();
            return true;


        }

        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector detector)
        {
 /*           scaleFactor = getWidth() / image.getWidth();
            animate().scaleXBy(scaleFactor).scaleYBy(scaleFactor)
                    .setDuration(300).start();
                    */
        }
    }
}
