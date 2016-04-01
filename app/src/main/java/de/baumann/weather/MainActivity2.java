package de.baumann.weather;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity2 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    private com.getbase.floatingactionbutton.FloatingActionsMenu fab;
    private boolean ret = true;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 21)
            WebView.enableSlowWholeDocumentDraw();

        setContentView(R.layout.activity_main);
        setTitle(R.string.ort2_ueberblick);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fab = (com.getbase.floatingactionbutton.FloatingActionsMenu) findViewById(R.id.multiple_actions);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipeView.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorAccent);

        mWebView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.loadUrl("http://m.wetterdienst.de/Wetter/Karlsruhe_(Baden)/");
        mWebView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); // load online by default

        if (!isNetworkAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }

        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                // do your stuff here
                swipeView.setRefreshing(false);
            }
        });
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isNetworkAvailable()) { // loading offline
                    mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
                }
                mWebView.reload();
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                    mWebView.scrollTo(0, 400);

                } else {
                    progressBar.setVisibility(View.VISIBLE);

                }
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
                        public void onClick(View view) {
                            moveTaskToBack(true);
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
            moveTaskToBack(true);
        }

        if (id == R.id.action_clearCache) {
            Snackbar.make(mWebView, R.string.toast_clearCache, Snackbar.LENGTH_LONG).show();
            mWebView.clearCache(true);
            mWebView.clearFormData();
            mWebView.clearHistory();
        }

        if (id == R.id.action_license) {
            Intent intent_in = new Intent(MainActivity2.this, Info.class);
            startActivity(intent_in);
        }

        if (id == R.id.action_save_screenshot) {

            if (android.os.Build.VERSION.SDK_INT >= 23) {
                int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new android.app.AlertDialog.Builder(this)
                                .setMessage(R.string.permissions)
                                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 23)
                                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                })
                                .setNegativeButton(getString(R.string.no), null)
                                .show();
                        return (true);
                    }
                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_ASK_PERMISSIONS);
                    return (true);
                }
            }

            Snackbar.make(mWebView, R.string.toast_screenshot, Snackbar.LENGTH_LONG).show();

            File directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/Webpages/");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");

            Picture picture = mWebView.capturePicture();
            Bitmap b = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);

            File screen = new File(Environment.getExternalStorageDirectory() + "/Pictures/Webpages/"
                    + dateFormat.format(date) + ".jpg");
            if (screen.exists())
                screen.delete();

            picture.draw(c);

            FileOutputStream fos = null;
            try {

                fos = new FileOutputStream(screen);
                b.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                fos.close();
            } catch (Exception e) {
                e.getMessage();

            }

            File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/Webpages/"
                    + dateFormat.format(date) + ".jpg");
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(intent);
        }

        if (id == R.id.action_screenshot) {

            if (android.os.Build.VERSION.SDK_INT >= 23) {
                int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new android.app.AlertDialog.Builder(this)
                                .setMessage(R.string.permissions)
                                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 23)
                                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                })
                                .setNegativeButton(getString(R.string.no), null)
                                .show();
                        return (true);
                    }
                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_ASK_PERMISSIONS);
                    return (true);
                }
            }

            Snackbar.make(mWebView, R.string.toast_screenshot, Snackbar.LENGTH_LONG).show();

            File directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/Webpages/");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");

            Picture picture = mWebView.capturePicture();
            Bitmap b = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);

            File screen = new File(Environment.getExternalStorageDirectory() + "/Pictures/Webpages/"
                    + dateFormat.format(date) + ".jpg");
            if (screen.exists())
                screen.delete();

            picture.draw(c);

            FileOutputStream fos = null;
            try {

                fos = new FileOutputStream(screen);
                b.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                fos.close();
            } catch (Exception e) {
                e.getMessage();

            }

            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/png");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            Uri bmpUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Pictures/Webpages/"
                    + dateFormat.format(date) + ".jpg"));
            sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
            startActivity(Intent.createChooser(sharingIntent, "Share using"));

            File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/Webpages/"
                    + dateFormat.format(date) + ".jpg");
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void fab1_click(View v) {
        // write your code here ..
        mWebView.loadUrl("http://m.wetterdienst.de/Wetter/Karlsruhe_(Baden)/");
        setTitle(R.string.ort2_ueberblick);
        if (!isNetworkAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }
        fab.collapse();
        ret = true;
    }

    public void fab2_click(View v) {
        // write your code here ..
        mWebView.loadUrl("http://m.wetterdienst.de/Wetter/Karlsruhe_(Baden)/stuendlich/");
        setTitle(R.string.ort2_stuendlich);
        if (!isNetworkAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }
        fab.collapse();
        ret = true;
    }

    public void fab3_click(View v) {
        // write your code here ..
        mWebView.loadUrl("http://m.wetterdienst.de/Wetter/Karlsruhe_(Baden)/10-Tage/");
        setTitle(R.string.ort2_trend);
        if (!isNetworkAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }
        fab.collapse();
        ret = true;
    }

    public void fab4_click(View v) {
        // write your code here ..
        mWebView.loadUrl("http://m.wetterdienst.de/Vorhersage/Baden-Wuerttemberg/");
        setTitle(R.string.ort2_bericht);
        if (!isNetworkAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }
        fab.collapse();
        ret = true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.ort1) {
            Intent intent_in = new Intent(MainActivity2.this, MainActivity.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.ort2) {
            Intent intent_in = new Intent(MainActivity2.this, MainActivity2.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.ort3) {
            Intent intent_in = new Intent(MainActivity2.this, MainActivity3.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.ort4) {
            Intent intent_in = new Intent(MainActivity2.this, MainActivity4.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.ort5) {
            Intent intent_in = new Intent(MainActivity2.this, MainActivity5.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.bookmarks) {
            Intent intent_in = new Intent(MainActivity2.this, Bookmarks.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_radar) {
            Intent intent_ra = new Intent(MainActivity2.this, InfoMeteo.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/niederschlag_1h/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_satellit) {
            Intent intent_ra = new Intent(MainActivity2.this, InfoMeteo.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/satellit/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_karten) {
            Intent intent_ra = new Intent(MainActivity2.this, InfoMeteo.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/film/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_thema) {
            Intent intent_th = new Intent(MainActivity2.this, InfoDWD.class);
            intent_th.putExtra("url", "http://www.dwd.de/SiteGlobals/Forms/ThemaDesTages/ThemaDesTages_Formular.html?pageNo=0&queryResultId=null");
            startActivityForResult(intent_th, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_lexikon) {
            Intent intent_le = new Intent(MainActivity2.this, InfoDWD.class);
            intent_le.putExtra("url", "http://www.dwd.de/DE/service/lexikon/lexikon_node.html");
            startActivityForResult(intent_le, 100);
            overridePendingTransition(0, 0);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
