package de.baumann.weather;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import de.baumann.weather.fragmentsWeather.FragmentForecast;
import de.baumann.weather.fragmentsWeather.FragmentHourly;
import de.baumann.weather.fragmentsWeather.FragmentOverview;
import de.baumann.weather.helper.Popup_bookmarks;
import de.baumann.weather.helper.helpers;

public class Screen_Weather extends AppCompatActivity {

    private ViewPager viewPager;
    private SharedPreferences sharedPref;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_weather);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_help, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkFirstRun();

        if(toolbar != null) {
            helpers.setupToolbar(toolbar, Screen_Weather.this);
        }

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);

        helpers.grantPermissionsStorage(Screen_Weather.this);
    }

    private void setupViewPager(ViewPager viewPager) {

        final String startTab = sharedPref.getString("tabWeather", "0");
        final int startTabInt = Integer.parseInt(startTab);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(new FragmentOverview(), String.valueOf(getString(R.string.title_overview)));
        adapter.addFragment(new FragmentHourly(), String.valueOf(getString(R.string.title_hourly)));
        adapter.addFragment(new FragmentForecast(), String.valueOf(getString(R.string.title_trend)));

        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startTabInt,true);

    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);// add return null; to display only icons
        }
    }

    private void checkFirstRun () {
        if (sharedPref.getBoolean ("first_browser", true)){

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Screen_Weather.this)
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

    @Override
    public void onBackPressed() {
        if (sharedPref.getBoolean ("longPress", false)){
            Snackbar snackbar = Snackbar
                    .make(viewPager, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_bookmark) {
            Intent intent_in = new Intent(Screen_Weather.this, Popup_bookmarks.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
        }

        if (id == android.R.id.home) {
            Intent intent_in = new Intent(Screen_Weather.this, Screen_Main.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}