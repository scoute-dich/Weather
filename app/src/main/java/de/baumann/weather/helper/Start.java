package de.baumann.weather.helper;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.weather.R;
import de.baumann.weather.Weather;

@SuppressWarnings({"UnusedParameters", "EmptyMethod"})
public class Start extends AppCompatActivity  {

    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_bookmarks);

        listView = (ListView)findViewById(R.id.bookmarks);

        setBookmarkList();

        @SuppressWarnings("unchecked")
        HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(0);
        Intent intent = new Intent(Start.this, Weather.class);
        intent.putExtra("url", map.get("url"));
        intent.putExtra("url2", map.get("url") + "stuendlich");
        intent.putExtra("url3", map.get("url") + "10-Tage");
        intent.putExtra("title", map.get("title"));
        startActivityForResult(intent, 100);
        finish();

    }

    private void setBookmarkList() {

        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            BrowserDatabase db = new BrowserDatabase(Start.this);
            ArrayList<String[]> bookmarkList = new ArrayList<>();
            db.getBookmarks(bookmarkList);
            if (bookmarkList.size() == 0) {
                db.loadInitialData();
                db.getBookmarks(bookmarkList);
            }
            db.close();

            for (String[] strAry : bookmarkList) {
                HashMap<String, String> map = new HashMap<>();
                map.put("seqno", strAry[0]);
                map.put("title", strAry[1]);
                map.put("url", strAry[2]);
                mapList.add(map);
            }

            SimpleAdapter simpleAdapter = new SimpleAdapter(
                    Start.this,
                    mapList,
                    R.layout.list_item2,
                    new String[] {"title", "url"},
                    new int[] {R.id.item, R.id.textView1}
            );

            listView.setAdapter(simpleAdapter);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}