package de.baumann.weather.fragmentsWeather;

import android.Manifest;
import android.app.AlertDialog;
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
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import de.baumann.weather.R;

public class FragmentForecast extends Fragment {

    private WebView mWebView;
    private SwipeRefreshLayout swipeView;
    private ProgressBar progressBar;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private boolean isNetworkUnAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview, container, false);

        setHasOptionsMenu(true);

        if (android.os.Build.VERSION.SDK_INT >= 21)
            WebView.enableSlowWholeDocumentDraw();


        PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        swipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe);
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

        mWebView = (WebView) rootView.findViewById(R.id.webView);
        assert mWebView != null;
        mWebView.loadUrl("http://m.wetterdienst.de/");
        mWebView.getSettings().setAppCachePath(getActivity().getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); // load online by default

        if (sharedPref.getBoolean ("java", false)){
            mWebView.getSettings().setJavaScriptEnabled(true);
        }

        if (isNetworkUnAvailable()) { // loading offline
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Snackbar.make(container, R.string.toast_cache, Snackbar.LENGTH_LONG).show();
        }

        Intent intent = getActivity().getIntent();
        mWebView.loadUrl(intent.getStringExtra("url3"));
        getActivity().setTitle(intent.getStringExtra("title"));

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
                    mWebView.scrollTo(0, 400);

                } else {
                    progressBar.setVisibility(View.VISIBLE);

                }
            }
        });

        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_saveBookmark).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_share:

                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if (sharedPref.getBoolean ("first_screenshot", false)){
                    final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstScreenshot_text)));
                    Linkify.addLinks(s, Linkify.WEB_URLS);

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.firstScreenshot_title)
                            .setMessage(s)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                    sharedPref.edit()
                                            .putBoolean("first_screenshot", false)
                                            .apply();
                                }
                            });
                    dialog.show();
                } else {
                    final CharSequence[] options = {getString(R.string.action_share_link), getString(R.string.action_share_screenshot), getString(R.string.action_save_screenshot)};
                    new AlertDialog.Builder(getActivity())
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
                                        startActivity(Intent.createChooser(sharingIntent, "Share using"));
                                    }
                                    if (options[item].equals(getString(R.string.action_save_screenshot))) {
                                        screenshot();
                                    }
                                }
                            }).show();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void screenshot() {

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(getActivity())
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
            //noinspection ResultOfMethodCallIgnored
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
        getActivity().sendBroadcast(intent);
    }
}