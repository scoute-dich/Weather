package de.baumann.weather;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import de.baumann.weather.helper.DbAdapter_Bookmarks;
import de.baumann.weather.helper.Popup_bookmarks;
import de.baumann.weather.helper.helpers;

public class Browser extends AppCompatActivity  {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    private Bitmap bitmap;
    private String shareString;
    private File shareFile;
    private SharedPreferences sharedPref;

    private static final int ID_SAVE_IMAGE = 10;
    private static final int ID_IMAGE_EXTERNAL_BROWSER = 11;
    private static final int ID_COPY_LINK = 12;
    private static final int ID_SHARE_LINK = 13;
    private static final int ID_SHARE_IMAGE = 14;

    private boolean isNetworkUnAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }

        setContentView(R.layout.activity_browser);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_help, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(toolbar != null) {
            helpers.setupToolbar(toolbar, Browser.this);
        }

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
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

        checkFirstRunBrowser();

        mWebView = (WebView) findViewById(R.id.webView);
        assert mWebView != null;
        mWebView.loadUrl("http://m.wetterdienst.de/");
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

        Intent intent = getIntent();
        mWebView.loadUrl(intent.getStringExtra("url"));
        setTitle(intent.getStringExtra("title"));

        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                // do your stuff here
                swipeView.setRefreshing(false);
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

                if (url.startsWith("http")) return false;//open web links as usual
                //try to find browse activity to handle uri
                Uri parsedUri = Uri.parse(url);
                PackageManager packageManager = Browser.this.getPackageManager();
                Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
                if (browseIntent.resolveActivity(packageManager) != null) {
                    Browser.this.startActivity(browseIntent);
                    return true;
                }
                //if not activity found, try to parse intent://
                if (url.startsWith("intent:")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent.resolveActivity(Browser.this.getPackageManager()) != null) {
                            Browser.this.startActivity(intent);
                            return true;
                        }
                        //try to find fallback url
                        String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                        if (fallbackUrl != null) {
                            mWebView.loadUrl(fallbackUrl);
                            return true;
                        }
                        //invite to install
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(
                                Uri.parse("market://details?id=" + intent.getPackage()));
                        if (marketIntent.resolveActivity(packageManager) != null) {
                            Browser.this.startActivity(marketIntent);
                            return true;
                        }
                    } catch (URISyntaxException e) {
                        //not an intent uri
                    }
                }
                return true;//do nothing in other cases
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {

                String url = mWebView.getUrl();

                if (url != null && url.contains("dwd")) {
                    setTitle(R.string.dwd);
                } else if (url != null && url.equals("http://m.wetterdienst.de/")) {
                    setTitle(R.string.menu_search);
                } else {
                    setTitle(mWebView.getTitle());
                }

                progressBar.setProgress(progress);
                progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);

                if (url != null && url.contains("dwd") && progress > 0 && progress <= 60) {
                    mWebView.loadUrl("javascript:(function() { " +
                            "var head = document.getElementsByTagName('header')[0];"
                            + "head.parentNode.removeChild(head);" +
                            "})()");
                }

                if (url != null && url.contains("dwd") && progress > 60) {
                    mWebView.loadUrl("javascript:(function() { " +
                            "var head = document.getElementsByTagName('header')[0];"
                            + "head.parentNode.removeChild(head);" +
                            "})()");
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
                                dm.enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.toast_download) + " " + filename , Snackbar.LENGTH_LONG).show();
                            }
                        });
                snackbar.show();
            }
        });
        helpers.grantPermissionsStorage(Browser.this);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            if (sharedPref.getBoolean ("longPress", false)){
                Snackbar snackbar = Snackbar
                        .make(mWebView, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                finishAffinity();
                            }
                        });
                snackbar.show();
            }
            mWebView.goBack();
        } else {
            if (sharedPref.getBoolean ("longPress", false)){
                Snackbar snackbar = Snackbar
                        .make(mWebView, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                finishAffinity();
                            }
                        });
                snackbar.show();
            } else {
                finishAffinity();
            }
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
            Browser.this.unregisterReceiver(onComplete);
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
            Browser.this.startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_image))));
            unregisterReceiver(onComplete2);
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        WebView w = (WebView)v;
        WebView.HitTestResult result = w.getHitTestResult();

        MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                WebView.HitTestResult result = mWebView.getHitTestResult();
                String url = result.getExtra();
                switch (item.getItemId()) {
                    //Save image to external memory
                    case ID_SAVE_IMAGE: {
                        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                        try {
                            if (url != null) {

                                Uri source = Uri.parse(url);
                                DownloadManager.Request request = new DownloadManager.Request(source);
                                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, helpers.newFileName());
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.context_saveImage_toast) + " " + helpers.newFileName() , Snackbar.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    break;

                    case ID_SHARE_IMAGE:
                        if(url != null) {

                            shareString = helpers.newFileName();
                            shareFile = helpers.newFile();

                            try {
                                Uri source = Uri.parse(url);
                                DownloadManager.Request request = new DownloadManager.Request(source);
                                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, shareString);
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.context_saveImage_toast) + " " + helpers.newFileName() , Snackbar.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                            }
                            registerReceiver(onComplete2, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                        }
                        break;

                    case ID_IMAGE_EXTERNAL_BROWSER:
                        if (url != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            Browser.this.startActivity(intent);
                        }
                        break;

                    //Copy url to clipboard
                    case ID_COPY_LINK:
                        if (url != null) {
                            ClipboardManager clipboard = (ClipboardManager) Browser.this.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
                            Snackbar.make(mWebView, R.string.context_linkCopy_toast, Snackbar.LENGTH_LONG).show();
                        }
                        break;

                    //Try to share link to other apps
                    case ID_SHARE_LINK:
                        if (url != null) {
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                            sendIntent.setType("text/plain");
                            Browser.this.startActivity(Intent.createChooser(sendIntent, getResources()
                                    .getString(R.string.app_share_link)));
                        }
                        break;
                }
                return true;
            }
        };

        if(result.getType() == WebView.HitTestResult.IMAGE_TYPE){
            menu.add(0, ID_SAVE_IMAGE, 0, getString(R.string.context_saveImage)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_SHARE_IMAGE, 0, getString(R.string.context_shareImage)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_IMAGE_EXTERNAL_BROWSER, 0, getString(R.string.context_externalBrowser)).setOnMenuItemClickListener(handler);
        } else if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            menu.add(0, ID_COPY_LINK, 0, getString(R.string.context_linkCopy)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_SHARE_LINK, 0, getString(R.string.menu_share_link)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_IMAGE_EXTERNAL_BROWSER, 0, getString(R.string.context_externalBrowser)).setOnMenuItemClickListener(handler);
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

        if (id == R.id.action_saveBookmark) {
            if (sharedPref.getBoolean ("first_search", true)) {

                final AlertDialog.Builder dialog = new AlertDialog.Builder(Browser.this)
                        .setTitle(R.string.firstSearch_title)
                        .setMessage(helpers.textSpannable(getString(R.string.firstSearch_text)))
                        .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                                sharedPref.edit()
                                        .putBoolean("first_search", false)
                                        .apply();
                            }
                        });
                dialog.show();
            } else {

                final DbAdapter_Bookmarks db = new DbAdapter_Bookmarks(this);
                db.open();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final View dialogView = View.inflate(this, R.layout.dialog_edit_title, null);

                final EditText edit_title = (EditText) dialogView.findViewById(R.id.pass_title);
                edit_title.setHint(R.string.bookmark_edit_title);
                edit_title.setText(mWebView.getTitle());

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
                            db.insert(inputTag, mWebView.getUrl(), "04", "", helpers.createDate());
                            dialog2.dismiss();
                            Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });

                helpers.showKeyboard(this,edit_title);
            }
        }

        if (id == R.id.action_bookmark) {
            Intent intent_in = new Intent(Browser.this, Popup_bookmarks.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
        }

        if (id == android.R.id.home) {
            Intent intent_in = new Intent(Browser.this, Screen_Main.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        if (id == R.id.menu_save_screenshot) {
            screenshot();
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

    private void checkFirstRunBrowser() {
        if (sharedPref.getBoolean ("first_browser", true)){

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Browser.this)
                    .setTitle(R.string.firstBrowser_title)
                    .setMessage(helpers.textSpannable(getString(R.string.firstBrowser_text)))
                    .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                            sharedPref.edit()
                                    .putBoolean("first_browser", false)
                                    .apply();
                        }
                    });
            dialog.show();
        }
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
