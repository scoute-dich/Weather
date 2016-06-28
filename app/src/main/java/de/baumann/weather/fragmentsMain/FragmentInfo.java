package de.baumann.weather.fragmentsMain;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import de.baumann.weather.Browser;
import de.baumann.weather.R;
import de.baumann.weather.helper.CustomListAdapter;


public class FragmentInfo extends Fragment {

    private String state;
    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String forecast = sharedPref.getString("forecast", "http://www.dwd.de/DE/wetter/vorhersage_aktuell/baden-wuerttemberg/vhs_bawue_node.html");

        switch (forecast) {
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/10-tage/10tage_node.html":
                state = getString(R.string.state_1);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/baden-wuerttemberg/vhs_bawue_node.html":
                state = getString(R.string.state_2);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/bayern/vhs_bay_node.html":
                state = getString(R.string.state_3);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/berlin_brandenburg/vhs_bbb_node.html":
                state = getString(R.string.state_4);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/hessen/vhs_hes_node.html":
                state = getString(R.string.state_5);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/mecklenburg_vorpommern/vhs_mvp_node.html":
                state = getString(R.string.state_6);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/niedersachsen_bremen/vhs_nib_node.html":
                state = getString(R.string.state_7);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/nordrhein_westfalen/vhs_nrw_node.html":
                state = getString(R.string.state_8);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/rheinland-pfalz_saarland/vhs_rps_node.html":
                state = getString(R.string.state_9);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/sachsen/vhs_sac_node.html":
                state = getString(R.string.state_10);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/sachen_anhalt/vhs_saa_node.html":
                state = getString(R.string.state_11);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/schleswig_holstein_hamburg/vhs_shh_node.html":
                state = getString(R.string.state_12);
                break;
            case "http://www.dwd.de/DE/wetter/vorhersage_aktuell/thueringen/vhs_thu_node.html":
                state = getString(R.string.state_13);
                break;
        }


        final String[] itemname ={
                getString(R.string.dwd_forecast) + " " + state,
                getString(R.string.dwd_radar),
                getString(R.string.dwd_karten),
                getString(R.string.dwd_satellit),
                getString(R.string.dwd_thema),
                getString(R.string.dwd_lexikon),
                getString(R.string.dwd_webSearch),
        };

        final String[] itemURL ={
                forecast,
                "http://www.dwd.de/DE/leistungen/radarbild_film/radarbild_film.html",
                "http://www.dwd.de/DE/leistungen/hobbymet_wk_europa/hobbyeuropakarten.html?nn=357606",
                "http://www.dwd.de/DE/leistungen/satellit_metsat8000stc/satellit_metsat8000stc.html?nn=357606",
                "http://www.dwd.de/SiteGlobals/Forms/ThemaDesTages/ThemaDesTages_Formular.html?pageNo=0&queryResultId=null",
                "http://www.dwd.de/DE/service/lexikon/lexikon_node.html",
                "https://startpage.com/",
        };

            final String[] itemDES ={
                getString(R.string.text_des_1),
                getString(R.string.text_des_2),
                getString(R.string.text_des_3),
                getString(R.string.text_des_4),
                getString(R.string.text_des_5),
                getString(R.string.text_des_6),
                getString(R.string.text_des_7),
        };

        Integer[] imgid={
                R.drawable.img_1,
                R.drawable.img_2,
                R.drawable.img_3,
                R.drawable.img_4,
                R.drawable.img_5,
                R.drawable.img_6,
                R.drawable.img_7,
        };

        View rootView = inflater.inflate(R.layout.fragment_screen_main, container, false);


        setHasOptionsMenu(true);

        ImageView imgHeader = (ImageView) rootView.findViewById(R.id.imageView3);
        if(imgHeader != null) {
            TypedArray images = getResources().obtainTypedArray(R.array.splash_images);
            int choice = (int) (Math.random() * images.length());
            imgHeader.setImageResource(images.getResourceId(choice, R.drawable.splash1));
            images.recycle();
        }

        CustomListAdapter adapter=new CustomListAdapter(getActivity(), itemname, itemURL, itemDES, imgid);
        listView = (ListView)rootView.findViewById(R.id.bookmarks);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub
                String Selecteditem= itemURL[+position];
                Intent intent = new Intent(getActivity(), Browser.class);
                intent.putExtra("url", Selecteditem);
                startActivityForResult(intent, 100);
                getActivity().finish();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final String title = itemname[+position];
                final String url = itemURL[+position];

                final CharSequence[] options = {getString(R.string.bookmark_edit_fav)};
                new AlertDialog.Builder(getActivity())
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {

                                if (options[item].equals (getString(R.string.bookmark_edit_fav))) {
                                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                    sharedPref.edit()
                                            .putString("favoriteURL", url)
                                            .putString("favoriteTitle", title)
                                            .apply();
                                    Snackbar.make(listView, R.string.bookmark_setFav, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }).show();

                return true;
            }
        });

        return rootView;
    }
}
