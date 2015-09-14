package com.zimmster.timmycam;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks {

    private final String IMAGE_ASSET_PATH = "/image";
    private final int TIMEOUT_MS = 8000;

    private ImageView image;
    private ProgressBar progress;
    private TextView errorMessage;

    private GoogleApiClient googleApiClient;
    private BroadcastReceiver updateImageBroadcastReceiver;

    private Timer timeOutTimer;
    private TimerTask timeOutTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image = (ImageView)findViewById(R.id.image);
        progress = (ProgressBar)findViewById(R.id.progress);
        errorMessage = (TextView)findViewById(R.id.network_error);

        //Add this Activity as a click listener to both the image and the error message (causes refresh)
        image.setOnClickListener(this);
        errorMessage.setOnClickListener(this);

        //Store a Google API Client with the Wearable api enabled.
        //Note the callback which lets us know when we're actually connected.
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        //When this broadcast receiver receives a message, update the image view to match the latest asset download by the phone
        updateImageBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateImage();
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Register the image receiver to start listening
        registerReceiver(updateImageBroadcastReceiver, new IntentFilter(MessageListener.ACTION_UPDATE_IMAGE));

        //Asynchronously connect to the phone
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //Once we've connected to the phone, request an initial image
        requestImageUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Intentionally do nothing. This is required by GoogleApiClient.ConnectionCallbacks
    }

    @Override
    protected void onStop() {
        //Stop the connection to the phone
        googleApiClient.disconnect();

        //Stop listening for image updates
        unregisterReceiver(updateImageBroadcastReceiver);

        super.onStop();
    }

    @Override
    public void onClick(View view) {
        requestImageUpdate();
    }

    private void requestImageUpdate() {
        //Hide any errors, show loading animation and dim the image
        errorMessage.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        image.setAlpha(0.5f);

        //Since the image comes from the phone, we won't necessarily know if something went wrong
        //so if we don't hear back from the phone after some time, assume it was a failure.

        //Cancel any existing timeout task
        if (timeOutTask != null) {
            timeOutTask.cancel();
        }

        //Cancel the timer
        if (timeOutTimer != null) {
            timeOutTimer.cancel();
        }

        //Create a new timeout timer
        timeOutTimer = new Timer();

        //Create a new timeout task
        timeOutTask = new TimerTask() {
            @Override
            public void run() {

                //In order to change the UI, this MUST be run on the UI thread
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //If we've hit this, it's been long enough and we haven't heard back from the phone, so assume failure

                        //Hide the image, hide the loader and show the error message.
                        image.setVisibility(View.GONE);
                        progress.setVisibility(View.GONE);
                        errorMessage.setVisibility(View.VISIBLE);

                        //Android Docs recommends calling cancel or the app may leak resources: http://developer.android.com/reference/java/util/Timer.html
                        timeOutTask.cancel();
                        timeOutTimer.cancel();
                    }
                });

            }
        };

        //Schedule the timeout task with some delay
        timeOutTimer.schedule(timeOutTask, TIMEOUT_MS);

        //Create a new thread to send the send the message to the phone
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Get a list of all devices that the watch is connected to (in my case this is just my phone)
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                //Send the request to each device
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), IMAGE_ASSET_PATH, "".getBytes()).await();
                }
            }
        }).start();
    }

    private void updateImage() {
        //The update was a success, so stop the timeout timer
        timeOutTask.cancel();
        timeOutTimer.cancel();

        //Get the URI of the image. This will end up as wear:/timmyimage.
        Uri imageAssetUri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path("/timmyimage").build();

        //Unfade the image
        image.setAlpha(1f);

        try {
            //Async get the data items that match the asset URI
            Wearable.DataApi.getDataItems(googleApiClient, imageAssetUri).setResultCallback(new ResultCallback<DataItemBuffer>() {
                @Override
                public void onResult(DataItemBuffer result) {

                    //Check if we actually got a result
                    if (result != null && result.getCount() >= 1) {
                        //Grab the first (and only) data item
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(result.get(0));

                        //Convert the dataitem to an Asset
                        Asset asset = dataMapItem.getDataMap().getAsset("image");

                        //Async get the file descriptor for the asset
                        Wearable.DataApi.getFdForAsset(googleApiClient, asset).setResultCallback(new ResultCallback<DataApi.GetFdForAssetResult>() {
                            @Override
                            public void onResult(DataApi.GetFdForAssetResult getFdForAssetResult) {
                                //Get an input stream of for the file
                                InputStream assetInputStream = getFdForAssetResult.getInputStream();

                                //Load the image as a bitmap
                                Bitmap bitmap = BitmapFactory.decodeStream(assetInputStream);

                                if (bitmap != null) {
                                    //It worked, so update image and show it
                                    image.setImageBitmap(bitmap);
                                    image.setVisibility(View.VISIBLE);
                                } else {
                                    //The asset data was not a bitmap, show an error message
                                    image.setVisibility(View.GONE);
                                    errorMessage.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    } else {
                        //No data item was found so show an error
                        image.setVisibility(View.GONE);
                        errorMessage.setVisibility(View.VISIBLE);
                    }

                    //Release the result to not leak resources
                    result.release();
                }
            });
        } catch (Exception ex ) {
            //Catch-all if anything went wrong, show an error.
            image.setVisibility(View.GONE);
            errorMessage.setVisibility(View.VISIBLE);
        }

        //Hide the loader now that we're all done
        progress.setVisibility(View.GONE);
    }
}