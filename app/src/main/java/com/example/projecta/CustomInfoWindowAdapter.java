package com.example.projecta;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View infoWindow;
    private final Context context;
    private final HashMap<String, Bitmap> imageCache = new HashMap<>(); // Cache to store loaded images

    public CustomInfoWindowAdapter(Context context) {
        this.context = context;
        this.infoWindow = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        // Retrieve the Restaurant object associated with this marker
        Restaurant restaurant = (Restaurant) marker.getTag();

        if (restaurant != null) {
            TextView title = infoWindow.findViewById(R.id.info_window_title);
            title.setText(restaurant.getName());

            ImageView imageView = infoWindow.findViewById(R.id.info_window_image);

            // Check if the image is already cached
            if (imageCache.containsKey(restaurant.getBusinessId())) {
                imageView.setImageBitmap(imageCache.get(restaurant.getBusinessId()));
            } else {
                // Set a placeholder while loading
                imageView.setImageResource(R.drawable.ic_placeholder_image);

                // Load image asynchronously and cache it
                Glide.with(context)
                        .asBitmap()
                        .load(restaurant.getImageUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(20)))
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                // Cache the image
                                imageCache.put(restaurant.getBusinessId(), resource);
                                imageView.setImageBitmap(resource);

                                // Check if the marker's info window is still open, then refresh
                                if (marker.isInfoWindowShown()) {
                                    marker.hideInfoWindow();
                                    marker.showInfoWindow();
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                imageView.setImageDrawable(placeholder);
                            }
                        });
            }
        }

        return infoWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        // Returning null here since we are using getInfoWindow for the full customization
        return null;
    }
}
