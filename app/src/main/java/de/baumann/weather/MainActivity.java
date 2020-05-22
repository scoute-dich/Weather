package de.baumann.weather;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;

import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private ProgressBar progressBar;
    private SharedPreferences sharedPref;

    private DbAdapter_Bookmarks db;
    private GridView bookmarkList;
    private TextView bookmarkTitle;

    private int showSearchField;
    private String startTitle;

    private String action_forecast;
    private String action_hourly;
    private String action_overView;

    private ImageButton ib_hour;
    private ImageButton ib_overview;
    private ImageButton ib_forecast;

    private LinearLayout menu_forecast;

    private Activity activity;
    private BottomSheetDialog bottomSheetDialog;
    private TextView titleView;
    private BottomAppBar bottomAppBar;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = MainActivity.this;

        setContentView(R.layout.activity_main);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        progressBar = findViewById(R.id.progressBar);
        titleView = findViewById(R.id.titleView);
        showSearchField = 0;

        bottomAppBar = findViewById(R.id.bar);
        bottomAppBar.setNavigationIcon(null);
        setSupportActionBar(bottomAppBar);

        mWebView = findViewById(R.id.webView);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setGeolocationEnabled(false);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);


        menu_forecast = findViewById(R.id.menu_forecast);


        ib_hour = findViewById(R.id.ib_hour);
        ib_forecast = findViewById(R.id.ib_forecast);
        ib_overview = findViewById(R.id.ib_overview);

        ib_hour.setImageResource(R.drawable.icon_hour);
        ib_forecast.setImageResource(R.drawable.icon_sun_accent);
        ib_overview.setImageResource(R.drawable.icon_forecast);
        ib_hour.setTag(R.drawable.icon_hour);
        ib_forecast.setTag(R.drawable.icon_sun_accent);
        ib_overview.setTag(R.drawable.icon_forecast);


        bottomAppBar.setOnTouchListener(new SwipeTouchListener(activity) {

            final NestedScrollView scrollView = findViewById(R.id.scrollView);

            public void onSwipeTop() {
                scrollView.smoothScrollTo(0,0);
            }
            public void onSwipeBottom() {
                scrollView.smoothScrollTo(0,1000000000);
            }
            public void onSwipeRight() {

                if (menu_forecast.getVisibility() == View.VISIBLE) {
                    Integer resource_ib_forecast = (Integer)ib_forecast.getTag();
                    Integer resource_ib_hour = (Integer)ib_hour.getTag();
                    Integer resource_ib_overview = (Integer)ib_overview.getTag();
                    if (resource_ib_forecast == R.drawable.icon_sun_accent) {
                        ib_hour.performClick();
                    }
                    if (resource_ib_hour == R.drawable.icon_hour_accent) {
                        ib_overview.performClick();
                    }
                    if (resource_ib_overview == R.drawable.icon_forecast_accent) {
                        ib_forecast.performClick();
                    }
                }
            }
            public void onSwipeLeft() {
                if (menu_forecast.getVisibility() == View.VISIBLE) {
                    Integer resource_ib_forecast = (Integer)ib_forecast.getTag();
                    Integer resource_ib_hour = (Integer)ib_hour.getTag();
                    Integer resource_ib_overview = (Integer)ib_overview.getTag();
                    if (resource_ib_forecast == R.drawable.icon_sun_accent) {
                        ib_overview.performClick();
                    }
                    if (resource_ib_hour == R.drawable.icon_hour_accent) {
                        ib_forecast.performClick();
                    }
                    if (resource_ib_overview == R.drawable.icon_forecast_accent) {
                        ib_hour.performClick();
                    }
                }
            }
        });

        if (helpers.isNetworkConnected(activity)) {
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Toast.makeText(activity, getString(R.string.toast_cache), Toast.LENGTH_SHORT).show();
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
                if (url.contains("google-analytics.com") || url.contains("fonts.googleapis.com")) {
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
                if (request.getUrl().toString().contains("google-analytics.com") || request.getUrl().toString().contains("fonts.googleapis.com")) {
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
                if (url != null) {
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
                        menu_forecast.setVisibility(View.VISIBLE);
                        if (startTitle != null) {
                            setTitle(startTitle);
                        } else {
                            setTitle(mWebView.getTitle());
                        }
                    }
                }

                progressBar.setProgress(progress);
                progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
            }
        });

        mWebView.loadUrl(startURL);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMenu(activity);
            }
        });

        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openBookmarks(activity);
                return false;
            }
        });

        ImageButton ib_bookmarks = findViewById(R.id.ib_bookmarks);
        ib_bookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBookmarks(activity);
            }
        });

        ImageButton ib_menu = findViewById(R.id.ib_menu);
        ib_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMenu(activity);
            }
        });
        ib_forecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(action_overView);
                ib_hour.setImageResource(R.drawable.icon_hour);
                ib_forecast.setImageResource(R.drawable.icon_sun_accent);
                ib_overview.setImageResource(R.drawable.icon_forecast);
                ib_hour.setTag(R.drawable.icon_hour);
                ib_forecast.setTag(R.drawable.icon_sun_accent);
                ib_overview.setTag(R.drawable.icon_forecast);
            }
        });
        ib_hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(action_hourly);
                ib_hour.setImageResource(R.drawable.icon_hour_accent);
                ib_forecast.setImageResource(R.drawable.icon_sun_light);
                ib_overview.setImageResource(R.drawable.icon_forecast);
                ib_hour.setTag(R.drawable.icon_hour_accent);
                ib_forecast.setTag(R.drawable.icon_sun_light);
                ib_overview.setTag(R.drawable.icon_forecast);
            }
        });
        ib_overview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(action_forecast);
                ib_hour.setImageResource(R.drawable.icon_hour);
                ib_forecast.setImageResource(R.drawable.icon_sun_light);
                ib_overview.setImageResource(R.drawable.icon_forecast_accent);
                ib_hour.setTag(R.drawable.icon_hour);
                ib_forecast.setTag(R.drawable.icon_sun_light);
                ib_overview.setTag(R.drawable.icon_forecast_accent);
            }
        });
    }

    private void openMenu (final Activity activity) {

        bottomSheetDialog = new BottomSheetDialog(Objects.requireNonNull(activity));
        View dialogView = View.inflate(activity, R.layout.grid_layout, null);

        TextView grid_title = dialogView.findViewById(R.id.grid_title);
        grid_title.setText(mWebView.getTitle());
        GridView grid = dialogView.findViewById(R.id.grid_item);
        GridItem_Menu itemAlbum_02 = new GridItem_Menu(getResources().getString(R.string.menu_settings), R.drawable.icon_settings);
        GridItem_Menu itemAlbum_03 = new GridItem_Menu(getResources().getString(R.string.menu_bookmark), R.drawable.icon_bookmark);
        GridItem_Menu itemAlbum_05 = new GridItem_Menu(getResources().getString(R.string.menu_share), R.drawable.icon_share);
        GridItem_Menu itemAlbum_06 = new GridItem_Menu(getResources().getString(R.string.menu_exit), R.drawable.icon_exit);
        GridItem_Menu itemAlbum_07 = new GridItem_Menu(getResources().getString(R.string.menu_reload), R.drawable.icon_reload);
        GridItem_Menu itemAlbum_08 = new GridItem_Menu(getResources().getString(R.string.menu_print), R.drawable.icon_printer);


        final List<GridItem_Menu> gridList = new LinkedList<>();

        gridList.add(gridList.size(), itemAlbum_02);
        gridList.add(gridList.size(), itemAlbum_03);
        gridList.add(gridList.size(), itemAlbum_08);
        gridList.add(gridList.size(), itemAlbum_05);
        gridList.add(gridList.size(), itemAlbum_07);
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

                        final BottomSheetDialog bottomSheetDialog_context = new BottomSheetDialog(Objects.requireNonNull(activity));
                        View dialogView = View.inflate(activity, R.layout.grid_layout, null);

                        final TextView tv = dialogView.findViewById(R.id.grid_title);
                        tv.setText(getString(R.string.menu_settings));

                        GridView grid = dialogView.findViewById(R.id.grid_item);
                        GridItem_Menu itemAlbum_01 = new GridItem_Menu(getResources().getString(R.string.action_license), R.drawable.icon_copyright);
                        GridItem_Menu itemAlbum_03 = new GridItem_Menu(getResources().getString(R.string.action_donate), R.drawable.icon_donate);
                        GridItem_Menu itemAlbum_04 = new GridItem_Menu(getResources().getString(R.string.menu_insertDefaultBookmarks), R.drawable.icon_bookmark);

                        final List<GridItem_Menu> gridList = new LinkedList<>();
                        gridList.add(gridList.size(), itemAlbum_01);
                        gridList.add(gridList.size(), itemAlbum_03);
                        gridList.add(gridList.size(), itemAlbum_04);

                        GridAdapter_Menu gridAdapter = new GridAdapter_Menu(activity, gridList);
                        grid.setNumColumns(1);
                        grid.setAdapter(gridAdapter);
                        gridAdapter.notifyDataSetChanged();

                        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                switch (position) {
                                    case 0:
                                        bottomSheetDialog_context.cancel();
                                        AlertDialog d = new AlertDialog.Builder(activity)
                                                .setTitle(R.string.about_title)
                                                .setMessage(helpers.textSpannable(getString(R.string.about_text)))
                                                .setPositiveButton(getString(R.string.toast_yes),
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                dialog.cancel();
                                                            }
                                                        }).show();
                                        d.show();
                                        ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                                        break;
                                    case 1:
                                        bottomSheetDialog_context.cancel();
                                        Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=NP6TGYDYP9SHY"); // missing 'http://' will cause crashed
                                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                        startActivity(intent);
                                        break;
                                    case 2:
                                        bottomSheetDialog_context.cancel();
                                        insertDefaultBookmarks();
                                        break;
                                }
                            }
                        });

                        bottomSheetDialog_context.setContentView(dialogView);
                        bottomSheetDialog_context.show();
                        helpers.setBottomSheetBehavior(bottomSheetDialog_context, dialogView);
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
                            editBookmark(activity, titleToSave.replace("/", "").replace("_", " "), urlToSave, 0);
                        }
                        break;
                    case 2:
                        if (url != null) {
                            bottomSheetDialog.cancel();
                            try {
                                String title = mWebView.getTitle() + ".pdf";
                                String pdfTitle = mWebView.getTitle();
                                PrintManager printManager = (PrintManager) Objects.requireNonNull(activity).getSystemService(Context.PRINT_SERVICE);
                                PrintDocumentAdapter printAdapter = mWebView.createPrintDocumentAdapter(title);
                                Objects.requireNonNull(printManager).print(pdfTitle, printAdapter, new PrintAttributes.Builder().build());
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(activity, getString(R.string.toast_notPrint), Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                    case 3:
                        if (url != null) {
                            bottomSheetDialog.cancel();
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("text/plain");
                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                            startActivity(Intent.createChooser(sharingIntent, null));
                        }
                        break;
                    case 4:
                        bottomSheetDialog.cancel();
                        if (helpers.isNetworkConnected(activity)) {
                            mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                            mWebView.reload();
                        } else {
                            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                            Toast.makeText(activity, getString(R.string.toast_cache), Toast.LENGTH_SHORT).show();
                            mWebView.reload();
                        }
                        break;
                    case 5:
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

    private void openBookmarks (Activity activity) {
        bottomSheetDialog = new BottomSheetDialog(Objects.requireNonNull(activity));
        View dialogView = View.inflate(activity, R.layout.grid_layout, null);

        ImageButton ib_search = dialogView.findViewById(R.id.ib_search);
        ib_search.setVisibility(View.VISIBLE);
        ib_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.cancel();
                mWebView.loadUrl("https://www.wetterdienst.de");
                showSearchField = 1;
            }
        });

        bookmarkTitle = dialogView.findViewById(R.id.grid_title);
        String text = getString(R.string.bookmark_fav)+ ": " + sharedPref.getString("favoriteTitle", "wetterdienst.de");
        bookmarkTitle.setText(text);
        bookmarkList = dialogView.findViewById(R.id.grid_item);

        db = new DbAdapter_Bookmarks(MainActivity.this);
        db.open();
        setBookmarkList();

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        helpers.setBottomSheetBehavior(bottomSheetDialog, dialogView);
    }

    private void setTitle (String text) {
        titleView.setText(text);
    }

    private void setBookmarkList() {
        showSearchField = 0;

        //display data
        final int layoutstyle = R.layout.item;
        int[] xml_id = new int[] {
                R.id.item_title,
        };
        String[] column = new String[] {
                "bookmarks_title"
        };
        final Cursor row = db.fetchAllData();
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(MainActivity.this, layoutstyle, row, column, xml_id, 0) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                Cursor row2 = (Cursor) bookmarkList.getItemAtPosition(position);
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));

                View v = super.getView(position, convertView, parent);
                ImageView iv_icon = v.findViewById(R.id.item_icon);

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

        bookmarkList.setAdapter(adapter);
        bookmarkList.setNumColumns(1);

        if (bookmarkList.getAdapter().getCount() == 0) {
            insertDefaultBookmarks();
        }

        //onClick function
        bookmarkList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

                Cursor row = (Cursor) bookmarkList.getItemAtPosition(position);
                final String bookmarks_content = row.getString(row.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_title = row.getString(row.getColumnIndexOrThrow("bookmarks_title"));

                ib_hour.setImageResource(R.drawable.icon_hour);
                ib_forecast.setImageResource(R.drawable.icon_sun_accent);
                ib_overview.setImageResource(R.drawable.icon_forecast);
                ib_hour.setTag(R.drawable.icon_hour);
                ib_forecast.setTag(R.drawable.icon_sun_accent);
                ib_overview.setTag(R.drawable.icon_forecast);

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

        bookmarkList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor row = (Cursor) bookmarkList.getItemAtPosition(position);
                final String _id = row.getString(row.getColumnIndexOrThrow("_id"));
                final String bookmarks_title = row.getString(row.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row.getString(row.getColumnIndexOrThrow("bookmarks_content"));

                final BottomSheetDialog bottomSheetDialog_context = new BottomSheetDialog(Objects.requireNonNull(activity));
                View dialogView = View.inflate(activity, R.layout.grid_layout, null);

                final TextView tv = dialogView.findViewById(R.id.grid_title);
                tv.setText(bookmarks_title);

                GridView grid = dialogView.findViewById(R.id.grid_item);
                GridItem_Menu itemAlbum_01 = new GridItem_Menu(getResources().getString(R.string.bookmark_edit_title), R.drawable.icon_edit);
                GridItem_Menu itemAlbum_03 = new GridItem_Menu(getResources().getString(R.string.bookmark_fav), R.drawable.icon_fav);
                GridItem_Menu itemAlbum_04 = new GridItem_Menu(getResources().getString(R.string.bookmark_remove_bookmark), R.drawable.icon_delete);

                final List<GridItem_Menu> gridList = new LinkedList<>();
                gridList.add(gridList.size(), itemAlbum_01);
                gridList.add(gridList.size(), itemAlbum_03);
                gridList.add(gridList.size(), itemAlbum_04);

                GridAdapter_Menu gridAdapter = new GridAdapter_Menu(activity, gridList);
                grid.setNumColumns(1);
                grid.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();

                grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        switch (position) {
                            case 0:
                                bottomSheetDialog_context.cancel();
                                editBookmark(activity, bookmarks_title, bookmarks_content, Integer.parseInt(_id));
                                break;
                            case 1:
                                bottomSheetDialog_context.cancel();
                                sharedPref.edit().putString("favoriteURL", bookmarks_content).putString("favoriteTitle", bookmarks_title).apply();
                                String text = getString(R.string.bookmark_fav)+ ": " + bookmarks_title;
                                bookmarkTitle.setText(text);
                                break;
                            case 2:
                                bottomSheetDialog_context.cancel();
                                bottomSheetDialog.cancel();
                                Snackbar snackbar = Snackbar
                                        .make(bottomAppBar, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_SHORT)
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
    }

    private void insertDefaultBookmarks () {

        try {
            if(!db.isExist("https://m.wetterdienst.de/")){
                db.insert("Wetterdienst.de", "https://m.wetterdienst.de/");
            }
            if(!db.isExist("https://www.dwd.de/DE/Home/home_node.html")){
                db.insert("DWD | Deutscher Wetterdienst", "https://www.dwd.de/DE/Home/home_node.html");
            }
            setBookmarkList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {

        if (mWebView.canGoBack()) {
            startTitle = null;
            mWebView.goBack();
        } else {
            Snackbar snackbar = Snackbar
                    .make(bottomAppBar, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    });
            snackbar.show();
        }
    }

    private void editBookmark(final Activity activity, String title, final String url, final int id) {

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
                        db.update(id, inputTitle, inputURL);
                        dialog.dismiss();
                    } else {
                        db.update(id, inputTitle, inputURL);
                        dialog.dismiss();
                    }
                }else{
                    if (url.startsWith("https://m.wetterdienst.de/Wetter/") ) {
                        db.insert(inputTitle, inputURL);
                        dialog.dismiss();
                    } else {
                        db.insert(inputTitle, inputURL);
                        dialog.dismiss();
                    }
                }
                setBookmarkList();
                Toast.makeText(activity, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
