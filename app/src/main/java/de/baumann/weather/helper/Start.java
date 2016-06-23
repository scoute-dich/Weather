package de.baumann.weather.helper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.weather.Browser;
import de.baumann.weather.R;
import de.baumann.weather.Weather;

public class Start extends AppCompatActivity  {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String startURL = sharedPref.getString("favorite", "http://m.wetterdienst.de/");

        if (startURL.contains("m.wetterdienst.de")) {
            Intent intent = new Intent(Start.this, Weather.class);
            intent.putExtra("url", startURL);
            startActivityForResult(intent, 100);
            finish();
        } else {
            Intent intent = new Intent(Start.this, Browser.class);
            intent.putExtra("url", startURL);
            startActivityForResult(intent, 100);
            finish();
        }
    }
}