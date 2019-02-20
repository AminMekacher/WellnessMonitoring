package ch.epfl.esl.studentactivitymonitoring;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ch.epfl.esl.commons.ID;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Locale;

/**
 * Created by Lara on 3/13/2018.
 * Class to receive and process Firebase push notifications sent from be.care
 * Translates the messages that are in French
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private final static String TAG = "MyFirebaseMessagingServ";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteMessage.Notification remoteMessageNotification = remoteMessage.getNotification();

        String title; //Notification title
        String message; //Notification message
        if (remoteMessageNotification != null) {
            title = remoteMessageNotification.getTitle();
            message = remoteMessageNotification.getBody();
            Log.v(TAG,"Title: " + title + " Message: " + message);
        } else {
            title = getString(R.string.app_name);
            message = "No message found";
        }

        //Get the phone's language and change notification language accordingly
        String language = Locale.getDefault().getDisplayLanguage();
        Log.v(TAG,"Language: " + language);
        if (language.equals("français")) {
            //do nothing, notifications are in French by default
        } else {
            title = getString(R.string.test_results);
            String[] words = message.split(" ");
            if (words[0].equals("Pas")) {
                message = getString(R.string.notification_no_fatigue);
            } else {
                message = getString(R.string.notification_fatigue);
            }
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, ID.FIREBASE_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_favorite_black_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                R.mipmap.ic_launcher_round))
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(new long[] { 1000, 1000})
                        .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(ID.FIREBASE_CHANNEL_ID,
                    "Firebase notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(ID.beCareNotificationID, notificationBuilder.build());
    }
}
