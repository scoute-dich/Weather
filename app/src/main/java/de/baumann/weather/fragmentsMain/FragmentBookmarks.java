package de.baumann.weather.fragmentsMain;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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
import de.baumann.weather.helper.BrowserDatabase;
import de.baumann.weather.helper.helpers;


public class FragmentBookmarks extends Fragment {

    private ListView listView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_screen_main, container, false);

        setHasOptionsMenu(true);

        ImageView imgHeader = (ImageView) rootView.findViewById(R.id.imageView_header);
        if(imgHeader != null) {
            TypedArray images = getResources().obtainTypedArray(R.array.splash_images);
            int choice = (int) (Math.random() * images.length());
            imgHeader.setImageResource(images.getResourceId(choice, R.drawable.splash1));
            images.recycle();
        }

        listView = (ListView)rootView.findViewById(R.id.bookmarks);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                String url = map.get("url");

                if (url.contains("m.wetterdienst")) {
                    Intent intent = new Intent(getActivity(), Screen_Weather.class);
                    intent.putExtra("url", map.get("url"));
                    intent.putExtra("url2", map.get("url") + "stuendlich");
                    intent.putExtra("url3", map.get("url") + "10-Tage");
                    intent.putExtra("title", map.get("title"));
                    startActivityForResult(intent, 100);
                    getActivity().finish();
                } else {
                    Intent intent = new Intent(getActivity(), Browser.class);
                    intent.putExtra("url", map.get("url"));
                    startActivityForResult(intent, 100);
                    getActivity().finish();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                final HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String seqnoStr = map.get("seqno");
                final String title = map.get("title");
                final String url = map.get("url");

                final CharSequence[] options = {
                        getString(R.string.bookmark_edit_title),
                        getString(R.string.bookmark_edit_url),
                        getString(R.string.bookmark_edit_fav),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(getActivity())
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {

                                    try {

                                        final BrowserDatabase db = new BrowserDatabase(getActivity());

                                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                        View dialogView = View.inflate(getActivity(), R.layout.dialog_edit_title, null);

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
                                        helpers.showKeyboard(getActivity(), edit_title);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (options[item].equals(getString(R.string.bookmark_edit_url))) {

                                    try {

                                        final BrowserDatabase db = new BrowserDatabase(getActivity());

                                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                        View dialogView = View.inflate(getActivity(), R.layout.dialog_edit_url, null);

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
                                        helpers.showKeyboard(getActivity(), edit_title);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (options[item].equals (getString(R.string.bookmark_edit_fav))) {
                                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                    sharedPref.edit()
                                            .putString("favoriteURL", url)
                                            .putString("favoriteTitle", title)
                                            .apply();
                                    Snackbar.make(listView, R.string.bookmark_setFav, Snackbar.LENGTH_LONG).show();
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {

                                    try {
                                        BrowserDatabase db = new BrowserDatabase(getActivity());
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
                                                                BrowserDatabase db = new BrowserDatabase(getActivity());
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

        return rootView;
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
            BrowserDatabase db = new BrowserDatabase(getActivity());
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
                    getActivity(),
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
