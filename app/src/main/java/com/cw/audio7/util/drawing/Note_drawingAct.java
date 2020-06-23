package com.cw.audio7.util.drawing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.page.Page_recycler;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.image.UtilImage;
import com.cw.audio7.util.preferences.Pref;

import java.util.Date;

public class Note_drawingAct extends Activity
{
    private Note_drawingView drawingView; // drawing View

    // create menu ids for each menu option
    private static final int COLOR_MENU_ID = Menu.FIRST;
    private static final int WIDTH_MENU_ID = Menu.FIRST + 1;
    private static final int ERASE_MENU_ID = Menu.FIRST + 2;
    private static final int CLEAR_MENU_ID = Menu.FIRST + 3;
    private static final int SAVE_MENU_ID = Menu.FIRST + 4;
    private static final int SAVE_OTHER_MENU_ID = Menu.FIRST + 5;

    // variable that refers to a Choose Color or Choose Line Width dialog
    private Dialog currentDialog;

    private DB_page dB;
    static String drawingUriInDB;
    long id;
    static boolean drawingHasNew;

    static int mode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        System.out.println("Note_drawingAct / _onCreate");
        Bundle extras = getIntent().getExtras();
        setMode(extras.getInt("drawing_mode"));

        dB = new DB_page(this, TabsHost.getCurrentPageTableId());

        if(getMode() == Util.DRAWING_EDIT) {
            id = extras.getLong("drawing_id");
            drawingUriInDB = dB.getNoteDrawingUri_byId(id);
            getActionBar().setTitle(R.string.edit_drawing);
        } else if(getMode() == Util.DRAWING_ADD) {
            drawingUriInDB = "";
            getActionBar().setTitle(R.string.add_drawing);
        }

        setContentView(R.layout.drawing_main); // inflate the layout
        drawingView = findViewById(R.id.doodleView);

