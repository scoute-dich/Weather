package de.baumann.weather;

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

public class Start extends AppCompatActivity  {

    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bookmarks);

        listView = (ListView)findViewById(R.id.bookmarks);

        setBookmarkList();

        HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(0);
        Intent intent = new Intent(Start.this, Browser.class);
        intent.putExtra("url", map.get("url"));
        intent.putExtra("url2", map.get("url") + "stuendlich");
        intent.putExtra("url3", map.get("url") + "10-Tage");
        intent.putExtra("title", map.get("title"));
        startActivityForResult(intent, 100);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 100:
                if (resultCode == Activity.RESULT_OK) {
                    if (data.getIntExtra("updated", 0) == 1) {
                        setBookmarkList();
                    }
                }
        }
    }

    private void setBookmarkList() {

        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            BrowserDatabase db = new BrowserDatabase(this);
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
                    this,
                    mapList,
                    android.R.layout.simple_list_item_2,
                    new String[] {"title", "url"},
                    new int[] {android.R.id.text1, android.R.id.text2}
            );

            listView.setAdapter(simpleAdapter);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void fab5_click(View view) {
    }

    public void fab1_click(View view) {
    }

    public void fab2_click(View view) {
    }

    public void fab3_click(View view) {
    }
}