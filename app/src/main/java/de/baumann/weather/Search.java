package de.baumann.weather;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
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
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Search extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private boolean isNetworkUnAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    public void fab5_click(View v){
        // write your code here ..

        try {
            final EditText input = new EditText(this);
            input.setText(mWebView.getTitle());
            final BrowserDatabase db = new BrowserDatabase(this);
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                    .setView(input)
                    .setMessage(R.string.edit_title)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            String inputTag = input.getText().toString().trim();
                            db.addBookmark(inputTag, mWebView.getUrl());
                            db.close();
                            Snackbar.make(mWebView, R.string.added, Snackbar.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    });
            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean ret = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFirstRun();

        if (android.os.Build.VERSION.SDK_INT >= 21)
            WebView.enableSlowWholeDocumentDraw();

        setContentView(R.layout.activity_bookmarks_browser);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final String startType = sharedPref.getString("startType", "1");

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startType.equals("2")) {
                    Intent intent_in = new Intent(Search.this, Start.class);
                    startActivity(intent_in);
                    overridePendingTransition(0, 0);
                } else if (startType.equals("1")){
                    Intent intent_in = new Intent(Search.this, Bookmarks.class);
                    startActivity(intent_in);
                    overridePendingTransition(0, 0);
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipeView.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);

        mWebView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.loadUrl("http://m.wetterdienst.de/");
        mWebView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); // load online by default

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

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);

                    String url = mWebView.getUrl();
                    if (url.contains("dwd")) {
                        mWebView.scrollTo(0, 160);
                        setTitle(R.string.dwd);
                    } else if (url.contains("meteoblue")) {
                        mWebView.scrollTo(0, 160);
                        setTitle(R.string.meteo);
                    } else {
                        mWebView.scrollTo(0, 0);
                        setTitle(R.string.action_search);
                    }

                } else {
                    progressBar.setVisibility(View.VISIBLE);

                }
            }
        });

        mWebView.setOnTouchListener(new OnSwipeTouchListener(Search.this) {

            public void onSwipeRight() {
                Intent intent_in = new Intent(Search.this, Bookmarks.class);
                startActivity(intent_in);
                overridePendingTransition(0, 0);
            }

            public void onSwipeLeft() {
                Intent intent_in = new Intent(Search.this, Bookmarks.class);
                startActivity(intent_in);
                overridePendingTransition(0, 0);
            }
        });

    }

    @Override
    public void onBackPressed() {
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

        if (id == R.id.action_settings) {
            Intent intent_in = new Intent(Search.this, UserSettingsActivity.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
        }

        if (id == R.id.action_share) {
            final CharSequence[] options = {getString(R.string.action_share_link), getString(R.string.action_share_screenshot), getString(R.string.action_save_screenshot)};
            new AlertDialog.Builder(Search.this)
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals(getString(R.string.action_share_link))) {
                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("image/png");
                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                startActivity(Intent.createChooser(sharingIntent, "Share using"));
                            }
                            if (options[item].equals(getString(R.string.action_share_screenshot))) {
                                if (android.os.Build.VERSION.SDK_INT >= 23) {
                                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                        if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                            new AlertDialog.Builder(Search.this)
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
                                Snackbar.make(swipeView, R.string.toast_screenshot, Snackbar.LENGTH_LONG).show();
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
                                FileOutputStream fos;
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
                            if (options[item].equals(getString(R.string.action_save_screenshot))) {
                                if (android.os.Build.VERSION.SDK_INT >= 23) {
                                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                        if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                            new AlertDialog.Builder(Search.this)
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
                                Snackbar.make(swipeView, R.string.toast_screenshot, Snackbar.LENGTH_LONG).show();
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
                                FileOutputStream fos;
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
                        }
                    }).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_favorite) {
            Intent intent_in = new Intent(Search.this, Start.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_bookmarks) {
            Intent intent_in = new Intent(Search.this, Bookmarks.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_search) {
            Intent intent_in = new Intent(Search.this, Search.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_radar) {
            Intent intent_ra = new Intent(Search.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/niederschlag_1h/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_satellit) {
            Intent intent_ra = new Intent(Search.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/satellit/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_karten) {
            Intent intent_ra = new Intent(Search.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/film/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_thema) {
            Intent intent_th = new Intent(Search.this, Search.class);
            intent_th.putExtra("url", "http://www.dwd.de/SiteGlobals/Forms/ThemaDesTages/ThemaDesTages_Formular.html?pageNo=0&queryResultId=null");
            startActivityForResult(intent_th, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_lexikon) {
            Intent intent_le = new Intent(Search.this, Search.class);
            intent_le.putExtra("url", "http://www.dwd.de/DE/service/lexikon/lexikon_node.html");
            startActivityForResult(intent_le, 100);
            overridePendingTransition(0, 0);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstSearch", true);
        if (isFirstRun){
            // Place your dialog code here to display the dialog
            final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstSearch_text)));
            Linkify.addLinks(s, Linkify.WEB_URLS);

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Search.this)
                    .setTitle(R.string.firstSearch_title)
                    .setMessage(s)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.notagain, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("firstSearch", false)
                                    .apply();
                        }
                    });
            dialog.show();
        }
    }
}
