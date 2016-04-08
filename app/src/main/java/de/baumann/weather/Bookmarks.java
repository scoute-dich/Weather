package de.baumann.weather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class Bookmarks extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listView = null;
    private ImageView imgHeader;

    public void fab5_click(View v){
        // write your code here ..
        Intent intent_in = new Intent(Bookmarks.this, Search.class);
        startActivity(intent_in);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFirstRun();

        setContentView(R.layout.activity_bookmarks);
        setTitle(R.string.action_bookmarks);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final String startType = sharedPref.getString("startType", "1");

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startType.equals("2")) {
                    Intent intent_in = new Intent(Bookmarks.this, Start.class);
                    startActivity(intent_in);
                    overridePendingTransition(0, 0);
                } else if (startType.equals("1")) {
                    Intent intent_in = new Intent(Bookmarks.this, Bookmarks.class);
                    startActivity(intent_in);
                    overridePendingTransition(0, 0);
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        imgHeader = (ImageView) findViewById(R.id.imageView3);

        TypedArray images = getResources().obtainTypedArray(R.array.splash_images);
        int choice = (int) (Math.random() * images.length());
        imgHeader.setImageResource(images.getResourceId(choice, R.drawable.splash1));
        images.recycle();

        listView = (ListView)findViewById(R.id.bookmarks);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                Intent intent = new Intent(Bookmarks.this, Browser.class);
                intent.putExtra("url", map.get("url"));
                intent.putExtra("url2", map.get("url") + "stuendlich");
                intent.putExtra("url3", map.get("url") + "10-Tage");
                intent.putExtra("title", map.get("title"));
                startActivityForResult(intent, 100);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String seqnoStr = map.get("seqno");
                final String title = map.get("title");
                final String url = map.get("url");

                final CharSequence[] options = {getString(R.string.edit_title), getString(R.string.edit_url),getString(R.string.delete_bookmark)};
                new AlertDialog.Builder(Bookmarks.this)
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.edit_title))) {
                                    try {
                                        final EditText input = new EditText(Bookmarks.this);
                                        input.setText(title);
                                        final BrowserDatabase db = new BrowserDatabase(Bookmarks.this);
                                        db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                        final AlertDialog.Builder dialog2 = new AlertDialog.Builder(Bookmarks.this)
                                                .setView(input)
                                                .setMessage(R.string.edit_title)
                                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        String inputTag = input.getText().toString().trim();
                                                        db.addBookmark(inputTag, url);
                                                        db.close();
                                                        setBookmarkList();
                                                    }
                                                })
                                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        dialog.cancel();
                                                    }
                                                });
                                        dialog2.show();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (options[item].equals(getString(R.string.edit_url))) {
                                    try {
                                        final EditText input = new EditText(Bookmarks.this);
                                        input.setText(url);
                                        final BrowserDatabase db = new BrowserDatabase(Bookmarks.this);
                                        db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                        final AlertDialog.Builder dialog2 = new AlertDialog.Builder(Bookmarks.this)
                                                .setView(input)
                                                .setMessage(R.string.edit_url)
                                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        String inputTag = input.getText().toString().trim();
                                                        db.addBookmark(title, inputTag);
                                                        db.close();
                                                        setBookmarkList();
                                                    }
                                                })
                                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        dialog.cancel();
                                                    }
                                                });
                                        dialog2.show();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                                if (options[item].equals(getString(R.string.delete_bookmark))) {


                                    try {
                                        BrowserDatabase db = new BrowserDatabase(Bookmarks.this);
                                        final int count = db.getRecordCount();
                                        db.close();

                                        if (count == 1) {
                                            Snackbar snackbar = Snackbar
                                                    .make(listView, R.string.cannot_remove, Snackbar.LENGTH_LONG);
                                            snackbar.show();

                                        } else {
                                            Snackbar snackbar = Snackbar
                                                    .make(listView, R.string.remove_confirmation, Snackbar.LENGTH_LONG)
                                                    .setAction(R.string.yes, new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            try {
                                                                BrowserDatabase db = new BrowserDatabase(Bookmarks.this);
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

    @Override
    public void onBackPressed() {
        Snackbar snackbar = Snackbar
                .make(listView, R.string.confirm_exit, Snackbar.LENGTH_LONG)
                .setAction(R.string.yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                });
        snackbar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_bookmarks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            moveTaskToBack(true);
        }

        if (id == R.id.action_settings) {
            Intent intent_in = new Intent(Bookmarks.this, UserSettingsActivity.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_favorite) {
            Intent intent_in = new Intent(Bookmarks.this, Start.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_bookmarks) {
            Intent intent_in = new Intent(Bookmarks.this, Bookmarks.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_search) {
            Intent intent_in = new Intent(Bookmarks.this, Search.class);
            startActivity(intent_in);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_radar) {
            Intent intent_ra = new Intent(Bookmarks.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/niederschlag_1h/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_satellit) {
            Intent intent_ra = new Intent(Bookmarks.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/satellit/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_karten) {
            Intent intent_ra = new Intent(Bookmarks.this, Search.class);
            intent_ra.putExtra("url", "https://www.meteoblue.com/de/wetter/karte/film/europa");
            startActivityForResult(intent_ra, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_thema) {
            Intent intent_th = new Intent(Bookmarks.this, Search.class);
            intent_th.putExtra("url", "http://www.dwd.de/SiteGlobals/Forms/ThemaDesTages/ThemaDesTages_Formular.html?pageNo=0&queryResultId=null");
            startActivityForResult(intent_th, 100);
            overridePendingTransition(0, 0);

        } else if (id == R.id.action_lexikon) {
            Intent intent_le = new Intent(Bookmarks.this, Search.class);
            intent_le.putExtra("url", "http://www.dwd.de/DE/service/lexikon/lexikon_node.html");
            startActivityForResult(intent_le, 100);
            overridePendingTransition(0, 0);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

    public void checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstBookmark", true);
        if (isFirstRun){
            // Place your dialog code here to display the dialog
            final SpannableString s = new SpannableString(Html.fromHtml(getString(R.string.firstBookmark_text)));
            Linkify.addLinks(s, Linkify.WEB_URLS);

            final AlertDialog.Builder dialog = new AlertDialog.Builder(Bookmarks.this)
                    .setTitle(R.string.firstBookmark_title)
                    .setMessage(s)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.notagain, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("firstBookmark", false)
                                    .apply();
                        }
                    });
            dialog.show();
        }
    }
}
