package com.zimmster.timmycam;

import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Kyle on 2015-07-12.
 */
public class MessageListener extends WearableListenerService {

    public static final String ACTION_UPDATE_IMAGE = "com.zimmster.timmycam.ACTION_UPDATE_IMAGE";
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        //When any data changes, broadcast that the image has been updated.
        //MainActivity listens for this event

        Intent intent = new Intent(ACTION_UPDATE_IMAGE);
        sendBroadcast(intent);
    }
}
