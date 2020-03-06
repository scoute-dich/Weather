package de.baumann.weather;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.baumann.weather.helper.CustomListAdapter;
import de.baumann.weather.helper.DbAdapter_Bookmarks;
import de.baumann.weather.helper.Settings;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    private SharedPreferences sharedPref;

    private DbAdapter_Bookmarks db;
    private ListView lv = null;

    private String state;
    private ListView lvInfo = null;

    private ImageView imgHeader;
    private int showSearchField;
    private String startTitle;

    private String action_forecast;
    private String action_hourly;
    private String action_overView;

    private Activity activity;
    private BottomSheetDialog bottomSheetDialog;
    private TextView header_start;

    private void changeHeaderImage() {
        if(imgHeader != null) {
            TypedArray images = getResources().obtainTypedArray(R.array.splash_images);
            int choice = (int) (Math.random() * images.length());
            imgHeader.setImageResource(images.getResourceId(choice, R.drawable.splash2));
            images.recycle();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = MainActivity.this;

        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(activity, R.xml.user_settings, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        progressBar = findViewById(R.id.progressBar);
        showSearchField = 0;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton ib_bookmarks = findViewById(R.id.ib_bookmarks);
        ib_bookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bottomSheetDialog = new BottomSheetDialog(Objects.requireNonNull(activity));
                View dialogView = View.inflate(activity, R.layout.dialog_bookmarks, null);

                lv = dialogView.findViewById(R.id.bookmarksList);
                lvInfo = dialogView.findViewById(R.id.weatherInfoList);
                imgHeader = dialogView.findViewById(R.id.iv_header);

                db = new DbAdapter_Bookmarks(MainActivity.this);
                db.open();
                changeHeaderImage();
                setBookmarkList();

                ImageButton ib_bookmarks = dialogView.findViewById(R.id.ib_bookmarks);
                ImageButton ib_info = dialogView.findViewById(R.id.ib_info);
                ImageButton ib_search = dialogView.findViewById(R.id.ib_search);
                final TextView header_title = dialogView.findViewById(R.id.header_title);
                header_start = dialogView.findViewById(R.id.header_start);
                header_start.setText(sharedPref.getString("favoriteTitle", "wetterdienst.de"));

                ib_bookmarks.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        changeHeaderImage();
                        lv.setVisibility(View.VISIBLE);
                        lvInfo.setVisibility(View.GONE);
                        header_title.setText(R.string.title_bookmarks);
                    }
                });

                ib_info.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        changeHeaderImage();
                        lv.setVisibility(View.GONE);
                        lvInfo.setVisibility(View.VISIBLE);
                        header_title.setText(R.string.title_weatherInfo);
                    }
                });

                ib_search.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                        mWebView.loadUrl("https://www.wetterdienst.de");
                        showSearchField = 1;
                    }
                });

                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                helpers.setBottomSheetBehavior(bottomSheetDialog, dialogView);
            }
        });

        swipeView = findViewById(R.id.swipe);
        mWebView = findViewById(R.id.webView);

        swipeView.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (helpers.isNetworkConnected(activity)) {
                    mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                    mWebView.reload();
                } else {
                    mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_SHORT).show();
                    mWebView.reload();
                    swipeView.setRefreshing(false);
                }
            }
        });

        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        if (helpers.isNetworkConnected(activity)) {
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_SHORT).show();
        }

        final String startURL = sharedPref.getString("favoriteURL", "https://m.wetterdienst.de/");
        startTitle = sharedPref.getString("favoriteTitle", "wetterdienst.de");

        if (startURL.startsWith("https://m.wetterdienst.de/Wetter/") ) {
            Pattern townPattern = Pattern.compile("Wetter/(.*?)/");
            Matcher matcher = townPattern.matcher( startURL );
            if(  matcher.find() ){
                String town = matcher.group().replace("Wetter/","");
                action_overView = "http://m.wetterdienst.de/Wetter/" + town;
                action_hourly = action_overView + "stuendlich";
                action_forecast = action_overView + "10-Tage";
            }
        } else {
            action_overView = startURL;
            action_hourly = startURL + "stuendlich";
            action_forecast = startURL + "10-Tage";
        }

        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                swipeView.setRefreshing(false);

                LinearLayout menu_forecast = findViewById(R.id.menu_forecast);

                if (url.contains("m.wetterdienst.de/Satellitenbild/") ||
                        url.contains("m.wetterdienst.de/Biowetter/") ||
                        url.contains("m.wetterdienst.de/Warnungen/") ||
                        url.contains("m.wetterdienst.de/Niederschlagsradar/") ||
                        url.contains("m.wetterdienst.de/Thema_des_Tages/") ||
                        url.contains("m.wetterdienst.de/Wetterbericht/") ||
                        url.contains("m.wetterdienst.de/Pollenflug/") ||
                        url.contains("m.wetterdienst.de/Vorhersage/") ||
                        url.contains("dwd") ||
                        url.length() < 27) {
                    menu_forecast.setVisibility(View.GONE);
                    setTitle(mWebView.getTitle());
                } else if (showSearchField == 0){
                    menu_forecast.setVisibility(View.VISIBLE);
                    if (startTitle != null) {
                        setTitle(startTitle);
                    } else {
                        setTitle(mWebView.getTitle());
                    }
                }
            }

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

            @Override
            @SuppressWarnings("deprecation")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.contains("google-analytics.com")) {
                    return new WebResourceResponse(
                            null,
                            null,
                            new ByteArrayInputStream("".getBytes())
                    );
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().contains("google-analytics.com")) {
                    return new WebResourceResponse(
                            null,
                            null,
                            new ByteArrayInputStream("".getBytes())
                    );
                }
                return super.shouldInterceptRequest(view, request);
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
                }
            }
        });

        mWebView.loadUrl(startURL);

        ImageButton ib_menu = findViewById(R.id.ib_menu);
        ib_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bottomSheetDialog = new BottomSheetDialog(Objects.requireNonNull(activity));
                View dialogView = View.inflate(activity, R.layout.grid_menu, null);
                GridView grid = dialogView.findViewById(R.id.grid_filter);
                GridItem_Menu itemAlbum_02 = new GridItem_Menu(getResources().getString(R.string.menu_settings), R.drawable.icon_settings);
                GridItem_Menu itemAlbum_03 = new GridItem_Menu(getResources().getString(R.string.menu_bookmark), R.drawable.icon_bookmark);
                GridItem_Menu itemAlbum_05 = new GridItem_Menu(getResources().getString(R.string.menu_share), R.drawable.icon_share);
                GridItem_Menu itemAlbum_06 = new GridItem_Menu(getResources().getString(R.string.menu_exit), R.drawable.icon_exit);

                final List<GridItem_Menu> gridList = new LinkedList<>();

                gridList.add(gridList.size(), itemAlbum_02);
                gridList.add(gridList.size(), itemAlbum_03);
                gridList.add(gridList.size(), itemAlbum_05);
                gridList.add(gridList.size(), itemAlbum_06);

                GridAdapter_Menu gridAdapter = new GridAdapter_Menu(activity, gridList);
                grid.setNumColumns(2);
                grid.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();

                final String url = mWebView.getUrl();
                startTitle = mWebView.getTitle();

                grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        switch (position) {
                            case 0:
                                bottomSheetDialog.cancel();
                                Intent intent = new Intent(activity, Settings.class);
                                activity.startActivity(intent);
                                break;
                            case 1:
                                if (url != null) {
                                    String urlToSave;
                                    String titleToSave;
                                    if (url.startsWith("https://m.wetterdienst.de/Wetter/") ) {
                                        Pattern townPattern = Pattern.compile("Wetter/(.*?)/");
                                        Matcher matcher = townPattern.matcher (mWebView.getUrl());
                                        if (matcher.find()){
                                            titleToSave = matcher.group().replace("Wetter/","");
                                            urlToSave = "http://m.wetterdienst.de/Wetter/" + titleToSave;
                                        } else {
                                            titleToSave = mWebView.getTitle();
                                            urlToSave = url;
                                        }
                                    } else {
                                        titleToSave = mWebView.getTitle();
                                        urlToSave = url;
                                    }
                                    bottomSheetDialog.cancel();
                                    editBookmark(activity, titleToSave.replace("/", "").replace("_", " "), urlToSave, mWebView, 0);
                                }
                                break;
                            case 2:
                                if (url != null) {
                                    bottomSheetDialog.cancel();
                                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                    sharingIntent.setType("text/plain");
                                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                    sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                    startActivity(Intent.createChooser(sharingIntent, (getString(R.string.menu_share))));
                                    break;
                                }
                            case 3:
                                bottomSheetDialog.cancel();
                                mWebView.destroy();
                                Objects.requireNonNull(activity).finish();
                                break;
                        }
                    }
                });

                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                helpers.setBottomSheetBehavior(bottomSheetDialog, dialogView);
            }
        });
        ImageButton ib_forecast = findViewById(R.id.ib_forecast);
        ib_forecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(action_overView);
            }
        });
        ImageButton ib_hour = findViewById(R.id.ib_hour);
        ib_hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(action_hourly);
            }
        });
        ImageButton ib_overview = findViewById(R.id.ib_overview);
        ib_overview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(action_forecast);
            }
        });
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

                View v = super.getView(position, convertView, parent);
                ImageView iv_icon = v.findViewById(R.id.icon);

                if ((bookmarks_content.contains("m.wetterdienst.de/Satellitenbild/") ||
                        bookmarks_content.contains("m.wetterdienst.de/Biowetter/") ||
                        bookmarks_content.contains("m.wetterdienst.de/Warnungen/") ||
                        bookmarks_content.contains("m.wetterdienst.de/Niederschlagsradar/") ||
                        bookmarks_content.contains("m.wetterdienst.de/Thema_des_Tages/") ||
                        bookmarks_content.contains("m.wetterdienst.de/Wetterbericht/") ||
                        bookmarks_content.contains("m.wetterdienst.de/Pollenflug/") ||
                        bookmarks_content.contains("m.wetterdienst.de/Vorhersage/") ||
                        bookmarks_content.contains("dwd") ||
                        bookmarks_content.length() < 27)) {
                    iv_icon.setImageResource(R.drawable.icon_sun);
                } else {
                    iv_icon.setImageResource(R.drawable.icon_location);
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

                bottomSheetDialog.cancel();
                startTitle = bookmarks_title;

                if( bookmarks_content.startsWith ("https://m.wetterdienst.de/Wetter/") ) {

                    Pattern townPattern = Pattern.compile("Wetter/(.*?)/");
                    Matcher matcher = townPattern.matcher( bookmarks_content );
                    if(  matcher.find() ){
                        String town = matcher.group().replace("Wetter/","");

                        action_overView = "http://m.wetterdienst.de/Wetter/" + town;
                        action_hourly = action_overView + "stuendlich";
                        action_forecast = action_overView + "10-Tage";

                    }

                } else {
                    action_overView = bookmarks_content;
                    action_hourly = bookmarks_content + "stuendlich";
                    action_forecast = bookmarks_content + "10-Tage";
                }

                mWebView.loadUrl(bookmarks_content);
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor row2 = (Cursor) lv.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String bookmarks_title = row2.getString(row2.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));

                final BottomSheetDialog bottomSheetDialog_context = new BottomSheetDialog(Objects.requireNonNull(activity));
                View dialogView = View.inflate(activity, R.layout.grid_menu, null);
                GridView grid = dialogView.findViewById(R.id.grid_filter);
                GridItem_Menu itemAlbum_01 = new GridItem_Menu(getResources().getString(R.string.bookmark_edit_title), R.drawable.icon_edit);
                GridItem_Menu itemAlbum_03 = new GridItem_Menu(getResources().getString(R.string.bookmark_toddleFav), R.drawable.icon_fav);
                GridItem_Menu itemAlbum_04 = new GridItem_Menu(getResources().getString(R.string.bookmark_remove_bookmark), R.drawable.icon_delete);

                final List<GridItem_Menu> gridList = new LinkedList<>();
                gridList.add(gridList.size(), itemAlbum_01);
                gridList.add(gridList.size(), itemAlbum_03);
                gridList.add(gridList.size(), itemAlbum_04);

                GridAdapter_Menu gridAdapter = new GridAdapter_Menu(activity, gridList);
                grid.setNumColumns(3);
                grid.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();

                grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        switch (position) {
                            case 0:
                                bottomSheetDialog_context.cancel();
                                editBookmark(activity, bookmarks_title, bookmarks_content, mWebView, Integer.parseInt(_id));
                                break;
                            case 1:
                                bottomSheetDialog_context.cancel();
                                sharedPref.edit().putString("favoriteURL", bookmarks_content).putString("favoriteTitle", bookmarks_title).apply();
                                header_start.setText(sharedPref.getString("favoriteTitle", "wetterdienst.de"));
                                break;
                            case 2:
                                bottomSheetDialog_context.cancel();
                                bottomSheetDialog.cancel();
                                Snackbar snackbar = Snackbar
                                        .make(mWebView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_SHORT)
                                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                db.delete(Integer.parseInt(_id));
                                                setBookmarkList();
                                            }
                                        });
                                snackbar.show();
                                break;
                        }
                    }
                });

                bottomSheetDialog_context.setContentView(dialogView);
                bottomSheetDialog_context.show();
                helpers.setBottomSheetBehavior(bottomSheetDialog_context, dialogView);
                return true;
            }
        });

        String forecast = sharedPref.getString("forecast", "https://www.dwd.de/DE/wetter/vorhersage_aktuell/baden-wuerttemberg/vhs_bawue_node.html");

        switch (Objects.requireNonNull(forecast)) {
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/10-tage/10tage_node.html":
                state = getString(R.string.state_1);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/baden-wuerttemberg/vhs_bawue_node.html":
                state = getString(R.string.state_2);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/bayern/vhs_bay_node.html":
                state = getString(R.string.state_3);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/berlin_brandenburg/vhs_bbb_node.html":
                state = getString(R.string.state_4);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/hessen/vhs_hes_node.html":
                state = getString(R.string.state_5);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/mecklenburg_vorpommern/vhs_mvp_node.html":
                state = getString(R.string.state_6);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/niedersachsen_bremen/vhs_nib_node.html":
                state = getString(R.string.state_7);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/nordrhein_westfalen/vhs_nrw_node.html":
                state = getString(R.string.state_8);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/rheinland-pfalz_saarland/vhs_rps_node.html":
                state = getString(R.string.state_9);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/sachsen/vhs_sac_node.html":
                state = getString(R.string.state_10);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/sachen_anhalt/vhs_saa_node.html":
                state = getString(R.string.state_11);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/schleswig_holstein_hamburg/vhs_shh_node.html":
                state = getString(R.string.state_12);
                break;
            case "https://www.dwd.de/DE/wetter/vorhersage_aktuell/thueringen/vhs_thu_node.html":
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
                "https://www.dwd.de/DE/leistungen/radarbild_film/radarbild_film.html",
                "https://www.dwd.de/DE/leistungen/hobbymet_wk_europa/hobbyeuropakarten.html?nn=357606",
                "https://www.dwd.de/DE/leistungen/satellit_metsat/satellit_metsat.html",
                "https://www.dwd.de/SiteGlobals/Forms/ThemaDesTages/ThemaDesTages_Formular.html?pageNo=0&queryResultId=null",
                "https://www.dwd.de/DE/service/lexikon/lexikon_node.html",
        };

        Integer[] imgid={
                R.drawable.icon_sun,
                R.drawable.icon_rain,
                R.drawable.icon_wind,
                R.drawable.icon_satellit,
                R.drawable.icon_info,
                R.drawable.icon_help,
        };

        CustomListAdapter adapter2=new CustomListAdapter(MainActivity.this, itemTITLE, itemURL, imgid);
        lvInfo.setAdapter(adapter2);
        lvInfo.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                bottomSheetDialog.cancel();
                mWebView.loadUrl(itemURL[+position]);
            }
        });
    }


    @Override
    public void onBackPressed() {

        if (mWebView.canGoBack()) {
            startTitle = null;
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

    private void editBookmark(final Activity activity, String title, final String url, final View view, final int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View dialogView = View.inflate(activity, R.layout.dialog_edit_bookmark, null);

        final EditText edit_title = dialogView.findViewById(R.id.pass_title);
        edit_title.setText(title.replace("/", "").replace("_", " "));

        final EditText edit_url = dialogView.findViewById(R.id.pass_url);
        edit_url.setText(url);

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

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do stuff, possibly set wantToCloseDialog to true then...
                String inputTitle = edit_title.getText().toString().trim();
                String inputURL = edit_url.getText().toString().trim();

                DbAdapter_Bookmarks db = new DbAdapter_Bookmarks(activity);
                db.open();

                if(db.isExist(inputURL)){
                    if (url.startsWith("https://m.wetterdienst.de/Wetter/") ) {
                        db.update(id, inputTitle, inputURL, "1", "", "");
                        dialog.dismiss();
                    } else {
                        db.update(id, inputTitle, inputURL, "2", "", "");
                        dialog.dismiss();
                    }
                }else{
                    if (url.startsWith("https://m.wetterdienst.de/Wetter/") ) {
                        db.insert(inputTitle, inputURL, "1", "", "");
                        dialog.dismiss();
                    } else {
                        db.insert(inputTitle, inputURL, "2", "", "");
                        dialog.dismiss();
                    }
                }
                setBookmarkList();
                Snackbar.make(view, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
