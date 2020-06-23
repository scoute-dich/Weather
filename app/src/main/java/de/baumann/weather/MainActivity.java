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

        bottomAppBar.setOnTouchListener(new SwipeTouchListener(activity) {
            final NestedScrollView scrollView = findViewById(R.id.scrollView);
            public void onSwipeTop() {
                scrollView.smoothScrollTo(0,0);
            }
            public void onSwipeBottom() {
                scrollView.smoothScrollTo(0,1000000000);
            }
            public void onSwipeLeft() {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
            public void onSwipeRight() {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
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
                setTitle(mWebView.getTitle());
                progressBar.setProgress(progress);
                progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
            }
        });

        mWebView.loadUrl(startURL);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> openMenu(activity));

        fab.setOnLongClickListener(v -> {
            openBookmarks(activity);
            return false;
        });

        fab.setOnTouchListener(new SwipeTouchListener(activity) {
            final NestedScrollView scrollView = findViewById(R.id.scrollView);
            public void onSwipeTop() {
                scrollView.smoothScrollTo(0,0);
            }
            public void onSwipeBottom() {
                scrollView.smoothScrollTo(0,1000000000);
            }
            public void onSwipeLeft() {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
            public void onSwipeRight() {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                }
            }
        });

        ImageButton ib_bookmarks = findViewById(R.id.ib_bookmarks);
        ib_bookmarks.setOnClickListener(v -> openBookmarks(activity));

        ImageButton ib_menu = findViewById(R.id.ib_menu);
        ib_menu.setOnClickListener(v -> openMenu(activity));
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

        grid.setOnItemClickListener((parent, view, position, id) -> {

            switch (position) {
                case 0:
                    bottomSheetDialog.cancel();

                    final BottomSheetDialog bottomSheetDialog_context = new BottomSheetDialog(Objects.requireNonNull(activity));
                    View dialogView1 = View.inflate(activity, R.layout.grid_layout, null);

                    final TextView tv = dialogView1.findViewById(R.id.grid_title);
                    tv.setText(getString(R.string.menu_settings));

                    GridView grid1 = dialogView1.findViewById(R.id.grid_item);
                    GridItem_Menu itemAlbum_01 = new GridItem_Menu(getResources().getString(R.string.action_license), R.drawable.icon_copyright);
                    GridItem_Menu itemAlbum_031 = new GridItem_Menu(getResources().getString(R.string.action_donate), R.drawable.icon_donate);
                    GridItem_Menu itemAlbum_04 = new GridItem_Menu(getResources().getString(R.string.menu_insertDefaultBookmarks), R.drawable.icon_bookmark);

                    final List<GridItem_Menu> gridList1 = new LinkedList<>();
                    gridList1.add(gridList1.size(), itemAlbum_01);
                    gridList1.add(gridList1.size(), itemAlbum_031);
                    gridList1.add(gridList1.size(), itemAlbum_04);

                    GridAdapter_Menu gridAdapter1 = new GridAdapter_Menu(activity, gridList1);
                    grid1.setNumColumns(1);
                    grid1.setAdapter(gridAdapter1);
                    gridAdapter1.notifyDataSetChanged();

                    grid1.setOnItemClickListener((parent1, view1, position1, id12) -> {

                        switch (position1) {
                            case 0:
                                bottomSheetDialog_context.cancel();
                                AlertDialog d = new AlertDialog.Builder(activity)
                                        .setTitle(R.string.about_title)
                                        .setMessage(helpers.textSpannable(getString(R.string.about_text)))
                                        .setPositiveButton(getString(R.string.toast_yes),
                                                (dialog, id1) -> dialog.cancel()).show();
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
                    });

                    bottomSheetDialog_context.setContentView(dialogView1);
                    bottomSheetDialog_context.show();
                    helpers.setBottomSheetBehavior(bottomSheetDialog_context, dialogView1);
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
        ib_search.setOnClickListener(view -> {
            bottomSheetDialog.cancel();
            mWebView.loadUrl("https://www.wetterdienst.de");
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
        bookmarkList.setOnItemClickListener((adapterview, view, position, id) -> {
            Cursor row12 = (Cursor) bookmarkList.getItemAtPosition(position);
            final String bookmarks_content = row12.getString(row12.getColumnIndexOrThrow("bookmarks_content"));
            bottomSheetDialog.cancel();
            mWebView.loadUrl(bookmarks_content);
        });

        bookmarkList.setOnItemLongClickListener((parent, view, position, id) -> {

            Cursor row1 = (Cursor) bookmarkList.getItemAtPosition(position);
            final String _id = row1.getString(row1.getColumnIndexOrThrow("_id"));
            final String bookmarks_title = row1.getString(row1.getColumnIndexOrThrow("bookmarks_title"));
            final String bookmarks_content = row1.getString(row1.getColumnIndexOrThrow("bookmarks_content"));

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

            grid.setOnItemClickListener((parent1, view12, position1, id1) -> {

                switch (position1) {
                    case 0:
                        bottomSheetDialog_context.cancel();
                        editBookmark(activity, bookmarks_title, bookmarks_content, Integer.parseInt(_id));
                        break;
                    case 1:
                        bottomSheetDialog_context.cancel();
                        sharedPref.edit().putString("favoriteURL", bookmarks_content).putString("favoriteTitle", bookmarks_title).apply();
                        String text = getString(R.string.bookmark_fav) + ": " + bookmarks_title;
                        bookmarkTitle.setText(text);
                        break;
                    case 2:
                        bottomSheetDialog_context.cancel();
                        bottomSheetDialog.cancel();
                        Snackbar snackbar = Snackbar
                                .make(bottomAppBar, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_SHORT)
                                .setAction(R.string.toast_yes, view1 -> {
                                    db.delete(Integer.parseInt(_id));
                                    setBookmarkList();
                                });
                        snackbar.show();
                        break;
                }
            });

            bottomSheetDialog_context.setContentView(dialogView);
            bottomSheetDialog_context.show();
            helpers.setBottomSheetBehavior(bottomSheetDialog_context, dialogView);
            return true;
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
            mWebView.goBack();
        } else {
            Snackbar snackbar = Snackbar
                    .make(bottomAppBar, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.toast_yes), view -> finish());
            snackbar.show();
        }
    }

    private void editBookmark(final Activity activity, String title, final String url, final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View dialogView = View.inflate(activity, R.layout.dialog_edit_bookmark, null);
        final EditText edit_title = dialogView.findViewById(R.id.pass_title);
        edit_title.setText(title.replace("/", "").replace("_", " "));
        builder.setView(dialogView);
        builder.setTitle(R.string.bookmark_edit_title);
        builder.setPositiveButton(R.string.toast_yes, (dialog, whichButton) -> {
            String inputTitle = edit_title.getText().toString().trim();
            DbAdapter_Bookmarks db = new DbAdapter_Bookmarks(activity);
            db.open();
            if(db.isExist(url)){
                if (url.startsWith("https://m.wetterdienst.de/Wetter/") ) {
                    db.update(id, inputTitle, url);
                    dialog.dismiss();
                } else {
                    db.update(id, inputTitle, url);
                    dialog.dismiss();
                }
            }else{
                if (url.startsWith("https://m.wetterdienst.de/Wetter/") ) {
                    db.insert(inputTitle, url);
                    dialog.dismiss();
                } else {
                    db.insert(inputTitle, url);
                    dialog.dismiss();
                }
            }
            setBookmarkList();
            Toast.makeText(activity, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.toast_cancel, (dialog, whichButton) -> dialog.cancel());
        final AlertDialog dialog = builder.create();
        dialog.show();
    }
}
