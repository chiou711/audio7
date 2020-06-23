package com.cw.audio7.util.drawing;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class Note_drawingView extends View
{
    // used to determine whether user moved a finger enough to draw again
    private static final float TOUCH_TOLERANCE = 10;

    private Bitmap bitmap; // drawing area for display or saving
    private Canvas bitmapCanvas; // used to draw on bitmap
    private Paint paintScreen; // use to draw bitmap onto screen
    private Paint paintLine; // used to draw lines onto bitmap
    private HashMap<Integer, Path> pathMap; // current Paths being drawn
    private HashMap<Integer, Point> previousPointMap; // current Points

    String filePath;
    Context context;
    Bitmap jpgBitmap;
    Bitmap overlayBitmap;
    Activity act;

    // constructor
    public Note_drawingView(Context context, AttributeSet attrs)
    {
        super(context, attrs); // pass context to View's constructor
        System.out.println("Note_drawingView / constructor");
        this.context = context;
        act = (Activity) context;

        paintScreen = new Paint(); // used to display bitmap onto screen

        // set the initial display settings for the painted line
        paintLine = new Paint();
        paintLine.setAntiAlias(true); // smooth edges of drawn line

        // set default line color
        paintLine.setColor(Color.argb(Pref.getPref_drawing_line_color_alpha(act),
                                      Pref.getPref_drawing_line_color_red(act),
                                      Pref.getPref_drawing_line_color_green(act),
                                      Pref.getPref_drawing_line_color_blue(act))); // default color is black

        paintLine.setStyle(Paint.Style.STROKE); // solid line

        // set default line width
        paintLine.setStrokeWidth(Pref.getPref_drawing_line_width(act));

        paintLine.setStrokeCap(Paint.Cap.ROUND); // rounded line ends
        pathMap = new HashMap<Integer, Path>();
        previousPointMap = new HashMap<Integer, Point>();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        System.out.println("Note_drawingView / _onFinishInflate");
        if(Note_drawingAct.getMode() == Util.DRAWING_EDIT) {
            filePath = Note_drawingAct.drawingUriInDB;
            jpgBitmap = BitmapFactory.decodeFile(filePath.replace("file:///", ""));
        }

        if(Note_drawingAct.getMode() == Util.DRAWING_EDIT) {
            if (jpgBitmap.getWidth() > jpgBitmap.getHeight()) {
                if(Util.isLandscapeOrientation(act))
                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    Toast.makeText(act,R.string.toast_edit_drawing_to_landscape,Toast.LENGTH_LONG).show();
            }
            else {
                if(Util.isPortraitOrientation(act))
                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    Toast.makeText(act,R.string.toast_edit_drawing_to_portrait,Toast.LENGTH_LONG).show();
            }
        }
        else if(Note_drawingAct.getMode() == Util.DRAWING_ADD) {
            if (Util.isLandscapeOrientation(act))
                act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else
                act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    // Method onSizeChanged creates BitMap and Canvas after app displays
    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE); // erase the BitMap with white

        if(Note_drawingAct.getMode() == Util.DRAWING_EDIT) {
            // draw overlay bitmap on canvas
            overlayBitmap = overlay(bitmap, jpgBitmap);
            bitmapCanvas.drawBitmap(overlayBitmap, 0, 0, paintScreen);
        }
    }

    // clear the painting
    public void clear()
    {
        pathMap.clear(); // remove all paths
        previousPointMap.clear(); // remove all previous points
        bitmap.eraseColor(Color.WHITE); // clear the bitmap
        invalidate(); // refresh the screen
    }
   
    // set the painted line's color
    public void setDrawingColor(int color)
    {
        paintLine.setColor(color);
    }

    // return the painted line's color
    public int getDrawingColor()
    {
      return paintLine.getColor();
    }

    // set the painted line's width
    public void setLineWidth(int width)
    {
        paintLine.setStrokeWidth(width);
    }

    // return the painted line's width
    public int getLineWidth()
    {
      return (int) paintLine.getStrokeWidth();
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
    }

    // called each time this View is drawn
    @Override
    protected void onDraw(Canvas canvas)
    {
//        System.out.println("Note_drawingView / onDraw ");
        canvas.drawBitmap(bitmap, 0, 0, paintScreen);
        // for each filePath currently being drawn
        for (Integer key : pathMap.keySet())
          canvas.drawPath(pathMap.get(key), paintLine); // draw line
    }


    // overlay bitmap
    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(getWidth(), getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, new Matrix(), null);
        return bmOverlay;
    }

    // handle touch event
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // get the event type and the ID of the pointer that caused the event
        int action = event.getActionMasked(); // event type
        int actionIndex = event.getActionIndex(); // pointer (i.e., finger)

        // determine which type of action the given MotionEvent
        // represents, then call the corresponding handling method
        if (action == MotionEvent.ACTION_DOWN ||
         action == MotionEvent.ACTION_POINTER_DOWN)
        {
            touchStarted(event.getX(actionIndex),
                         event.getY(actionIndex),
                         event.getPointerId(actionIndex));
        }
        else if (action == MotionEvent.ACTION_UP ||
         action == MotionEvent.ACTION_POINTER_UP)
        {
            touchEnded(event.getPointerId(actionIndex));
        }
        else
        {
            touchMoved(event);
        }

        invalidate();
        return true;
    }

    // called when the user touches the screen
    private void touchStarted(float x, float y, int lineID)
    {
        Path path; // used to store the path for the given touch id
        Point point; // used to store the last point in path

        // if there is already a path for lineID
        if (pathMap.containsKey(lineID))
        {
            path = pathMap.get(lineID); // get the Path
            path.reset(); // reset the Path because a new touch has started
            point = previousPointMap.get(lineID); // get Path's last point
        }
        else
        {
            path = new Path(); // create a new Path
            pathMap.put(lineID, path); // add the Path to Map
            point = new Point(); // create a new Point
            previousPointMap.put(lineID, point); // add the Point to the Map
        }

        // move to the coordinates of the touch
        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;
    }

    // called when the user drags along the screen
    private void touchMoved(MotionEvent event)
    {
        // for each of the pointers in the given MotionEvent
        for (int i = 0; i < event.getPointerCount(); i++)
        {
            // get the pointer ID and pointer index
            int pointerID = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerID);

            // if there is a path associated with the pointer
            if (pathMap.containsKey(pointerID))
            {
                // get the new coordinates for the pointer
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);

                // get the Path and previous Point associated with
                // this pointer
                Path path = pathMap.get(pointerID);
                Point point = previousPointMap.get(pointerID);

                // calculate how far the user moved from the last update
                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);

                // if the distance is significant enough to matter
                if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE)
                {
                    // move the path to the new location
                    path.quadTo(point.x, point.y, (newX + point.x) / 2,
                      (newY + point.y) / 2);

                    // store the new coordinates
                    point.x = (int) newX;
                    point.y = (int) newY;
                }
            }
        }
    }

    // called when the user finishes a touch
    private void touchEnded(int lineID)
    {
        Path path = pathMap.get(lineID); // get the corresponding Path
        bitmapCanvas.drawPath(path, paintLine); // draw to bitmapCanvas
        path.reset(); // reset the Path

        if(!Note_drawingAct.drawingHasNew)
            Note_drawingAct.drawingHasNew = true;
    }

    // update the current image to the Gallery
    public void updateImage()
    {
        try
        {
            filePath = Note_drawingAct.drawingUriInDB;

            // get an OutputStream to uri
            OutputStream outStream = getContext().getContentResolver().openOutputStream(Uri.parse(filePath));

            // copy the bitmap to the OutputStream
            new Thread(new Runnable() {
                public void run() {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    // flush and close the OutputStream
                    try {
                        outStream.flush(); // empty the buffer
                        outStream.close(); // close the stream
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // display a message indicating that the image was saved
            Toast message = Toast.makeText(getContext(),
            R.string.message_updated, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
            message.getYOffset() / 2);
            message.show(); // display the Toast
        }
        catch (IOException ex)
        {
            // display a message indicating that the image was saved
            Toast message = Toast.makeText(getContext(),
                 R.string.message_error_saving, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                 message.getYOffset() / 2);
            message.show(); // display the Toast
        }
    }


    // save the current image to the Gallery
    public String saveImage()
    {
        // First, create a sub-directory named App name under DCIM if needed
        File imageDir = Util.getPicturesDir(context);
        if(!imageDir.isDirectory())
           imageDir.mkdir();

        String dirString = imageDir.getPath();
        String fileName = "Draw" + Util.getCurrentTimeString();

        // create a ContentValues and configure new image's data
        ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, fileName);
        values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(Images.Media.MIME_TYPE, "image/jpg");

        String uriStr = "file://"+ dirString +"/" +fileName +".jpg";

        // get a Uri for the location to save the file
        Uri uri = getContext().getContentResolver().insert(
              Images.Media.EXTERNAL_CONTENT_URI, values);

        try
        {
            // get an OutputStream to uri
            OutputStream outStream =
                   getContext().getContentResolver().openOutputStream(Uri.parse(uriStr));

            // copy the bitmap to the OutputStream
            new Thread(new Runnable() {
                public void run() {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    // flush and close the OutputStream
                    try {
                        outStream.flush(); // empty the buffer
                        outStream.close(); // close the stream
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // display a message indicating that the image was saved
            Toast message = Toast.makeText(getContext(),
            R.string.message_saved, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
            message.getYOffset() / 2);
            message.show();
        }
        catch (IOException ex)
        {
             // display a message indicating that the image was saved
             Toast message = Toast.makeText(getContext(),
                R.string.message_error_saving, Toast.LENGTH_SHORT);
             message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                message.getYOffset() / 2);
             message.show();
        }

      return uriStr;
    }
}