        drawingHasNew = false;
    }

    public static int getMode() {
        return mode;
    }

    public static void setMode(int mode) {
        Note_drawingAct.mode = mode;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        drawingHasNew = false;
    }

    @Override
    public void onBackPressed() {
        if(drawingHasNew)
            confirmUpdateChangeDlg();
        else
            super.onBackPressed();
    }

    // confirmation to update change or not
    void confirmUpdateChangeDlg()
    {
        getIntent().putExtra("NOTE_ADDED","edited");

        AlertDialog.Builder builder = new AlertDialog.Builder(Note_drawingAct.this);
        builder.setTitle(R.string.confirm_dialog_title);

        if(getMode() == Util.DRAWING_ADD)
            builder.setMessage(R.string.add_new_note_confirm_save);
        else if(getMode() == Util.DRAWING_EDIT)
            builder.setMessage(R.string.edit_note_confirm_update);

            builder.setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    if(UtilImage.hasImageExtension(drawingUriInDB, Note_drawingAct.this))
                        updateDrawingInDB();
                    else
                        saveDrawingInDB();

                    setResult(RESULT_OK, getIntent());
                    finish();
                }})
            .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which)
                {   // do nothing
                }})
            .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    setResult(RESULT_CANCELED, getIntent());
                    finish();
                }})
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu); // call super's method

        menu.add(0, COLOR_MENU_ID, 0, R.string.menuitem_color)
          .setIcon(android.R.drawable.ic_menu_edit)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, WIDTH_MENU_ID, 1, R.string.menuitem_line_width)
           .setIcon(android.R.drawable.ic_menu_sort_by_size)
           .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(Menu.NONE, ERASE_MENU_ID, Menu.NONE,
         R.string.menuitem_erase);
        menu.add(Menu.NONE, CLEAR_MENU_ID, Menu.NONE,
         R.string.menuitem_clear);

        menu.add(Menu.NONE, SAVE_MENU_ID, Menu.NONE,
        R.string.menuitem_save_image);

        menu.add(Menu.NONE, SAVE_OTHER_MENU_ID, Menu.NONE,
                R.string.menuitem_save_as_other_image);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // switch based on the MenuItem id
        switch (item.getItemId())
        {
            case COLOR_MENU_ID:
                showColorDialog(); // display color selection dialog
                return true;

            case WIDTH_MENU_ID:
                showLineWidthDialog(); // display line thickness dialog
                return true;

            case ERASE_MENU_ID:
                drawingView.setDrawingColor(Color.WHITE); // line color white
                return true;

            case CLEAR_MENU_ID:
                drawingView.clear(); // clear drawingView
                return true;

            case SAVE_MENU_ID:
                if(UtilImage.hasImageExtension(drawingUriInDB, Note_drawingAct.this))
                    updateDrawingInDB();
                else
                    saveDrawingInDB();
                System.out.println("Note_drawingAct / onOptionsItemSelected / SAVE_MENU_ID / drawingUriInDB = " + drawingUriInDB);
                return true;

            case SAVE_OTHER_MENU_ID:
                saveDrawingInDB();
                return true;
        }
        return super.onOptionsItemSelected(item); // call super's method
    }

    // save drawing in DB
    void saveDrawingInDB()
    {
        String uriStr = drawingView.saveImage(); // save the current images
        dB = new DB_page(this, TabsHost.getCurrentPageTableId());
        String scheme = Uri.parse(uriStr).getScheme();
        // add single file
        if( scheme.equalsIgnoreCase("file") ||
            scheme.equalsIgnoreCase("content") )
        {
            // check if content scheme points to local file
            if(scheme.equalsIgnoreCase("content"))
            {
                String realPath = Util.getLocalRealPathByUri(this, Uri.parse(uriStr));

                if(realPath != null)
                    uriStr = "file://".concat(realPath);
            }

            if( !Util.isEmptyString(uriStr))
            {
                // insert
                // set marking to 1 for default
                id = dB.insertNote("", "", "", uriStr, "", "", 1, (long) 0);// add new note, get return row Id
                drawingUriInDB = uriStr;
            }

            if( getIntent().getExtras().getString("extra_ADD_NEW_TO_TOP", "false").equalsIgnoreCase("true") &&
                    dB.getNotesCount(true) > 0 )
            {
                Page_recycler.swapTopBottom();
            }

            if(!Util.isEmptyString(uriStr))
            {
                String drawingName = Util.getDisplayNameByUriString(uriStr, this);
                Util.showSavedFileToast(drawingName,this);
            }
        }
        drawingHasNew = false;
    }

    // update drawing in DB
    void updateDrawingInDB()
    {
        drawingView.updateImage(); // save the current images
        Date now = new Date();
        dB.updateNote(id,
                dB.getNoteTitle_byId(id),
                dB.getNotePictureUri_byId(id),
                dB.getNoteAudioUri_byId(id),
                drawingUriInDB,
                dB.getNoteLinkUri_byId(id),
                dB.getNoteBody_byId(id),
                dB.getNoteMarking_byId(id),
                now.getTime(),
                true);// add new note, get return row Id
        drawingHasNew = false;
    }

    // display a dialog for selecting color
    private void showColorDialog()
    {
        // create the dialog and inflate its content
        currentDialog = new Dialog(this);
        currentDialog.setContentView(R.layout.drawing_color_dialog);
        currentDialog.setTitle(R.string.title_color_dialog);
        currentDialog.setCancelable(true);
      
        // get the color SeekBars and set their onChange listeners
        final SeekBar alphaSeekBar =
            (SeekBar) currentDialog.findViewById(R.id.alphaSeekBar);
        final SeekBar redSeekBar =
            (SeekBar) currentDialog.findViewById(R.id.redSeekBar);
        final SeekBar greenSeekBar =
            (SeekBar) currentDialog.findViewById(R.id.greenSeekBar);
        final SeekBar blueSeekBar =
            (SeekBar) currentDialog.findViewById(R.id.blueSeekBar);

        // register SeekBar event listeners
        alphaSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
        redSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
        greenSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
        blueSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
     
        // use current drawing color to set SeekBar values
        final int color = drawingView.getDrawingColor();
        alphaSeekBar.setProgress(Color.alpha(color));
        redSeekBar.setProgress(Color.red(color));
        greenSeekBar.setProgress(Color.green(color));
        blueSeekBar.setProgress(Color.blue(color));
      
        // set the Set Color Button's onClickListener
        Button setColorButton = (Button) currentDialog.findViewById(
             R.id.setColorButton);
        setColorButton.setOnClickListener(setColorButtonListener);
 
        currentDialog.show();
    }
   
    // OnSeekBarChangeListener for the SeekBars in the color dialog
    private OnSeekBarChangeListener colorSeekBarChanged =
      new OnSeekBarChangeListener()
    {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser)
        {
            // get the SeekBars and the colorView LinearLayout
            SeekBar alphaSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.alphaSeekBar);
            SeekBar redSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.redSeekBar);
            SeekBar greenSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.greenSeekBar);
            SeekBar blueSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.blueSeekBar);
            View colorView =
                (View) currentDialog.findViewById(R.id.colorView);

            // display the current color
            colorView.setBackgroundColor(Color.argb(
                alphaSeekBar.getProgress(), redSeekBar.getProgress(),
                greenSeekBar.getProgress(), blueSeekBar.getProgress()));
        }
      
        // required method of interface OnSeekBarChangeListener
        @Override
        public void onStartTrackingTouch(SeekBar seekBar)
        {
        }
      
        // required method of interface OnSeekBarChangeListener
        @Override
        public void onStopTrackingTouch(SeekBar seekBar)
        {
        }
    };
   
    // OnClickListener for the color dialog's Set Color Button
    private OnClickListener setColorButtonListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // get the color SeekBars
            SeekBar alphaSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.alphaSeekBar);
            SeekBar redSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.redSeekBar);
            SeekBar greenSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.greenSeekBar);
            SeekBar blueSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.blueSeekBar);

            int a = alphaSeekBar.getProgress();
            int r = redSeekBar.getProgress();
            int g = greenSeekBar.getProgress();
            int b =  blueSeekBar.getProgress();
            // set the line color
            drawingView.setDrawingColor(Color.argb(a,r,g,b));

            Pref.setPref_drawing_line_color_alpha(Note_drawingAct.this,a);
            Pref.setPref_drawing_line_color_red(Note_drawingAct.this,r);
            Pref.setPref_drawing_line_color_green(Note_drawingAct.this,g);
            Pref.setPref_drawing_line_color_blue(Note_drawingAct.this,b);

            currentDialog.dismiss(); // hide the dialog
            currentDialog = null; // dialog no longer needed
        }
    };
   
    // display a dialog for setting the line width
    private void showLineWidthDialog()
    {
        // create the dialog and inflate its content
        currentDialog = new Dialog(this);
        currentDialog.setContentView(R.layout.drawing_width_dialog);
        currentDialog.setTitle(R.string.title_line_width_dialog);
        currentDialog.setCancelable(true);

        // get widthSeekBar and configure it
        SeekBar widthSeekBar =
         (SeekBar) currentDialog.findViewById(R.id.widthSeekBar);
        widthSeekBar.setOnSeekBarChangeListener(widthSeekBarChanged);
        widthSeekBar.setProgress(drawingView.getLineWidth());

        // set the Set Line Width Button's onClickListener
        Button setLineWidthButton =
         (Button) currentDialog.findViewById(R.id.widthDialogDoneButton);
        setLineWidthButton.setOnClickListener(setLineWidthButtonListener);

        currentDialog.show();
    }

    // OnSeekBarChangeListener for the SeekBar in the width dialog
    private OnSeekBarChangeListener widthSeekBarChanged = new OnSeekBarChangeListener() {
        Bitmap bitmap = Bitmap.createBitmap( // create Bitmap
            400, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); // associate with Canvas

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser)
        {
            // get the ImageView
            ImageView widthImageView = (ImageView)
               currentDialog.findViewById(R.id.widthImageView);

            // configure a Paint object for the current SeekBar value
            Paint p = new Paint();
            p.setColor(drawingView.getDrawingColor());
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(progress);

            // erase the bitmap and redraw the line
            bitmap.eraseColor(Color.WHITE);
            canvas.drawLine(30, 50, 370, 50, p);
            widthImageView.setImageBitmap(bitmap);
        }

        // required method of interface OnSeekBarChangeListener
        @Override
        public void onStartTrackingTouch(SeekBar seekBar)
        {
        }

        // required method of interface OnSeekBarChangeListener
        @Override
        public void onStopTrackingTouch(SeekBar seekBar)
        {
        }
    };

    // OnClickListener for the line width dialog's Set Line Width Button
    private OnClickListener setLineWidthButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v)
        {
            // get the color SeekBars
            SeekBar widthSeekBar =
                (SeekBar) currentDialog.findViewById(R.id.widthSeekBar);

            int width = widthSeekBar.getProgress();

            // set the line width
            drawingView.setLineWidth(width);

            //set preference
            Pref.setPref_drawing_line_width(Note_drawingAct.this,width);

            currentDialog.dismiss(); // hide the dialog
            currentDialog = null; // dialog no longer needed
        }
    };
}