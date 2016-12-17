/*
    This file is part of the Diaspora Native WebApp.

    Diaspora Native WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Diaspora Native WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora Native WebApp.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.weather.helper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import de.baumann.weather.Browser;
import de.baumann.weather.Screen_Main;
import de.baumann.weather.R;
import de.baumann.weather.Screen_Weather;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        setContentView(R.layout.activity_splash);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_help, false);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final String startType = sharedPref.getString("startType", "1");

        if (startType.equals("2")) {

            new Handler().postDelayed(new Runnable() {
                public void run() {

                    String startURL = sharedPref.getString("favoriteURL", "http://m.wetterdienst.de/");
                    String startTitle = sharedPref.getString("favoriteTitle", "http://m.wetterdienst.de/");

                    if (startURL.contains("m.wetterdienst.de")) {
                        Intent intent = new Intent(SplashActivity.this, Screen_Weather.class);
                        intent.putExtra("url", startURL);
                        intent.putExtra("url2", startURL + "stuendlich");
                        intent.putExtra("url3", startURL + "10-Tage");
                        intent.putExtra("title", startTitle);
                        startActivity(intent);
                        SplashActivity.this.finish();
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    } else {
                        Intent intent = new Intent(SplashActivity.this, Browser.class);
                        intent.putExtra("url", startURL);
                        startActivity(intent);
                        SplashActivity.this.finish();
                        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                    }
                }
            }, 1000);

        } else if (startType.equals("1")){

            new Handler().postDelayed(new Runnable() {
                public void run() {

                    Intent mainIntent = new Intent(SplashActivity.this, Screen_Main.class);
                    mainIntent.putExtra("id", "1");
                    startActivity(mainIntent);
                    SplashActivity.this.finish();
                    overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                }
            }, 1000);
        }
    }
}
