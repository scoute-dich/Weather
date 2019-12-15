package de.baumann.weather.helper;


import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

import de.baumann.weather.R;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] itemTITLE;
    private final Integer[] imgid;

    @SuppressWarnings("UnusedParameters")
    public CustomListAdapter(Activity context, String[] itemTITLE, String[] itemURL, Integer[] imgid) {
        super(context, R.layout.list_item, itemTITLE);

        this.context=context;
        this.itemTITLE=itemTITLE;
        this.imgid=imgid;
    }

    @NonNull
    public View getView(int position, View rowView, @NonNull ViewGroup parent) {

        if (rowView == null) {
            LayoutInflater infInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = Objects.requireNonNull(infInflater).inflate(R.layout.list_item, parent, false);
        }

        ImageView imageView = rowView.findViewById(R.id.icon);
        TextView textTITLE = rowView.findViewById(R.id.textView_title);

        imageView.setImageResource(imgid[position]);
        textTITLE.setText(itemTITLE[position]);
        return rowView;
    }
}
