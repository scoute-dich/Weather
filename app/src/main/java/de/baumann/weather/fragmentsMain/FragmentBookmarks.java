package de.baumann.weather.fragmentsMain;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import de.baumann.weather.Browser;
import de.baumann.weather.R;
import de.baumann.weather.Screen_Weather;
import de.baumann.weather.helper.DbAdapter_Bookmarks;
import de.baumann.weather.helper.helpers;


public class FragmentBookmarks extends Fragment {

    //calling variables
    private DbAdapter_Bookmarks db;
    private ListView lv = null;
    private SharedPreferences sharedPref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_screen_main, container, false);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings_help, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setHasOptionsMenu(true);

        ImageView imgHeader = (ImageView) rootView.findViewById(R.id.imageView_header);
        if(imgHeader != null) {
            TypedArray images = getResources().obtainTypedArray(R.array.splash_images);
            int choice = (int) (Math.random() * images.length());
            imgHeader.setImageResource(images.getResourceId(choice, R.drawable.splash1));
            images.recycle();
        }

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_in = new Intent(getActivity(), Browser.class);
                startActivity(intent_in);
                getActivity().overridePendingTransition(0, 0);
                getActivity().finish();
            }
        });

        lv = (ListView)rootView.findViewById(R.id.list);

        db = new DbAdapter_Bookmarks(getActivity());
        db.open();

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

    public void doBack() {
        //BackPressed in activity will call this;
        if (sharedPref.getBoolean ("longPress", false)){
            Snackbar snackbar = Snackbar
                    .make(lv, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            getActivity().finishAffinity();
                        }
                    });
            snackbar.show();
        } else {
            getActivity().finishAffinity();
        }
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
        final Cursor row = db.fetchAllData(getActivity());
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), layoutstyle, row, column, xml_id, 0) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                Cursor row2 = (Cursor) lv.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String bookmarks_title = row2.getString(row2.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_icon = row2.getString(row2.getColumnIndexOrThrow("bookmarks_icon"));
                final String bookmarks_attachment = row2.getString(row2.getColumnIndexOrThrow("bookmarks_attachment"));
                final String bookmarks_creation = row2.getString(row2.getColumnIndexOrThrow("bookmarks_creation"));

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

                iv_attachment.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        if (bookmarks_attachment.equals("")) {

                            if (db.isExistFav("true")) {
                                Snackbar.make(lv, R.string.bookmark_setFav_not, Snackbar.LENGTH_LONG).show();
                            } else {
                                iv_attachment.setImageResource(R.drawable.star_grey);
                                db.update(Integer.parseInt(_id), bookmarks_title, bookmarks_content, bookmarks_icon, "true", bookmarks_creation);
                                setBookmarkList();
                                sharedPref.edit()
                                        .putString("favoriteURL", bookmarks_content)
                                        .putString("favoriteTitle", bookmarks_title)
                                        .apply();
                                Snackbar.make(lv, R.string.bookmark_setFav, Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            iv_attachment.setImageResource(R.drawable.star_outline);
                            db.update(Integer.parseInt(_id), bookmarks_title, bookmarks_content, bookmarks_icon, "", bookmarks_creation);
                            setBookmarkList();
                        }
                    }
                });

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
                    Intent intent = new Intent(getActivity(), Screen_Weather.class);
                    intent.putExtra("url", bookmarks_content);
                    intent.putExtra("url2", bookmarks_content + "stuendlich");
                    intent.putExtra("url3", bookmarks_content + "10-Tage");
                    intent.putExtra("title", bookmarks_title);
                    startActivityForResult(intent, 100);
                    getActivity().finish();
                } else {
                    Intent intent = new Intent(getActivity(), Browser.class);
                    intent.putExtra("url", bookmarks_content);
                    startActivityForResult(intent, 100);
                    getActivity().finish();
                }
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor row2 = (Cursor) lv.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String bookmarks_title = row2.getString(row2.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_icon = row2.getString(row2.getColumnIndexOrThrow("bookmarks_icon"));
                final String bookmarks_attachment = row2.getString(row2.getColumnIndexOrThrow("bookmarks_attachment"));
                final String bookmarks_creation = row2.getString(row2.getColumnIndexOrThrow("bookmarks_creation"));

                final CharSequence[] options = {
                        getString(R.string.bookmark_edit_title),
                        getString(R.string.bookmark_edit_url),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(getActivity())
                        .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {

                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                    View dialogView = View.inflate(getActivity(), R.layout.dialog_edit_url, null);

                                    final EditText edit_title = (EditText) dialogView.findViewById(R.id.pass_title);
                                    edit_title.setHint(R.string.bookmark_edit_title);
                                    edit_title.setText(bookmarks_title);

                                    builder.setView(dialogView);
                                    builder.setTitle(R.string.bookmark_edit_title);
                                    builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String inputTag = edit_title.getText().toString().trim();
                                            db.update(Integer.parseInt(_id), inputTag, bookmarks_content, bookmarks_icon, bookmarks_attachment, bookmarks_creation);
                                            setBookmarkList();
                                            Snackbar.make(lv, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
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

                                }

                                if (options[item].equals(getString(R.string.bookmark_edit_url))) {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                    View dialogView = View.inflate(getActivity(), R.layout.dialog_edit_title, null);

                                    final EditText edit_title = (EditText) dialogView.findViewById(R.id.pass_title);
                                    edit_title.setHint(R.string.bookmark_edit_url);
                                    edit_title.setText(bookmarks_content);

                                    builder.setView(dialogView);
                                    builder.setTitle(R.string.bookmark_edit_url);
                                    builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String inputTag = edit_title.getText().toString().trim();
                                            db.update(Integer.parseInt(_id), bookmarks_title, inputTag, bookmarks_icon, bookmarks_attachment, bookmarks_creation);
                                            setBookmarkList();
                                            Snackbar.make(lv, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
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
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {
                                    Snackbar snackbar = Snackbar
                                            .make(lv, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                            .setAction(R.string.toast_yes, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    db.delete(Integer.parseInt(_id));
                                                    setBookmarkList();
                                                }
                                            });
                                    snackbar.show();
                                }

                            }
                        }).show();
                return true;
            }
        });
    }
}