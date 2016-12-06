package de.baumann.weather.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.weather.Browser;
import de.baumann.weather.R;
import de.baumann.weather.Screen_Weather;

public class Popup_bookmarks extends Activity {

    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_help, false);

        listView = (ListView)findViewById(R.id.dialogList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                String url = map.get("url");

                if (url.contains("m.wetterdienst")) {
                    Intent intent = new Intent(Popup_bookmarks.this, Screen_Weather.class);
                    intent.putExtra("url", map.get("url"));
                    intent.putExtra("url2", map.get("url") + "stuendlich");
                    intent.putExtra("url3", map.get("url") + "10-Tage");
                    intent.putExtra("title", map.get("title"));
                    startActivityForResult(intent, 100);
                    finish();
                } else {
                    Intent intent = new Intent(Popup_bookmarks.this, Browser.class);
                    intent.putExtra("url", map.get("url"));
                    startActivityForResult(intent, 100);
                    finish();
                }
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                String url = map.get("url");

                if (url.contains("m.wetterdienst")) {
                    Intent intent = new Intent(Popup_bookmarks.this, Screen_Weather.class);
                    intent.putExtra("url", map.get("url"));
                    intent.putExtra("url2", map.get("url") + "stuendlich");
                    intent.putExtra("url3", map.get("url") + "10-Tage");
                    intent.putExtra("title", map.get("title"));
                    startActivityForResult(intent, 100);
                    Popup_bookmarks.this.finish();
                } else {
                    Intent intent = new Intent(Popup_bookmarks.this, Browser.class);
                    intent.putExtra("url", map.get("url"));
                    startActivityForResult(intent, 100);
                    Popup_bookmarks.this.finish();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String seqnoStr = map.get("seqno");
                final String title = map.get("title");
                final String url = map.get("url");

                final CharSequence[] options = {
                        getString(R.string.bookmark_edit_title),
                        getString(R.string.bookmark_edit_url),
                        getString(R.string.bookmark_edit_fav),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(Popup_bookmarks.this)
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {

                                    try {

                                        final BrowserDatabase db = new BrowserDatabase(Popup_bookmarks.this);

                                        AlertDialog.Builder builder = new AlertDialog.Builder(Popup_bookmarks.this);
                                        View dialogView = View.inflate(Popup_bookmarks.this, R.layout.dialog_edit_title, null);

                                        final EditText edit_title = (EditText) dialogView.findViewById(R.id.pass_title);
                                        edit_title.setText(title);

                                        builder.setView(dialogView);
                                        builder.setTitle(R.string.bookmark_edit_title);
                                        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                String inputTag = edit_title.getText().toString().trim();
                                                db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                                db.addBookmark(inputTag, url);
                                                db.close();
                                                setBookmarkList();
                                                Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                        builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                dialog.cancel();
                                            }
                                        });

                                        final AlertDialog dialog2 = builder.create();
                                        // Display the custom alert dialog on interface
                                        dialog2.show();
                                        helpers.showKeyboard(Popup_bookmarks.this, edit_title);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (options[item].equals(getString(R.string.bookmark_edit_url))) {

                                    try {

                                        final BrowserDatabase db = new BrowserDatabase(Popup_bookmarks.this);

                                        AlertDialog.Builder builder = new AlertDialog.Builder(Popup_bookmarks.this);
                                        View dialogView = View.inflate(Popup_bookmarks.this, R.layout.dialog_edit_url, null);

                                        final EditText edit_title = (EditText) dialogView.findViewById(R.id.pass_title);
                                        edit_title.setText(url);

                                        builder.setView(dialogView);
                                        builder.setTitle(R.string.bookmark_edit_url);
                                        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                String inputTag = edit_title.getText().toString().trim();
                                                db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                                db.addBookmark(title, inputTag);
                                                db.close();
                                                setBookmarkList();
                                                Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                        builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                dialog.cancel();
                                            }
                                        });

                                        final AlertDialog dialog2 = builder.create();
                                        // Display the custom alert dialog on interface
                                        dialog2.show();
                                        helpers.showKeyboard(Popup_bookmarks.this, edit_title);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (options[item].equals (getString(R.string.bookmark_edit_fav))) {
                                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Popup_bookmarks.this);
                                    sharedPref.edit()
                                            .putString("favoriteURL", url)
                                            .putString("favoriteTitle", title)
                                            .apply();
                                    Snackbar.make(listView, R.string.bookmark_setFav, Snackbar.LENGTH_LONG).show();
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {

                                    try {
                                        BrowserDatabase db = new BrowserDatabase(Popup_bookmarks.this);
                                        final int count = db.getRecordCount();
                                        db.close();

                                        if (count == 1) {
                                            Snackbar snackbar = Snackbar
                                                    .make(listView, R.string.bookmark_remove_cannot, Snackbar.LENGTH_LONG);
                                            snackbar.show();

                                        } else {
                                            Snackbar snackbar = Snackbar
                                                    .make(listView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                                    .setAction(R.string.toast_yes, new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            try {
                                                                BrowserDatabase db = new BrowserDatabase(Popup_bookmarks.this);
                                                                db.deleteBookmark(Integer.parseInt(seqnoStr));
                                                                db.close();
                                                                setBookmarkList();
                                                            } catch (PackageManager.NameNotFoundException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    });
                                            snackbar.show();
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        }).show();

                return true;
            }
        });

        setBookmarkList();
    }

    private void setBookmarkList() {

        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            BrowserDatabase db = new BrowserDatabase(Popup_bookmarks.this);
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
                    Popup_bookmarks.this,
                    mapList,
                    R.layout.list_item,
                    new String[] {"title", "url"},
                    new int[] {R.id.textView_title, R.id.textView_des}
            ){
                @Override
                public View getView (final int position, final View convertView, final ViewGroup parent) {

                    @SuppressWarnings("unchecked")
                    HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                    final String url = map.get("url");

                    View v = super.getView(position, convertView, parent);
                    ImageView i=(ImageView) v.findViewById(R.id.icon);

                    if (url.contains("wetterdienst.de")) {
                        i.setImageResource(R.drawable.google_maps);
                    } else {
                        i.setImageResource(R.drawable.white_balance_sunny);
                    }
                    return v;
                }
            };
            listView.setAdapter(simpleAdapter);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}