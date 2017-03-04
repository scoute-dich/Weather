package de.baumann.weather;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import de.baumann.weather.fragmentsMain.FragmentBookmarks;
import de.baumann.weather.fragmentsMain.FragmentInfo;
import de.baumann.weather.helper.Settings;
import de.baumann.weather.helper.helpers;

public class Screen_Main extends AppCompatActivity {

    private SharedPreferences sharedPref;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_main);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_help, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(toolbar != null) {
            helpers.setupToolbar(toolbar, Screen_Main.this);
        }
        helpers.grantPermissionsStorage(Screen_Main.this);
        checkFirstRun();
    }

    private void setupViewPager(ViewPager viewPager) {

        final String startTab = sharedPref.getString("tabMain", "1");
        final int startTabInt = Integer.parseInt(startTab);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(new FragmentInfo(), String.valueOf(getString(R.string.title_weatherInfo)));
        adapter.addFragment(new FragmentBookmarks(), String.valueOf(getString(R.string.title_bookmarks)));

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_bookmarks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent_in = new Intent(Screen_Main.this, Settings.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(viewPager.getCurrentItem() == 0) {
            FragmentInfo fragmentInfo = (FragmentInfo)viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragmentInfo.doBack();
        } else if(viewPager.getCurrentItem() == 1) {
            FragmentBookmarks fragmentInfo = (FragmentBookmarks)viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragmentInfo.doBack();
        }
    }

    private void checkFirstRun() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Screen_Main.this);
        if (sharedPref.getBoolean ("first_bookmark", true)){

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Screen_Main.this)
                    .setTitle(R.string.firstBookmark_title)
                    .setMessage(helpers.textSpannable(getString(R.string.firstBookmark_text)))
                    .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                            sharedPref.edit()
                                    .putBoolean("first_bookmark", false)
                                    .apply();
                        }
                    });
            dialog.show();
        }
    }
}