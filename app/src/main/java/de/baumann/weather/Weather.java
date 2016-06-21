package de.baumann.weather;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import de.baumann.weather.fragmentsWeather.FragmentForecast;
import de.baumann.weather.fragmentsWeather.FragmentHourly;
import de.baumann.weather.fragmentsWeather.FragmentOverview;
import de.baumann.weather.helper.Start;

public class Weather extends AppCompatActivity {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkFirstRun();

        if(toolbar != null) {
            final String startType = sharedPref.getString("startType", "1");
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (startType.equals("2")) {
                        Intent intent_in = new Intent(Weather.this, Start.class);
                        startActivity(intent_in);
                        overridePendingTransition(0, 0);
                        finish();
                    } else if (startType.equals("1")) {
                        Intent intent_in = new Intent(Weather.this, Main.class);
                        startActivity(intent_in);
                        overridePendingTransition(0, 0);
                        finish();
                    }
                }
            });
        }


        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(new FragmentOverview(), String.valueOf(getString(R.string.fab1_title)));
        adapter.addFragment(new FragmentHourly(), String.valueOf(getString(R.string.fab2_title)));
        adapter.addFragment(new FragmentForecast(), String.valueOf(getString(R.string.fab3_title)));

        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
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

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);// add return null; to display only icons
        }
    }

    private void checkFirstRun () {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean ("first_browser", false)){
            final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstBrowser_text)));
            Linkify.addLinks(s, Linkify.WEB_URLS);

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Weather.this)
                    .setTitle(R.string.firstBrowser_title)
                    .setMessage(s)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.notagain, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
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
        Intent intent_in = new Intent(Weather.this, Main.class);
        startActivity(intent_in);
        overridePendingTransition(0, 0);
        finish();
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

        if (id == R.id.action_settings) {
            Intent intent_in = new Intent(Weather.this, UserSettingsActivity.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        if (id == R.id.action_exit) {
            finish();
        }

        if (id == android.R.id.home) {
            Intent intent_in = new Intent(Weather.this, Main.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}