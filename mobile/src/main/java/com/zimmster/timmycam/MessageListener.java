package com.zimmster.timmycam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Created by Kyle on 2015-07-12.
 */
public class MessageListener extends WearableListenerService {
    public static final String IMAGE_URL = "http://timmycam.conestogac.on.ca/IMAGE.JPG";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        //If the message we received was to update the image, then do it
        if (messageEvent.getPath().equalsIgnoreCase("/image")) {
            updateImageAsset();
        }
    }

    private void updateImageAsset() {
        Bitmap bitmap;

        //Try to get the image from the web
        try {
            URL url = new URL(IMAGE_URL);
            bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch (IOException ex) {
            //TODO: Send a message to the watch that getting the image failed
            return;
        }

        //Create a client to connect to the watch
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        //Assets are how Android Wear shares resources between devices, so create an asset from the bitmap
        //https://developer.android.com/training/wearables/data-layer/assets.html
        Asset asset = createAssetFromBitmap(bitmap);

        //Since we're not on the UI thread we can do this synchronously
        googleApiClient.blockingConnect();

        //Put the asset in a data sending request
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/timmyimage");
        dataMapRequest.getDataMap().putAsset("image", asset);

        //A timestamp forces the request to not be discarded as a duplicate
        dataMapRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

        //Send the request
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, request).await();

        //Now that we're done, disconnect from the watch
        googleApiClient.disconnect();
    }

    //Shamelessly taken from Android Docs: https://developer.android.com/training/wearables/data-layer/assets.html#TransferAsset
    //Note: Since it's a photo I'm using JPEG instead of PNG.
    //TODO: Can I use less quality to reduce filesize?
    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}
