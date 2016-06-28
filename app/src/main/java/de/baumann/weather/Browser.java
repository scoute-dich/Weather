package de.baumann.weather;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.baumann.weather.helper.BrowserDatabase;
import de.baumann.weather.helper.ImageDownloadTask;
import de.baumann.weather.helper.Start;

public class Browser extends AppCompatActivity  {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 21)
            WebView.enableSlowWholeDocumentDraw();

        setContentView(R.layout.activity_browser);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

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
                        Intent intent_in = new Intent(Browser.this, Screen_Main.class);
                        startActivity(intent_in);
                        overridePendingTransition(0, 0);
                        finish();
                    }
                }
            });

            if (sharedPref.getBoolean ("longPress", false)){
                toolbar.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        finish();
                        return true;
                    }
                });
            }
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
        registerForContextMenu(mWebView);

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
                    } else {
                        mWebView.scrollTo(0, 0);
                        setTitle(R.string.menu_search);
                    }

                } else {
                    progressBar.setVisibility(View.VISIBLE);

                }
            }
        });

        mWebView.setDownloadListener(new DownloadListener() {

            public void onDownloadStart(final String url, String userAgent,
                                        final String contentDisposition, final String mimetype,
                                        long contentLength) {

                final String filename= URLUtil.guessFileName(url, contentDisposition, mimetype);
                Snackbar snackbar = Snackbar
                        .make(mWebView, getString(R.string.toast_download_1) + " " + filename, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                DownloadManager.Request request = new DownloadManager.Request(
                                        Uri.parse(url));

                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.toast_download) + " " +
                                        filename , Snackbar.LENGTH_LONG).show();
                            }
                        });
                snackbar.show();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(Browser.this)
                            .setMessage(R.string.app_permissions)
                            .setPositiveButton(getString(R.string.toast_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (android.os.Build.VERSION.SDK_INT >= 23)
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .setNegativeButton(getString(R.string.toast_cancel), null)
                            .show();
                    return;
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            Intent intent_in = new Intent(Browser.this, Screen_Main.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }
    }

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
                        File directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/");
                        if (!directory.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            directory.mkdirs();
                        }

                        if (url != null) {
                            Uri source = Uri.parse(url);
                            DownloadManager.Request request = new DownloadManager.Request(source);
                            File destinationFile = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/"
                                    + source.getLastPathSegment());
                            request.setDestinationUri(Uri.fromFile(destinationFile));
                            ((DownloadManager) Browser.this.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
                            Snackbar.make(mWebView, getString(R.string.context_saveImage_toast) + " " +
                                    destinationFile.getAbsolutePath() , Snackbar.LENGTH_LONG).show();
                        }
                    }
                    break;

                    case ID_SHARE_IMAGE:
                        if(url != null) {
                            final Uri source = Uri.parse(url);
                            final Uri local = Uri.parse(Environment.getExternalStorageDirectory() + "/Pictures/Websites/"+source.getLastPathSegment());
                            new ImageDownloadTask(local.getPath()) {
                                @Override
                                protected void onPostExecute(Bitmap result) {
                                    Uri myUri= Uri.fromFile(new File(local.getPath()));
                                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                    sharingIntent.setType("image/*");
                                    sharingIntent.putExtra(Intent.EXTRA_STREAM, myUri);
                                    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    Browser.this.startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_image))));
                                }
                            }.execute(url);
                        } else {
                            Snackbar.make(mWebView, R.string.context_shareImage_toast, Snackbar.LENGTH_LONG).show();
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
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPref.getBoolean ("first_search", false)) {
                final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstSearch_text)));
                Linkify.addLinks(s, Linkify.WEB_URLS);

                final AlertDialog.Builder dialog = new AlertDialog.Builder(Browser.this)
                        .setTitle(R.string.firstSearch_title)
                        .setMessage(s)
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
                try {
                    final EditText input = new EditText(this);
                    input.setText(mWebView.getTitle());
                    final BrowserDatabase db = new BrowserDatabase(this);
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                            .setView(input)
                            .setMessage(R.string.bookmark_edit_title)
                            .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String inputTag = input.getText().toString().trim();
                                    db.addBookmark(inputTag, mWebView.getUrl());
                                    db.close();
                                    Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                                }
                            })
                            .setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            });
                    dialog.show();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (id == R.id.action_settings) {
            Intent intent_in = new Intent(Browser.this, UserSettingsActivity.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        if (id == android.R.id.home) {
            Intent intent_in = new Intent(Browser.this, Screen_Main.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        if (id == R.id.action_share) {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPref.getBoolean ("first_screenshot", false)){
                final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstScreenshot_text)));
                Linkify.addLinks(s, Linkify.WEB_URLS);

                final AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.firstScreenshot_title)
                        .setMessage(s)
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
                final CharSequence[] options = {getString(R.string.menu_share_link), getString(R.string.menu_share_screenshot), getString(R.string.menu_save_screenshot)};
                new AlertDialog.Builder(Browser.this)
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.menu_share_link))) {
                                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                    sharingIntent.setType("text/plain");
                                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                    sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                    startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
                                }
                                if (options[item].equals(getString(R.string.menu_share_screenshot))) {
                                    screenshot();
                                    new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/");
                                    Date date = new Date();
                                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_HH-mm", Locale.getDefault());

                                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                    sharingIntent.setType("image/png");
                                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                    sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                    Uri bmpUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/"
                                            + dateFormat.format(date) + ".jpg"));
                                    sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                    startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_screenshot))));
                                }
                                if (options[item].equals(getString(R.string.menu_save_screenshot))) {
                                    screenshot();
                                }
                            }
                        }).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }


    private void checkFirstRunBrowser() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean ("first_browser", false)){
            final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstBrowser_text)));
            Linkify.addLinks(s, Linkify.WEB_URLS);

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Browser.this)
                    .setTitle(R.string.firstBrowser_title)
                    .setMessage(s)
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

        File directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/Websites/");
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean ("first_screenshot", false)){
            final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstScreenshot_text)));
            Linkify.addLinks(s, Linkify.WEB_URLS);

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Browser.this)
                    .setTitle(R.string.firstScreenshot_title)
                    .setMessage(s)
                    .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                            sharedPref.edit()
                                    .putBoolean("first_screenshot", false)
                                    .apply();
                        }
                    });
            dialog.show();
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
