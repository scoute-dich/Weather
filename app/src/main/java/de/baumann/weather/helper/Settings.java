package de.baumann.weather.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import de.baumann.weather.R;
import de.baumann.weather.Screen_Main;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        setTitle(R.string.menu_settings);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_help, false);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(toolbar != null) {
            helpers.setupToolbar(toolbar, Settings.this);
        }

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Display the fragment as the activity_screen_main content
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new SettingsFragment())
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

                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    getActivity().startActivity(intent);

                    return true;
                }
            });
        }

        private void addChangelogListener() {
            Preference reset = findPreference("changelog");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    Uri uri = Uri.parse("https://github.com/scoute-dich/Weather/blob/master/CHANGELOG.md"); // missing 'http://' will cause crashed
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }
            });
        }

        private void addLicenseListener() {
            Preference reset = findPreference("license");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    final AlertDialog d = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.about_title)
                            .setMessage(helpers.textSpannable(getString(R.string.about_text)))
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

        private void addDonateListListener() {

            Preference reset = findPreference("donate");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=NP6TGYDYP9SHY"); // missing 'http://' will cause crashed
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }
            });
        }

        private void add_helpChooseListener() {

            Preference reset = findPreference("help");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    Intent intent_in = new Intent(getActivity(), Settings_help.class);
                    startActivity(intent_in);
                    getActivity().overridePendingTransition(0, 0);
                    getActivity().finish();
                    return true;
                }
            });
        }

        private void addShortcutListener() {

            final Activity activity = getActivity();
            Preference reset = findPreference("shortcuts");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    final CharSequence[] options = {
                            getString(R.string.title_bookmarks),
                            getString(R.string.title_weatherInfo)};
                    new AlertDialog.Builder(getActivity())
                            .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            })
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int item) {
                                    if (options[item].equals(getString(R.string.title_bookmarks))) {
                                        Intent i = new Intent(getActivity().getApplicationContext(), Popup_bookmarks.class);
                                        i.setAction(Intent.ACTION_MAIN);

                                        Intent shortcut = new Intent();
                                        shortcut.setAction(Intent.ACTION_MAIN);
                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, (getString(R.string.app_name)) + " | " + getString(R.string.title_bookmarks));
                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                                Intent.ShortcutIconResource.fromContext(getActivity().getApplicationContext(), R.mipmap.ic_launcher));
                                        shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                        getActivity().sendBroadcast(shortcut);
                                        helpers.makeToast(activity, getString(R.string.toast_shortcut));
                                    }
                                    if (options[item].equals(getString(R.string.title_weatherInfo))) {
                                        Intent i = new Intent(getActivity().getApplicationContext(), Popup_info.class);
                                        i.setAction(Intent.ACTION_MAIN);

                                        Intent shortcut = new Intent();
                                        shortcut.setAction(Intent.ACTION_MAIN);
                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, (getString(R.string.app_name)) + " | " + getString(R.string.title_weatherInfo));
                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                                Intent.ShortcutIconResource.fromContext(getActivity().getApplicationContext(), R.mipmap.ic_launcher));
                                        shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                        getActivity().sendBroadcast(shortcut);
                                        helpers.makeToast(activity, getString(R.string.toast_shortcut));
                                    }
                                }
                            }).show();

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
            addDonateListListener();
            add_helpChooseListener();
            addShortcutListener();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent_in = new Intent(Settings.this, Screen_Main.class);
        startActivity(intent_in);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            Intent intent_in = new Intent(Settings.this, Screen_Main.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
