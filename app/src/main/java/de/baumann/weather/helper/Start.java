package de.baumann.weather.helper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import de.baumann.weather.Browser;
import de.baumann.weather.Screen_Weather;

public class Start extends AppCompatActivity  {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String startURL = sharedPref.getString("favoriteURL", "http://m.wetterdienst.de/");
        String startTitle = sharedPref.getString("favoriteTitle", "http://m.wetterdienst.de/");

        if (startURL.contains("m.wetterdienst.de")) {
            Intent intent = new Intent(Start.this, Screen_Weather.class);
            intent.putExtra("url", startURL);
            intent.putExtra("url2", startURL + "stuendlich");
            intent.putExtra("url3", startURL + "10-Tage");
            intent.putExtra("title", startTitle);
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