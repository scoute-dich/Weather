package de.baumann.weather;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.baumann.weather.helper.OnSwipeTouchListener;

@SuppressWarnings("UnusedParameters")
public class Browser extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    private com.getbase.floatingactionbutton.FloatingActionsMenu fab;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private boolean isNetworkUnAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 21)
            WebView.enableSlowWholeDocumentDraw();

        setContentView(R.layout.activity_main);
        checkFirstRun();

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fab = (com.getbase.floatingactionbutton.FloatingActionsMenu) findViewById(R.id.multiple_actions);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(toolbar != null) {
            final String startType = sharedPref.getString("startType", "1");
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (startType.equals("2")) {
                        Intent intent_in = new Intent(Browser.this, Start.class);
                        startActivity(intent_in);
                        overridePendingTransition(0, 0);
                        finish();
                    } else if (startType.equals("1")) {
                        Intent intent_in = new Intent(Browser.this, Bookmarks.class);
                        startActivity(intent_in);
                        overridePendingTransition(0, 0);
                        finish();
                    }
                }
            });
        }

        swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        assert swipeView != null;
        swipeView.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (isNetworkUnAvailable()) { // loading offline
                    mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
                }
                mWebView.reload();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawer != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if(navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        mWebView = (WebView) findViewById(R.id.webView);
        assert mWebView != null;
        mWebView.loadUrl("http://m.wetterdienst.de/");
        mWebView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); // load online by default

        if (sharedPref.getBoolean ("java", false)){
            mWebView.getSettings().setJavaScriptEnabled(true);
        }

        if (isNetworkUnAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }

        Intent intent = getIntent();
        mWebView.loadUrl(intent.getStringExtra("url"));
        setTitle(intent.getStringExtra("title"));

        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                // do your stuff here
                swipeView.setRefreshing(false);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);

                    String url = mWebView.getUrl();
                    if (url.contains("dwd")) {
                        mWebView.scrollTo(0, 160);
                        setTitle(R.string.dwd);
                        fab.setVisibility(View.GONE);
                    } else if (url.contains("meteoblue")) {
                        mWebView.scrollTo(0, 280);
                        setTitle(R.string.meteo);
                        fab.setVisibility(View.GONE);
                    } else {
                        mWebView.scrollTo(0, 400);
                    }

                } else {
                    progressBar.setVisibility(View.VISIBLE);

                }
            }
        });

        mWebView.setOnTouchListener(new OnSwipeTouchListener(Browser.this) {
            public void onSwipeRight() {
                Intent intent_in = new Intent(Browser.this, Bookmarks.class);
                startActivity(intent_in);
                overridePendingTransition(0, 0);
                finish();
            }

            public void onSwipeLeft() {
                Intent intent_in = new Intent(Browser.this, Bookmarks.class);
                startActivity(intent_in);
                overridePendingTransition(0, 0);
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
        fab.collapse();
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            setTitle(R.string.app_name);
        } else {
            Snackbar snackbar = Snackbar
                    .make(swipeView, R.string.confirm_exit, Snackbar.LENGTH_LONG)
                    .setAction(R.string.yes, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {finishAffinity();
                        }
                    });
            snackbar.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            finishAffinity();
        }

        if (id == R.id.action_settings) {
            Intent intent_in = new Intent(Browser.this, UserSettingsActivity.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        if (id == R.id.action_share) {
            final CharSequence[] options = {getString(R.string.action_share_link), getString(R.string.action_share_screenshot), getString(R.string.action_save_screenshot)};
            new AlertDialog.Builder(Browser.this)
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals(getString(R.string.action_share_link))) {
                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("text/plain");
                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                startActivity(Intent.createChooser(sharingIntent, "Share using"));
                            }
                            if (options[item].equals(getString(R.string.action_share_screenshot))) {
                                if (android.os.Build.VERSION.SDK_INT >= 23) {
                                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                        if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                            new AlertDialog.Builder(Browser.this)
                                                    .setMessage(R.string.permissions)
                                                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            if (android.os.Build.VERSION.SDK_INT >= 23)
                                                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                        REQUEST_CODE_ASK_PERMISSIONS);
                                                        }
                                                    })
                                                    .setNegativeButton(getString(R.string.no), null)
                                                    .show();
                                            return;
                                        }
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_CODE_ASK_PERMISSIONS);
                                        return;
                                    }
                                }

                                File directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/");
                                if (!directory.exists()) {
                                    directory.mkdirs();
                                }

                                Date date = new Date();
                                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_HH-mm", Locale.getDefault());

                                String filename = getString(R.string.toast_screenshot) + " " + Environment.getExternalStorageDirectory() + "/Pictures/Websites/" + dateFormat.format(date) + ".jpg";
                                Snackbar.make(swipeView, filename, Snackbar.LENGTH_LONG).show();

                                mWebView.measure(View.MeasureSpec.makeMeasureSpec(
                                        View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                                mWebView.layout(0, 0, mWebView.getMeasuredWidth(),
                                        mWebView.getMeasuredHeight());
                                mWebView.setDrawingCacheEnabled(true);
                                mWebView.buildDrawingCache();
                                Bitmap bm = Bitmap.createBitmap(mWebView.getMeasuredWidth(),
                                        mWebView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

                                Canvas bigcanvas = new Canvas(bm);
                                Paint paint = new Paint();
                                int iHeight = bm.getHeight();
                                bigcanvas.drawBitmap(bm, 0, iHeight, paint);
                                mWebView.draw(bigcanvas);
                                System.out.println("1111111111111111111111="
                                        + bigcanvas.getWidth());
                                System.out.println("22222222222222222222222="
                                        + bigcanvas.getHeight());

                                try {
                                    OutputStream fOut;
                                    File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/", dateFormat.format(date) + ".jpg");
                                    fOut = new FileOutputStream(file);

                                    bm.compress(Bitmap.CompressFormat.PNG, 50, fOut);
                                    fOut.flush();
                                    fOut.close();
                                    bm.recycle();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("image/png");
                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                Uri bmpUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/"
                                        + dateFormat.format(date) + ".jpg"));
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                startActivity(Intent.createChooser(sharingIntent, "Share using"));

                                File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/"
                                        + dateFormat.format(date) + ".jpg");
                                Uri uri = Uri.fromFile(file);
                                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
                                sendBroadcast(intent);
                            }
                            if (options[item].equals(getString(R.string.action_save_screenshot))) {
                                if (android.os.Build.VERSION.SDK_INT >= 23) {
                                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                        if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                            new AlertDialog.Builder(Browser.this)
                                                    .setMessage(R.string.permissions)
                                                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            if (android.os.Build.VERSION.SDK_INT >= 23)
                                                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                        REQUEST_CODE_ASK_PERMISSIONS);
                                                        }
                                                    })
                                                    .setNegativeButton(getString(R.string.no), null)
                                                    .show();
                                            return;
                                        }
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_CODE_ASK_PERMISSIONS);
                                        return;
                                    }
                                }

                                File directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/");
                                if (!directory.exists()) {
                                    directory.mkdirs();
                                }

                                Date date = new Date();
                                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_HH-mm", Locale.getDefault());

                                String filename = getString(R.string.toast_screenshot) + " " + Environment.getExternalStorageDirectory() + "/Pictures/Websites/" + dateFormat.format(date) + ".jpg";
                                Snackbar.make(swipeView, filename, Snackbar.LENGTH_LONG).show();

                                mWebView.measure(View.MeasureSpec.makeMeasureSpec(
                                        View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                                mWebView.layout(0, 0, mWebView.getMeasuredWidth(),
                                        mWebView.getMeasuredHeight());
                                mWebView.setDrawingCacheEnabled(true);
                                mWebView.buildDrawingCache();
                                Bitmap bm = Bitmap.createBitmap(mWebView.getMeasuredWidth(),
                                        mWebView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

                                Canvas bigcanvas = new Canvas(bm);
                                Paint paint = new Paint();
                                int iHeight = bm.getHeight();
                                bigcanvas.drawBitmap(bm, 0, iHeight, paint);
                                mWebView.draw(bigcanvas);
                                System.out.println("1111111111111111111111="
                                        + bigcanvas.getWidth());
                                System.out.println("22222222222222222222222="
                                        + bigcanvas.getHeight());

                                try {
                                    OutputStream fOut;
                                    File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/", dateFormat.format(date) + ".jpg");
                                    fOut = new FileOutputStream(file);

                                    bm.compress(Bitmap.CompressFormat.PNG, 50, fOut);
                                    fOut.flush();
                                    fOut.close();
                                    bm.recycle();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/"
                                        + dateFormat.format(date) + ".jpg");
                                Uri uri = Uri.fromFile(file);
                                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
                                sendBroadcast(intent);
                            }
                        }
                    }).show();
            checkFirstRun2();
        }

        return super.onOptionsItemSelected(item);
    }

    public void fab1_click(View v) {
        Intent intent = getIntent();
        mWebView.loadUrl(intent.getStringExtra("url"));
        setTitle(intent.getStringExtra("title") + " | " + getString(R.string.fab1_title));
        if (isNetworkUnAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }
        fab.collapse();
    }

    public void fab2_click(View v) {
        Intent intent = getIntent();
        mWebView.loadUrl(intent.getStringExtra("url2"));
        setTitle(intent.getStringExtra("title") + " | " + getString(R.string.fab2_title));
        if (isNetworkUnAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }
        fab.collapse();
    }

    public void fab3_click(View v) {
        Intent intent = getIntent();
        mWebView.loadUrl(intent.getStringExtra("url3"));
        setTitle(intent.getStringExtra("title") + " | " + getString(R.string.fab3_title));
        if (isNetworkUnAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }
        fab.collapse();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_favorite) {
            Intent intent_in = new Intent(Browser.this, Start.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();

        } else if (id == R.id.action_bookmarks) {
            Intent intent_in = new Intent(Browser.this, Bookmarks.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();

        } else if (id == R.id.action_search) {
            Intent intent_in = new Intent(Browser.this, Search.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();

        } else if (id == R.id.action_radar) {
            Intent intent_ra = new Intent(Browser.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/niederschlag_1h/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);
            finish();

        } else if (id == R.id.action_satellit) {
            Intent intent_ra = new Intent(Browser.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/satellit/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);
            finish();

        } else if (id == R.id.action_karten) {
            Intent intent_ra = new Intent(Browser.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/film/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);
            finish();

        } else if (id == R.id.action_thema) {
            Intent intent_th = new Intent(Browser.this, Search.class);
            intent_th.putExtra("url", "http://www.dwd.de/SiteGlobals/Forms/ThemaDesTages/ThemaDesTages_Formular.html?pageNo=0&queryResultId=null");
            startActivityForResult(intent_th, 100);
            overridePendingTransition(0, 0);
            finish();

        } else if (id == R.id.action_lexikon) {
            Intent intent_le = new Intent(Browser.this, Search.class);
            intent_le.putExtra("url", "http://www.dwd.de/DE/service/lexikon/lexikon_node.html");
            startActivityForResult(intent_le, 100);
            overridePendingTransition(0, 0);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void checkFirstRun() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean ("first_browser", false)){
            final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstBrowser_text)));
            Linkify.addLinks(s, Linkify.WEB_URLS);

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Browser.this)
                    .setTitle(R.string.firstBrowser_title)
                    .setMessage(s)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.notagain, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            sharedPref.edit()
                                    .putBoolean("first_browser", false)
                                    .apply();
                        }
                    });
            dialog.show();
        }
    }

    private void checkFirstRun2() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean ("first_screenshot", false)){
            final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstScreenshot_text)));
            Linkify.addLinks(s, Linkify.WEB_URLS);

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Browser.this)
                    .setTitle(R.string.firstScreenshot_title)
                    .setMessage(s)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.notagain, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            sharedPref.edit()
                                    .putBoolean("first_screenshot", false)
                                    .apply();
                        }
                    });
            dialog.show();
        }
    }
}
