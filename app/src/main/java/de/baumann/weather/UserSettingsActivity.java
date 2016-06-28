package de.baumann.weather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import de.baumann.weather.helper.Start;


public class UserSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_user_settings);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            setTitle(R.string.menu_settings);
        }

        // Display the fragment as the activity_screen_main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private void addClearCacheListener() {

            final Activity activity = getActivity();

            Preference reset = findPreference("clearCache");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference pref)
                {

                    final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.action_clearCache_dialog)));
                    Linkify.addLinks(s, Linkify.WEB_URLS);

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                            .setMessage(s)
                            .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    File dir = getActivity().getCacheDir();

                                    if (dir != null && dir.isDirectory()) {
                                        try {
                                            File[] children = dir.listFiles();
                                            if (children.length > 0) {
                                                for (File aChildren : children) {
                                                    File[] temp = aChildren.listFiles();
                                                    for (File aTemp : temp) {
                                                        //noinspection ResultOfMethodCallIgnored
                                                        aTemp.delete();
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e("Cache", "failed cache clean");
                                        }
                                    }

                                    Toast.makeText(activity,R.string.toast_clearCache,Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            });
                    dialog.show();

                    return true;
                }
            });
        }

        private void addChangelogListener() {
            Preference reset = findPreference("changelog");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference pref)
                {

                    final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.changelog_text)));
                    Linkify.addLinks(s, Linkify.WEB_URLS);

                    final AlertDialog d = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.action_changelog)
                            .setMessage( s )
                            .setPositiveButton(getString(R.string.toast_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }).show();
                    d.show();
                    ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                    return true;
                }
            });
        }

        private void addLicenseListener() {
            Preference reset = findPreference("license");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.about_text)));
                    Linkify.addLinks(s, Linkify.WEB_URLS);

                    final AlertDialog d = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.about_title)
                            .setMessage(s)
                            .setPositiveButton(getString(R.string.toast_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }).show();
                    d.show();
                    ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                    return true;
                }
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.user_settings);
            addLicenseListener();
            addChangelogListener();
            addClearCacheListener();
        }
    }

    public void onBackPressed() {
        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final String startType = sharedPref.getString("startType", "1");
        if (startType.equals("2")) {
            Intent intent_in = new Intent(UserSettingsActivity.this, Start.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        } else if (startType.equals("1")) {
            Intent intent_in = new Intent(UserSettingsActivity.this, Screen_Main.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            final String startType = sharedPref.getString("startType", "1");
            if (startType.equals("2")) {
                Intent intent_in = new Intent(UserSettingsActivity.this, Start.class);
                startActivity(intent_in);
                overridePendingTransition(0, 0);
                finish();
            } else if (startType.equals("1")) {
                Intent intent_in = new Intent(UserSettingsActivity.this, Screen_Main.class);
                startActivity(intent_in);
                overridePendingTransition(0, 0);
                finish();
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
