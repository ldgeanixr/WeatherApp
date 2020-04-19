package com.example.googlemapapi;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagesService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseService";

    public FirebaseMessagesService() {
        super();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i(TAG, "Firebase Message Received");

        if(remoteMessage.getNotification()!=null){
            Log.d(TAG, "Message: " + remoteMessage
                    .getNotification()
                    .getBody());

            String name = remoteMessage.getNotification().getTitle();

            String message = remoteMessage.getNotification().getBody();
            Log.d(TAG, "onMessageReceived: " + message);

            Intent intent = new Intent("my.action.fbservice");
            intent.putExtra("city", name);
            intent.putExtra("msg", message);

            sendBroadcast(intent);
        }


    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
    }

}

