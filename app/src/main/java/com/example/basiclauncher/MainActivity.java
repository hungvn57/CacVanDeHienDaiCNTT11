package com.example.basiclauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_CODE_SPEECH_INPUT = 1234;


    DrawerAdapter drawerAdapterObject;
    GridView drawerGrid;
    @SuppressWarnings("deprecation")
    SlidingDrawer slidingDrawer;
    RelativeLayout homeView;
    RelativeLayout searchBar;
    RelativeLayout removeBar;
    ImageButton searchVoice;
    EditText edtSearch;
    Pac[] pacs;
    PackageManager pm;
    AppWidgetManager mAppWidgetManager;
    LauncherAppWidgetHost mAppWidgetHost;
    int REQUEST_CREATE_APPWIDGET = 900;
    int REQUEST_CREATE_SHORTCUT = 700;

    int numWidgets;
    SharedPreferences globalPrefs;
    static Activity activity;

    static boolean appLaunchable = true;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new LauncherAppWidgetHost(this, R.id.APPWIDGET_HOST_ID);
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        drawerGrid = (GridView) findViewById(R.id.content);
        slidingDrawer = (SlidingDrawer) findViewById(R.id.drawer);
        homeView = (RelativeLayout) findViewById(R.id.home_view);
        searchBar = (RelativeLayout) findViewById(R.id.search_bar);
        removeBar = (RelativeLayout) findViewById(R.id.remove_bar);
        edtSearch = (EditText) findViewById(R.id.edt_search_bar);

        edtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchString = edtSearch.getText().toString();
                    searchWeb(searchString);
                    return true;
                }
                return false;
            }
        });

        pm = getPackageManager();
        new LoadApps().execute();
        addAppsToHome();
        //set_pacs(true);
        slidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {

            @Override
            public void onDrawerOpened() {
                appLaunchable = true;
                searchBar.setVisibility(View.GONE);
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                searchBar.setVisibility(View.VISIBLE);
            }
        });

        homeView.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {

                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);

                String[] items = {getResources().getString(R.string.widget)
                        , getResources().getString(R.string.shortcut)
                        , getResources().getString(R.string.theme)};

                b.setItems(items, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        switch (which) {
                            case 0:
                                selectWidget();
                                break;

                            case 1:
                                selectShortcut();
                                break;

                            case 2:
                                selectTheme();
                                break;
                        }
                    }
                });
                AlertDialog d = b.create();
                d.show();


                return false;
            }
        });

        searchVoice = (ImageButton) findViewById(R.id.imgbt_search_voice);
        searchVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(new PacReceiver(), filter);
    }


    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say something!");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Divice not support search speach",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void searchWeb(String query)
    {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, query);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }


    void selectTheme() {
        Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);

        Intent filter = new Intent(Intent.ACTION_MAIN);
        filter.addCategory("com.anddoes.launcher.THEME");

        intent.putExtra(Intent.EXTRA_INTENT, filter);

        startActivityForResult(intent, R.id.REQUEST_PICK_THEME);
    }

    void selectShortcut() {
        Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
        startActivityForResult(intent, R.id.REQUEST_PICK_SHORTCUT);
    }

    void selectWidget() {
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent);
        startActivityForResult(pickIntent, R.id.REQUEST_PICK_APPWIDGET);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void addEmptyData(Intent pickIntent) {
        ArrayList customInfo = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList customExtras = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    ;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == R.id.REQUEST_PICK_APPWIDGET) {
                configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(data);
            } else if (requestCode == R.id.REQUEST_PICK_SHORTCUT) {
                configureShortcut(data);
            } else if (requestCode == REQUEST_CREATE_SHORTCUT) {
                createShortcut(data);
            } else if (requestCode == R.id.REQUEST_PICK_THEME) {
                globalPrefs.edit().putString("theme", data.getComponent().getPackageName()).commit();
                set_pacs(false);
            } else if (requestCode == REQ_CODE_SPEECH_INPUT) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                searchWeb(result.get(0));
            }

        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    void configureShortcut(Intent data) {
        startActivityForResult(data, REQUEST_CREATE_SHORTCUT);
    }

    public void createShortcut(Intent intent) {
        Intent.ShortcutIconResource iconResource = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
        Bitmap icon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        String shortcutLabel = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Intent shortIntent = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);

        if (icon == null) {
            if (iconResource != null) {
                Resources resources = null;
                try {
                    resources = pm.getResourcesForApplication(iconResource.packageName);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
                if (resources != null) {
                    int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    if (resources.getDrawable(id) instanceof StateListDrawable) {
                        Drawable d = ((StateListDrawable) resources.getDrawable(id)).getCurrent();
                        icon = ((BitmapDrawable) d).getBitmap();
                    } else
                        icon = ((BitmapDrawable) resources.getDrawable(id)).getBitmap();
                }
            }
        }


        if (shortcutLabel != null && shortIntent != null && icon != null) {
            LayoutParams lp = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = 100;
            lp.topMargin = (int) 100;

            LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout ll = (LinearLayout) li.inflate(R.layout.drawer_item, null);

            ((ImageView) ll.findViewById(R.id.icon_image)).setImageBitmap(icon);

            ll.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    v.setOnTouchListener(new AppTouchListener(MainActivity.this, null, homeView, searchBar, removeBar));
                    return false;
                }
            });

            ll.setOnClickListener(new ShortcutClickListener(this));
            ll.setTag(shortIntent);
            homeView.addView(ll, lp);
        }

    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            createWidget(data);
        }
    }

    public void createWidget(Intent data) {
        int topMargin = 110;
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(homeView.getWidth() / 1, homeView.getHeight() / 3);
//        lp.leftMargin = numWidgets * (homeView.getWidth() / 1);
        lp.topMargin = topMargin + numWidgets * (homeView.getHeight() / 3);

        hostView.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);

                String[] items = {"Delete"};

                b.setItems(items, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        switch (which) {
                            case 0:
                                homeView.removeView(hostView);
                                numWidgets--;
                                break;
                        }
                    }
                });
                AlertDialog d = b.create();
                d.show();

                return false;
            }
        });

        homeView.addView(hostView, lp);
        slidingDrawer.bringToFront();
        numWidgets++;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAppWidgetHost.stopListening();
    }

    public class LoadApps extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> pacsList = pm.queryIntentActivities(mainIntent, 0);
            pacs = new Pac[pacsList.size()];
            for (int I = 0; I < pacsList.size(); I++) {
                pacs[I] = new Pac();
                pacs[I].icon = pacsList.get(I).loadIcon(pm);
                pacs[I].packageName = pacsList.get(I).activityInfo.packageName;
                pacs[I].name = pacsList.get(I).activityInfo.name;
                pacs[I].label = pacsList.get(I).loadLabel(pm).toString();
            }
            new SortApps().exchange_sort(pacs);
            themePacs();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (drawerAdapterObject == null) {
                drawerAdapterObject = new DrawerAdapter(activity, pacs);
                drawerGrid.setAdapter(drawerAdapterObject);
                drawerGrid.setOnItemClickListener(new DrawerClickListener(activity, pacs, pm));
                drawerGrid.setOnItemLongClickListener(new DrawerLongClickListener(activity, slidingDrawer, homeView, pacs, searchBar, removeBar));
            } else {
                drawerAdapterObject.pacsForAdapter = pacs;
                drawerAdapterObject.notifyDataSetInvalidated();
            }
        }


    }

    public void addAppsToHome() {

        AppSerializableData data = SerializationTools.loadSerializedData();
        if (data != null) {
            for(int i = 0; i < data.apps.size(); i++){
                Pac p = data.apps.get(i);
                if(p==null) {
                    data.apps.remove(i);
                    i--;
                }
            }
            SerializationTools.serializeData(data);
            data = SerializationTools.loadSerializedData();
            for(int i = 0; i < data.apps.size(); i++){
                Pac p = data.apps.get(i);
                p.setPos(i);
            }
            SerializationTools.serializeData(data);
            data = SerializationTools.loadSerializedData();
            for (Pac pacToAddToHome : data.apps) {
                pacToAddToHome.addToHome(pacToAddToHome, this, homeView, searchBar, removeBar);
            }

        }
    }

    public void set_pacs(boolean init) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pacsList = pm.queryIntentActivities(mainIntent, 0);
        pacs = new Pac[pacsList.size()];
        for (int I = 0; I < pacsList.size(); I++) {
            pacs[I] = new Pac();
            pacs[I].icon = pacsList.get(I).loadIcon(pm);
            pacs[I].packageName = pacsList.get(I).activityInfo.packageName;
            pacs[I].name = pacsList.get(I).activityInfo.name;
            pacs[I].label = pacsList.get(I).loadLabel(pm).toString();
        }
        new SortApps().exchange_sort(pacs);
        themePacs();

        if (init) {
            drawerAdapterObject = new DrawerAdapter(this, pacs);
            drawerGrid.setAdapter(drawerAdapterObject);
            drawerGrid.setOnItemClickListener(new DrawerClickListener(this, pacs, pm));
            drawerGrid.setOnItemLongClickListener(new DrawerLongClickListener(this, slidingDrawer, homeView, pacs, searchBar, removeBar));
        } else {
            drawerAdapterObject.pacsForAdapter = pacs;
            drawerAdapterObject.notifyDataSetInvalidated();
        }
    }

    @SuppressWarnings("deprecation")
    public void themePacs() {
        //theming vars-----------------------------------------------
        final int ICONSIZE = Tools.numtodp(65, MainActivity.this);
        Resources themeRes = null;
        String resPacName = globalPrefs.getString("theme", "");
        String iconResource = null;
        int intres = 0;
        int intresiconback = 0;
        int intresiconfront = 0;
        int intresiconmask = 0;
        float scaleFactor = 1.0f;

        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        p.setAntiAlias(true);

        Paint origP = new Paint(Paint.FILTER_BITMAP_FLAG);
        origP.setAntiAlias(true);

        Paint maskp = new Paint(Paint.FILTER_BITMAP_FLAG);
        maskp.setAntiAlias(true);
        maskp.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));

        if (resPacName.compareTo("") != 0) {
            try {
                themeRes = pm.getResourcesForApplication(resPacName);
            } catch (Exception e) {
            }
            ;
            if (themeRes != null) {
                String[] backAndMaskAndFront = ThemeTools.getIconBackAndMaskResourceName(themeRes, resPacName);
                if (backAndMaskAndFront[0] != null)
                    intresiconback = themeRes.getIdentifier(backAndMaskAndFront[0], "drawable", resPacName);
                if (backAndMaskAndFront[1] != null)
                    intresiconmask = themeRes.getIdentifier(backAndMaskAndFront[1], "drawable", resPacName);
                if (backAndMaskAndFront[2] != null)
                    intresiconfront = themeRes.getIdentifier(backAndMaskAndFront[2], "drawable", resPacName);
            }
        }

        Options uniformOptions = new BitmapFactory.Options();
        uniformOptions.inScaled = false;
        uniformOptions.inDither = false;
        uniformOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Canvas origCanv;
        Canvas canvas;
        scaleFactor = ThemeTools.getScaleFactor(themeRes, resPacName);
        Bitmap back = null;
        Bitmap mask = null;
        Bitmap front = null;
        Bitmap scaledBitmap = null;
        Bitmap scaledOrig = null;
        Bitmap orig = null;

        if (resPacName.compareTo("") != 0 && themeRes != null) {
            try {
                if (intresiconback != 0)
                    back = BitmapFactory.decodeResource(themeRes, intresiconback, uniformOptions);
            } catch (Exception e) {
            }
            try {
                if (intresiconmask != 0)
                    mask = BitmapFactory.decodeResource(themeRes, intresiconmask, uniformOptions);
            } catch (Exception e) {
            }
            try {
                if (intresiconfront != 0)
                    front = BitmapFactory.decodeResource(themeRes, intresiconfront, uniformOptions);
            } catch (Exception e) {
            }
        }
        //theming vars-----------------------------------------------
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Config.RGB_565;
        options.inDither = true;

        for (int I = 0; I < pacs.length; I++) {
            if (themeRes != null) {
                iconResource = null;
                intres = 0;
                iconResource = ThemeTools.getResourceName(themeRes, resPacName, "ComponentInfo{" + pacs[I].packageName + "/" + pacs[I].name + "}");
                if (iconResource != null) {
                    intres = themeRes.getIdentifier(iconResource, "drawable", resPacName);
                }

                if (intres != 0) {//has single drawable for app
                    pacs[I].icon = new BitmapDrawable(BitmapFactory.decodeResource(themeRes, intres, uniformOptions));
                } else {
                    orig = Bitmap.createBitmap(pacs[I].icon.getIntrinsicWidth(), pacs[I].icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    pacs[I].icon.setBounds(0, 0, pacs[I].icon.getIntrinsicWidth(), pacs[I].icon.getIntrinsicHeight());
                    pacs[I].icon.draw(new Canvas(orig));

                    scaledOrig = Bitmap.createBitmap(ICONSIZE, ICONSIZE, Config.ARGB_8888);
                    scaledBitmap = Bitmap.createBitmap(ICONSIZE, ICONSIZE, Config.ARGB_8888);
                    canvas = new Canvas(scaledBitmap);
                    if (back != null) {
                        canvas.drawBitmap(back, Tools.getResizedMatrix(back, ICONSIZE, ICONSIZE), p);
                    }

                    origCanv = new Canvas(scaledOrig);
                    orig = Tools.getResizedBitmap(orig, ((int) (ICONSIZE * scaleFactor)), ((int) (ICONSIZE * scaleFactor)));
                    origCanv.drawBitmap(orig, scaledOrig.getWidth() - (orig.getWidth() / 2) - scaledOrig.getWidth() / 2, scaledOrig.getWidth() - (orig.getWidth() / 2) - scaledOrig.getWidth() / 2, origP);

                    if (mask != null) {
                        origCanv.drawBitmap(mask, Tools.getResizedMatrix(mask, ICONSIZE, ICONSIZE), maskp);
                    }

                    if (back != null) {
                        canvas.drawBitmap(Tools.getResizedBitmap(scaledOrig, ICONSIZE, ICONSIZE), 0, 0, p);
                    } else
                        canvas.drawBitmap(Tools.getResizedBitmap(scaledOrig, ICONSIZE, ICONSIZE), 0, 0, p);

                    if (front != null)
                        canvas.drawBitmap(front, Tools.getResizedMatrix(front, ICONSIZE, ICONSIZE), p);

                    pacs[I].icon = new BitmapDrawable(scaledBitmap);
                }
            }
        }


        front = null;
        back = null;
        mask = null;
        scaledOrig = null;
        orig = null;
        scaledBitmap = null;
        canvas = null;
        origCanv = null;
        p = null;
        maskp = null;
        resPacName = null;
        iconResource = null;
        intres = 0;
    }


    public class PacReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            new LoadApps().execute();
        }

    }

}
