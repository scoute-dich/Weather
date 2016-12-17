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

import de.baumann.weather.fragmentsMain.FragmentBookmarks;
import de.baumann.weather.fragmentsMain.FragmentInfo;
import de.baumann.weather.helper.Popup_bookmarks;
import de.baumann.weather.helper.Popup_info;
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

        final String startTab = sharedPref.getString("tabMain", "0");
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

        if (id == R.id.action_search) {
            Intent intent_in = new Intent(Screen_Main.this, Browser.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        if (id == R.id.action_shortcut) {
            final CharSequence[] options = {
                    getString(R.string.title_bookmarks),
                    getString(R.string.title_weatherInfo)};
            new AlertDialog.Builder(Screen_Main.this)
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals(getString(R.string.title_bookmarks))) {
                                Intent i = new Intent(getApplicationContext(), Popup_bookmarks.class);
                                i.setAction(Intent.ACTION_MAIN);

                                Intent shortcut = new Intent();
                                shortcut.setAction(Intent.ACTION_MAIN);
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, (getString(R.string.app_name)) + " | " + getString(R.string.title_bookmarks));
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                        Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                                shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                sendBroadcast(shortcut);
                                Snackbar.make(viewPager, R.string.toast_shortcut, Snackbar.LENGTH_LONG).show();
                            }
                            if (options[item].equals(getString(R.string.title_weatherInfo))) {
                                Intent i = new Intent(getApplicationContext(), Popup_info.class);
                                i.setAction(Intent.ACTION_MAIN);

                                Intent shortcut = new Intent();
                                shortcut.setAction(Intent.ACTION_MAIN);
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, (getString(R.string.app_name)) + " | " + getString(R.string.title_weatherInfo));
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                        Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                                shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                sendBroadcast(shortcut);
                                Snackbar.make(viewPager, R.string.toast_shortcut, Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }).show();


        }

        return super.onOptionsItemSelected(item);
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