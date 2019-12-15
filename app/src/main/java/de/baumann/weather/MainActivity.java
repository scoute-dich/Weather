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
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
            imgHeader.setImageResource(images.getResourceId(choice, R.drawable.splash1));
            images.recycle();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = MainActivity.this;

        WebView.enableSlowWholeDocumentDraw();
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

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

        mWebView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setJavaScriptEnabled(true);

        if (helpers.isNetworkConnected(activity)) {
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_SHORT).show();
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
                } else if (showSearchField == 0){
                    menu_forecast.setVisibility(View.VISIBLE);
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
                } else {
                    setTitle(mWebView.getTitle());
                }
            }
        });

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
                grid.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();

                final String url = mWebView.getUrl();

                grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        switch (position) {
                            case 0:
                                bottomSheetDialog.cancel();
                                Intent intent = new Intent(activity, Settings.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                activity.startActivity(intent);
                                break;
                            case 1:
                                if (url != null) {
                                    bottomSheetDialog.cancel();
                                    final DbAdapter_Bookmarks db = new DbAdapter_Bookmarks(activity);
                                    db.open();

                                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                    final View dialogView = View.inflate(activity, R.layout.dialog_edit_title, null);

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
                                                Snackbar.make(edit_title, getString(R.string.toast_newTitle), Snackbar.LENGTH_SHORT).show();
                                            }else{
                                                if (mWebView.getUrl().contains("wetterdienst")) {
                                                    db.insert(inputTag, mWebView.getUrl(), "1", "", "");
                                                    dialog2.dismiss();
                                                } else {
                                                    db.insert(inputTag, mWebView.getUrl(), "2", "", "");
                                                    dialog2.dismiss();
                                                }

                                                Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
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

                if (bookmarks_content.contains("wetterdienst.de")) {
                    iv_icon.setImageResource(R.drawable.icon_location);
                } else {
                    iv_icon.setImageResource(R.drawable.icon_sun);
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

                final BottomSheetDialog bottomSheetDialog_context = new BottomSheetDialog(Objects.requireNonNull(activity));
                View dialogView = View.inflate(activity, R.layout.grid_menu, null);
                GridView grid = dialogView.findViewById(R.id.grid_filter);
                GridItem_Menu itemAlbum_01 = new GridItem_Menu(getResources().getString(R.string.bookmark_edit_title), R.drawable.icon_edit);
                GridItem_Menu itemAlbum_02 = new GridItem_Menu(getResources().getString(R.string.bookmark_edit_url), R.drawable.icon_link);
                GridItem_Menu itemAlbum_03 = new GridItem_Menu(getResources().getString(R.string.bookmark_toddleFav), R.drawable.icon_fav);
                GridItem_Menu itemAlbum_04 = new GridItem_Menu(getResources().getString(R.string.bookmark_remove_bookmark), R.drawable.icon_delete);

                final List<GridItem_Menu> gridList = new LinkedList<>();
                gridList.add(gridList.size(), itemAlbum_01);
                gridList.add(gridList.size(), itemAlbum_02);
                gridList.add(gridList.size(), itemAlbum_03);
                gridList.add(gridList.size(), itemAlbum_04);

                GridAdapter_Menu gridAdapter = new GridAdapter_Menu(activity, gridList);
                grid.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();

                grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        AlertDialog.Builder builder;
                        View dialogView;
                        final EditText edit_title;
                        AlertDialog dialog;

                        switch (position) {
                            case 0:
                                bottomSheetDialog_context.cancel();
                                builder = new AlertDialog.Builder(MainActivity.this);
                                dialogView = View.inflate(MainActivity.this, R.layout.dialog_edit_url, null);

                                edit_title = dialogView.findViewById(R.id.pass_title);
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

                                dialog = builder.create();
                                dialog.show();
                                break;
                            case 1:
                                bottomSheetDialog_context.cancel();
                                builder = new AlertDialog.Builder(MainActivity.this);
                                dialogView = View.inflate(MainActivity.this, R.layout.dialog_edit_url, null);

                                edit_title = dialogView.findViewById(R.id.pass_title);
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

                                dialog = builder.create();
                                dialog.show();
                                break;
                            case 2:
                                bottomSheetDialog_context.cancel();
                                sharedPref.edit().putString("favoriteURL", bookmarks_content).putString("favoriteTitle", bookmarks_title).apply();
                                header_start.setText(sharedPref.getString("favoriteTitle", "wetterdienst.de"));
                                break;
                            case 3:
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
}
