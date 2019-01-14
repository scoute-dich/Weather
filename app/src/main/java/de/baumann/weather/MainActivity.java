package de.baumann.weather;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Objects;

import de.baumann.weather.helper.CustomListAdapter;
import de.baumann.weather.helper.DbAdapter_Bookmarks;
import de.baumann.weather.helper.Settings;
import de.baumann.weather.helper.helpers;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    private Bitmap bitmap;
    private File shareFile;
    private SharedPreferences sharedPref;

    private DbAdapter_Bookmarks db;
    private ListView lv = null;
    private DrawerLayout drawer;

    private String state;
    private ListView lvInfo = null;

    private ImageView imgHeader;
    private int showSearchField;
    private int showForecastMenu;
    private String startTitle;


    private String action_forecast;
    private String action_hourly;
    private String action_overView;

    private boolean isNetworkUnAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = Objects.requireNonNull(connectivityManager).getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView.enableSlowWholeDocumentDraw();
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        progressBar = findViewById(R.id.progressBar);
        showSearchField = 0;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        swipeView = findViewById(R.id.swipe);
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

        ImageButton ib_settings = findViewById(R.id.ib_settings);
        ib_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.closeDrawer(GravityCompat.START);
                Intent intent_in = new Intent(MainActivity.this, Settings.class);
                startActivity(intent_in);
            }
        });

        ImageButton ib_search = findViewById(R.id.ib_search);
        ib_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.closeDrawer(GravityCompat.START);
                showSearchField = 1;
                showForecastMenu = 0;
                startTitle = "wetterdienst.de";
                mWebView.loadUrl("http://m.wetterdienst.de/");
            }
        });

        mWebView = findViewById(R.id.webView);
        assert mWebView != null;
        mWebView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); // load online by default
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setJavaScriptEnabled(true);
        registerForContextMenu(mWebView);

        if (isNetworkUnAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }

        final String startURL = sharedPref.getString("favoriteURL", "http://m.wetterdienst.de/");
        startTitle = sharedPref.getString("favoriteTitle", "wetterdienst.de");
        action_overView = startURL;
        action_hourly = startURL + "stuendlich";
        action_forecast = startURL + "10-Tage";

        mWebView.loadUrl(startURL);
        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                swipeView.setRefreshing(false);

                if (url != null && (
                        url.contains("m.wetterdienst.de/Satellitenbild/") ||
                                url.contains("m.wetterdienst.de/Biowetter/") ||
                                url.contains("m.wetterdienst.de/Warnungen/") ||
                                url.contains("m.wetterdienst.de/Niederschlagsradar/") ||
                                url.contains("m.wetterdienst.de/Wetterbericht/") ||
                                url.contains("m.wetterdienst.de/Thema_des_Tages/") ||
                                url.contains("m.wetterdienst.de/Wetterbericht/") ||
                                url.contains("m.wetterdienst.de/Pollenflug/") ||
                                url.contains("m.wetterdienst.de/Vorhersage/") ||
                                url.contains("dwd") ||
                                url.length() < 27)) {
                    setTitle(mWebView.getTitle());
                    showForecastMenu = 0;
                } else if (url != null && url.equals(startURL)) {
                    showForecastMenu = 1;
                }
                invalidateOptionsMenu();
            }


            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                final Uri uri = Uri.parse(url);
                return handleUri(uri);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                final Uri uri = request.getUrl();
                return handleUri(uri);
            }

            private boolean handleUri(final Uri uri) {
                final String url = uri.toString();
                // Based on some condition you need to determine if you are going to load the url
                // in your web view itself or in a browser.
                // You can use `host` or `scheme` or any part of the `uri` to decide.

                if(url.contains("dwd") || url.contains("wetterdienst")) {
                    mWebView.loadUrl(url);
                    return false;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    mWebView.getContext().startActivity(intent);
                    return true;
                }
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {

                String url = mWebView.getUrl();

                progressBar.setProgress(progress);
                progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);

                if (url != null && url.contains("dwd") && progress > 0) {
                    mWebView.loadUrl("javascript:(function() { " +
                            "var head = document.getElementsByTagName('header')[0];"
                            + "head.parentNode.removeChild(head);" +
                            "})()");
                    setTitle(mWebView.getTitle());

                } else if (url != null && showSearchField == 0 && progress > 0) {
                    mWebView.loadUrl("javascript:(function() { " +
                            "var head = document.getElementsByClassName('logo')[0];" +
                            "head.parentNode.removeChild(head);" +
                            "var head2 = document.getElementsByClassName('navbar navbar-default')[0];" +
                            "head2.parentNode.removeChild(head2);" +
                            "var head3 = document.getElementsByClassName('frc_head')[0];" +
                            "head3.parentNode.removeChild(head3);" +
                            "var head4 = document.getElementsByClassName('hrcolor')[0];" +
                            "head4.parentNode.removeChild(head4);" +
                            "document.getElementsByTagName('hr')[0].style.display=\"none\"; " +
                            "})()");
                    setTitle(startTitle);
                    showSearchField = 0;
                }
            }
        });

        mWebView.setDownloadListener(new DownloadListener() {

            public void onDownloadStart(final String url, String userAgent,
                                        final String contentDisposition, final String mimetype,
                                        long contentLength) {

                registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                final String filename= URLUtil.guessFileName(url, contentDisposition, mimetype);
                Snackbar snackbar = Snackbar
                        .make(mWebView, getString(R.string.toast_download_1) + " " + filename, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                Objects.requireNonNull(dm).enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.toast_download) + " " + filename , Snackbar.LENGTH_LONG).show();
                            }
                        });
                snackbar.show();
            }
        });
        helpers.grantPermissionsStorage(MainActivity.this);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        drawer.addDrawerListener(
                new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                        // Respond when the drawer's position changes
                    }

                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) {
                        // Respond when the drawer is opened
                        setBookmarkList();
                    }

                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {
                        // Respond when the drawer is closed
                        if(imgHeader != null) {
                            TypedArray images = getResources().obtainTypedArray(R.array.splash_images);
                            int choice = (int) (Math.random() * images.length());
                            imgHeader.setImageResource(images.getResourceId(choice, R.drawable.splash1));
                            images.recycle();
                        }
                    }

                    @Override
                    public void onDrawerStateChanged(int newState) {
                        // Respond when the drawer motion state changes
                    }
                }
        );

        toggle.syncState();

        lv = findViewById(R.id.bookmarksList);
        lvInfo = findViewById(R.id.weatherInfoList);
        imgHeader = findViewById(R.id.iv_header);

        db = new DbAdapter_Bookmarks(MainActivity.this);
        db.open();

        setBookmarkList();
    }

    private void setBookmarkList() {
        showSearchField = 0;

        //display data
        final int layoutstyle=R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.textView_title,
        };
        String[] column = new String[] {
                "bookmarks_title"
        };
        final Cursor row = db.fetchAllData();
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(MainActivity.this, layoutstyle, row, column, xml_id, 0) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                Cursor row2 = (Cursor) lv.getItemAtPosition(position);
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_attachment = row2.getString(row2.getColumnIndexOrThrow("bookmarks_attachment"));

                View v = super.getView(position, convertView, parent);
                ImageView iv_icon = v.findViewById(R.id.icon);
                final ImageView iv_attachment = v.findViewById(R.id.fav);

                if (bookmarks_content.contains("wetterdienst.de")) {
                    iv_icon.setImageResource(R.drawable.google_maps);
                } else {
                    iv_icon.setImageResource(R.drawable.white_balance_sunny);
                }

                switch (bookmarks_attachment) {
                    case "":
                        iv_attachment.setVisibility(View.GONE);
                        break;
                    default:
                        iv_attachment.setVisibility(View.VISIBLE);
                        break;
                }

                return v;
            }
        };

        lv.setAdapter(adapter);
        //onClick function
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

                Cursor row = (Cursor) lv.getItemAtPosition(position);
                final String bookmarks_content = row.getString(row.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_title = row.getString(row.getColumnIndexOrThrow("bookmarks_title"));

                showForecastMenu = 1;
                drawer.closeDrawer(GravityCompat.START);
                startTitle = bookmarks_title;
                mWebView.loadUrl(bookmarks_content);

                action_overView = bookmarks_content;
                action_hourly = bookmarks_content + "stuendlich";
                action_forecast = bookmarks_content + "10-Tage";
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor row2 = (Cursor) lv.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String bookmarks_title = row2.getString(row2.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_icon = row2.getString(row2.getColumnIndexOrThrow("bookmarks_icon"));
                final String bookmarks_attachment = row2.getString(row2.getColumnIndexOrThrow("bookmarks_attachment"));
                final String bookmarks_creation = row2.getString(row2.getColumnIndexOrThrow("bookmarks_creation"));

                final CharSequence[] options = {
                        getString(R.string.bookmark_edit_title),
                        getString(R.string.bookmark_edit_url),
                        getString(R.string.bookmark_toddleFav),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(MainActivity.this)
                        .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {

                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    View dialogView = View.inflate(MainActivity.this, R.layout.dialog_edit_url, null);

                                    final EditText edit_title = dialogView.findViewById(R.id.pass_title);
                                    edit_title.setHint(R.string.bookmark_edit_title);
                                    edit_title.setText(bookmarks_title);

                                    builder.setView(dialogView);
                                    builder.setTitle(R.string.bookmark_edit_title);
                                    builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String inputTag = edit_title.getText().toString().trim();
                                            db.update(Integer.parseInt(_id), inputTag, bookmarks_content, bookmarks_icon, bookmarks_attachment, bookmarks_creation);
                                            setBookmarkList();
                                            Snackbar.make(lv, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                                    builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.cancel();
                                        }
                                    });

                                    final AlertDialog dialog2 = builder.create();
                                    // Display the custom alert dialog on interface
                                    dialog2.show();
                                    helpers.showKeyboard(MainActivity.this, edit_title);

                                }

                                if (options[item].equals(getString(R.string.bookmark_edit_url))) {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    View dialogView = View.inflate(MainActivity.this, R.layout.dialog_edit_url, null);

                                    final EditText edit_title = dialogView.findViewById(R.id.pass_title);
                                    edit_title.setHint(R.string.bookmark_edit_url);
                                    edit_title.setText(bookmarks_content);

                                    builder.setView(dialogView);
                                    builder.setTitle(R.string.bookmark_edit_url);
                                    builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String inputTag = edit_title.getText().toString().trim();
                                            db.update(Integer.parseInt(_id), bookmarks_title, inputTag, bookmarks_icon, bookmarks_attachment, bookmarks_creation);
                                            setBookmarkList();
                                            Snackbar.make(lv, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                                    builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.cancel();
                                        }
                                    });

                                    final AlertDialog dialog2 = builder.create();
                                    // Display the custom alert dialog on interface
                                    dialog2.show();
                                    helpers.showKeyboard(MainActivity.this, edit_title);
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {
                                    Snackbar snackbar = Snackbar
                                            .make(lv, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                            .setAction(R.string.toast_yes, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    db.delete(Integer.parseInt(_id));
                                                    setBookmarkList();
                                                }
                                            });
                                    snackbar.show();
                                }

                                if (options[item].equals(getString(R.string.bookmark_toddleFav))) {

                                    if (bookmarks_attachment.equals("")) {
                                        if (db.isExistFav("true")) {
                                            Snackbar.make(lv, R.string.bookmark_setFav_not, Snackbar.LENGTH_LONG).show();
                                        } else {
                                            db.update(Integer.parseInt(_id), bookmarks_title, bookmarks_content, "", "true", bookmarks_creation);
                                            setBookmarkList();
                                            sharedPref.edit()
                                                    .putString("favoriteURL", bookmarks_content)
                                                    .putString("favoriteTitle", bookmarks_title)
                                                    .apply();
                                        }
                                    } else {
                                        db.update(Integer.parseInt(_id), bookmarks_title, bookmarks_content, "", "", bookmarks_creation);
                                        setBookmarkList();
                                    }
                                }
                            }
                        }).show();
                return true;
            }
        });

        String forecast = sharedPref.getString("forecast", "http://www.dwd.de/DE/wetter/vorhersage_aktuell/baden-wuerttemberg/vhs_bawue_node.html");

        switch (Objects.requireNonNull(forecast)) {
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/10-tage/10tage_node.html":
                state = getString(R.string.state_1);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/baden-wuerttemberg/vhs_bawue_node.html":
                state = getString(R.string.state_2);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/bayern/vhs_bay_node.html":
                state = getString(R.string.state_3);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/berlin_brandenburg/vhs_bbb_node.html":
                state = getString(R.string.state_4);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/hessen/vhs_hes_node.html":
                state = getString(R.string.state_5);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/mecklenburg_vorpommern/vhs_mvp_node.html":
                state = getString(R.string.state_6);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/niedersachsen_bremen/vhs_nib_node.html":
                state = getString(R.string.state_7);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/nordrhein_westfalen/vhs_nrw_node.html":
                state = getString(R.string.state_8);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/rheinland-pfalz_saarland/vhs_rps_node.html":
                state = getString(R.string.state_9);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/sachsen/vhs_sac_node.html":
                state = getString(R.string.state_10);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/sachen_anhalt/vhs_saa_node.html":
                state = getString(R.string.state_11);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/schleswig_holstein_hamburg/vhs_shh_node.html":
                state = getString(R.string.state_12);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/thueringen/vhs_thu_node.html":
                state = getString(R.string.state_13);
                break;
        }


        final String[] itemTITLE ={
                getString(R.string.dwd_forecast) + " " + state,
                getString(R.string.dwd_radar),
                getString(R.string.dwd_karten),
                getString(R.string.dwd_satellit),
                getString(R.string.dwd_thema),
                getString(R.string.dwd_lexikon),
        };

        final String[] itemURL ={
                forecast,
                "http://www.dwd.de/DE/leistungen/radarbild_film/radarbild_film.html",
                "http://www.dwd.de/DE/leistungen/hobbymet_wk_europa/hobbyeuropakarten.html?nn=357606",
                "http://www.dwd.de/DE/leistungen/satellit_metsat/satellit_metsat.html",
                "http://www.dwd.de/SiteGlobals/Forms/ThemaDesTages/ThemaDesTages_Formular.html?pageNo=0&queryResultId=null",
                "http://www.dwd.de/DE/service/lexikon/lexikon_node.html",
        };

        Integer[] imgid={
                R.drawable.img_1,
                R.drawable.img_2,
                R.drawable.img_3,
                R.drawable.img_4,
                R.drawable.img_5,
                R.drawable.img_6,
        };

        CustomListAdapter adapter2=new CustomListAdapter(MainActivity.this, itemTITLE, itemURL, imgid);
        lvInfo.setAdapter(adapter2);
        lvInfo.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                drawer.closeDrawer(GravityCompat.START);
                showForecastMenu = 0;
                mWebView.loadUrl(itemURL[+position]);
            }
        });
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            Snackbar snackbar = Snackbar
                    .make(mWebView, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    });
            snackbar.show();
        }
    }

    private final BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {

            Snackbar snackbar = Snackbar
                    .make(mWebView, getString(R.string.toast_download_2), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                        }
                    });
            snackbar.show();
            MainActivity.this.unregisterReceiver(onComplete);
        }
    };

    private final BroadcastReceiver onComplete2 = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {

            Uri myUri= Uri.fromFile(shareFile);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/*");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, myUri);
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            MainActivity.this.startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_image))));
            unregisterReceiver(onComplete2);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem action_forecast = menu.findItem(R.id.action_forecast);
        MenuItem action_hourly = menu.findItem(R.id.action_hourly);
        MenuItem action_overView = menu.findItem(R.id.action_overView);

        if(showForecastMenu == 1) {
            action_forecast.setVisible(true);
            action_hourly.setVisible(true);
            action_overView.setVisible(true);
        } else {
            action_forecast.setVisible(false);
            action_hourly.setVisible(false);
            action_overView.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_saveBookmark) {

            final DbAdapter_Bookmarks db = new DbAdapter_Bookmarks(this);
            db.open();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final View dialogView = View.inflate(this, R.layout.dialog_edit_title, null);

            final EditText edit_title = dialogView.findViewById(R.id.pass_title);
            edit_title.setHint(R.string.bookmark_edit_title);
            if (mWebView.getUrl() != null && mWebView.getUrl().startsWith("http://m.wetterdienst.de/Wetter/")) {
                edit_title.setText(mWebView.getUrl().substring(31).replace("/","").replace("_", " "));
            } else if (mWebView.getUrl() != null && mWebView.getUrl().startsWith("https://m.wetterdienst.de/Wetter/")) {
                edit_title.setText(mWebView.getUrl().substring(32).replace("/","").replace("_", " "));
            } else {
                edit_title.setText(mWebView.getTitle());
            }

            builder.setView(dialogView);
            builder.setTitle(R.string.bookmark_edit_title);
            builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });

            final AlertDialog dialog2 = builder.create();
            // Display the custom alert dialog on interface
            dialog2.show();

            dialog2.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Do stuff, possibly set wantToCloseDialog to true then...
                    String inputTag = edit_title.getText().toString().trim();

                    if(db.isExist(mWebView.getUrl())){
                        Snackbar.make(edit_title, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                    }else{
                        if (mWebView.getUrl().contains("wetterdienst")) {
                            db.insert(inputTag, mWebView.getUrl(), "1", "", "");
                            dialog2.dismiss();
                        } else {
                            db.insert(inputTag, mWebView.getUrl(), "2", "", "");
                            dialog2.dismiss();
                        }

                        Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
            helpers.showKeyboard(this,edit_title);
        }

        if (id == R.id.menu_save_screenshot) {
            screenshot();
        }
        if (id == R.id.action_exit) {
            finish();
        }

        if (id == R.id.action_forecast) {
            mWebView.loadUrl(action_forecast);
        }
        if (id == R.id.action_hourly) {
            mWebView.loadUrl(action_hourly);
        }
        if (id == R.id.action_overView) {
            mWebView.loadUrl(action_overView);
        }


        if (id == R.id.menu_share_screenshot) {
            screenshot();

            if (sharedPref.getBoolean ("first_screenshot", true)){
                Snackbar.make(mWebView, R.string.toast_screenshot_failed, Snackbar.LENGTH_SHORT).show();
            } else if (shareFile.exists()) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("image/png");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                Uri bmpUri = Uri.fromFile(shareFile);
                sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_screenshot))));
            }
        }

        if (id == R.id.menu_share_link) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
        }

        return super.onOptionsItemSelected(item);
    }

    private void screenshot() {

        if (sharedPref.getBoolean ("first_screenshot", true)){

            final AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.firstScreenshot_title)
                    .setMessage(helpers.textSpannable(getString(R.string.firstScreenshot_text)))
                    .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                            sharedPref.edit()
                                    .putBoolean("first_screenshot", false)
                                    .apply();
                        }
                    });
            dialog.show();
        } else {
            shareFile = helpers.newFile();

            try{
                mWebView.measure(View.MeasureSpec.makeMeasureSpec(
                        View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                mWebView.layout(0, 0, mWebView.getMeasuredWidth(), mWebView.getMeasuredHeight());
                mWebView.setDrawingCacheEnabled(true);
                mWebView.buildDrawingCache();

                bitmap = Bitmap.createBitmap(mWebView.getMeasuredWidth(),
                        mWebView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint();
                int iHeight = bitmap.getHeight();
                canvas.drawBitmap(bitmap, 0, iHeight, paint);
                mWebView.draw(canvas);

            }catch (OutOfMemoryError e) {
                e.printStackTrace();
                Snackbar.make(mWebView, R.string.toast_screenshot_failed, Snackbar.LENGTH_SHORT).show();
            }

            if (bitmap != null) {
                try {
                    OutputStream fOut;
                    fOut = new FileOutputStream(shareFile);

                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, fOut);
                    fOut.flush();
                    fOut.close();
                    bitmap.recycle();

                    Snackbar.make(mWebView, getString(R.string.context_saveImage_toast) + " " + helpers.newFileName() , Snackbar.LENGTH_SHORT).show();

                    Uri uri = Uri.fromFile(shareFile);
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
                    sendBroadcast(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(mWebView, R.string.toast_perm, Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }
}
