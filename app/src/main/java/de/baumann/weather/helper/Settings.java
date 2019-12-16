package de.baumann.weather.helper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;

import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Objects;

import de.baumann.weather.R;
import de.baumann.weather.helpers;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        setTitle(R.string.menu_settings);

        androidx.preference.PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new SettingsFragment())
                .commit();
    }

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.user_settings, rootKey);

            Objects.requireNonNull(findPreference("license")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
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
                    return false;
                }
            });

            Objects.requireNonNull(findPreference("donate")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=NP6TGYDYP9SHY"); // missing 'http://' will cause crashed
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
