package de.baumann.weather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Created by juergen on 03.04.16. Licensed under GPL. Licensed under GPL.
 */
public class UserSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_user_settings);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.action_settings);


        // Display the fragment as the main content
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


                    File dir = getActivity().getCacheDir();

                    if (dir != null && dir.isDirectory()) {
                        try {
                            File[] children = dir.listFiles();
                            if (children.length > 0) {
                                for (File aChildren : children) {
                                    File[] temp = aChildren.listFiles();
                                    for (File aTemp : temp) {
                                        aTemp.delete();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Cache", "failed cache clean");
                        }
                    }

                    Toast.makeText(activity,R.string.toast_clearCache,Toast.LENGTH_SHORT).show();

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
                            .setPositiveButton(getString(R.string.yes),
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
                            .setPositiveButton(getString(R.string.yes),
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

    @Override
    public void onBackPressed() {
        Intent intent_in = new Intent(de.baumann.weather.UserSettingsActivity.this, Bookmarks.class);
        startActivity(intent_in);
        overridePendingTransition(0, 0);
        finish();
    }
}
