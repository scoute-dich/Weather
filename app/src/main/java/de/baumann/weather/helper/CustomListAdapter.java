package de.baumann.weather.helper;


import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.baumann.weather.R;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] itemTITLE;
    private final String[] itemURL;
    private final String[] itemDES;
    private final Integer[] imgid;

    public CustomListAdapter(Activity context, String[] itemTITLE, String[] itemURL, String[] itemDES, Integer[] imgid) {
        super(context, R.layout.list_item, itemTITLE);

        this.context=context;
        this.itemTITLE=itemTITLE;
        this.itemURL=itemURL;
        this.itemDES=itemDES;
        this.imgid=imgid;
    }

    @NonNull
    public View getView(int position, View rowView, @NonNull ViewGroup parent) {

        if (rowView == null) {
            LayoutInflater infInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = infInflater.inflate(R.layout.list_item, parent, false);
        }

        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView textTITLE = (TextView) rowView.findViewById(R.id.textView_title);
        TextView textURL = (TextView) rowView.findViewById(R.id.textView_url);
        TextView textDES = (TextView) rowView.findViewById(R.id.textView_des);

        imageView.setImageResource(imgid[position]);
        textTITLE.setText(itemTITLE[position]);
        textURL.setText(itemURL[position]);
        textDES.setText(itemDES[position]);
        return rowView;
    }
}
