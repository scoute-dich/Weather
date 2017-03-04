package de.baumann.weather.helper;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import de.baumann.weather.Browser;
import de.baumann.weather.R;
import de.baumann.weather.Screen_Weather;

public class Popup_bookmarks extends Activity {

    //calling variables
    private DbAdapter_Bookmarks db;
    private ListView lv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);
        lv = (ListView)findViewById(R.id.dialogList);

        db = new DbAdapter_Bookmarks(this);
        db.open();

        setBookmarkList();
    }

    private void setBookmarkList() {

        //display data
        final int layoutstyle=R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.textView_title,
                R.id.textView_des,
                R.id.textView_create
        };
        String[] column = new String[] {
                "bookmarks_title",
                "bookmarks_content",
                "bookmarks_creation"
        };
        final Cursor row = db.fetchAllData(this);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, layoutstyle, row, column, xml_id, 0) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                Cursor row2 = (Cursor) lv.getItemAtPosition(position);
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_attachment = row2.getString(row2.getColumnIndexOrThrow("bookmarks_attachment"));

                View v = super.getView(position, convertView, parent);
                ImageView iv_icon = (ImageView) v.findViewById(R.id.icon);
                final ImageView iv_attachment = (ImageView) v.findViewById(R.id.fav);

                if (bookmarks_content.contains("wetterdienst.de")) {
                    iv_icon.setImageResource(R.drawable.google_maps);
                } else {
                    iv_icon.setImageResource(R.drawable.white_balance_sunny);
                }

                switch (bookmarks_attachment) {
                    case "":
                        iv_attachment.setVisibility(View.VISIBLE);
                        iv_attachment.setImageResource(R.drawable.star_outline);
                        break;
                    default:
                        iv_attachment.setVisibility(View.VISIBLE);
                        iv_attachment.setImageResource(R.drawable.star_grey);
                        break;
                }

                return v;
            }
        };

        lv.setAdapter(adapter);
        //onClick function
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

                Cursor row2 = (Cursor) lv.getItemAtPosition(position);
                final String bookmarks_title = row2.getString(row2.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));

                if (bookmarks_content.contains("m.wetterdienst")) {
                    Intent intent = new Intent(Popup_bookmarks.this, Screen_Weather.class);
                    intent.putExtra("url", bookmarks_content);
                    intent.putExtra("url2", bookmarks_content + "stuendlich");
                    intent.putExtra("url3", bookmarks_content + "10-Tage");
                    intent.putExtra("title", bookmarks_title);
                    startActivityForResult(intent, 100);
                    finish();
                } else {
                    Intent intent = new Intent(Popup_bookmarks.this, Browser.class);
                    intent.putExtra("url", bookmarks_content);
                    startActivityForResult(intent, 100);
                    finish();
                }
            }
        });
    }
}