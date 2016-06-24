package de.baumann.weather.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.widget.ImageView;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.HttpsURLConnection;

import info.guardianproject.netcipher.NetCipher;

/**
 * Task that can be used to download images from URLs and store them in storage
 * Created by Gregor Santner (gsantner) on 24.03.16.
 */

public class ImageDownloadTask extends AsyncTask<String, Void, Bitmap> {
    private final ImageView imageView;
    private final String savePath;

    /**
     * Download image from URL
     *
     * @param savePath  Save image to file (null = don't save)
     */
    public ImageDownloadTask(@Nullable String savePath) {
        this.imageView = null;
        this.savePath = savePath;
    }

    protected Bitmap doInBackground(String... urls) {
        String url = urls[0];
        Bitmap bitmap = null;
        FileOutputStream out = null;
        InputStream inStream;
        HttpsURLConnection connection;
        try {
            connection = NetCipher.getHttpsURLConnection(url);
            inStream = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inStream);

            // Save to file if not null
            if (savePath != null) {
                out = new FileOutputStream(savePath);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            try {
                inStream.close();
            } catch (IOException e) {/*Nothing*/}

            connection.disconnect();

        } catch (Exception ignored) {

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {
            }
        }
        return bitmap;
    }

    protected void onPostExecute(Bitmap result) {
        // Display on imageview if not null
        if (imageView != null) {
            imageView.setImageBitmap(result);
        }
    }
}