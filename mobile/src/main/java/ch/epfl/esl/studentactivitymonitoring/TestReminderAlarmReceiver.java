package ch.epfl.esl.studentactivitymonitoring;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import ch.epfl.esl.commons.ID;

/**
 * Created by orlandic on 02/03/2018.
 * Code runs when alarm manager sets it to
 * Displays notifications for users to complete the orthostatic test
 */

public class TestReminderAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();
            //Display notification
        int testID = intent.getIntExtra("ID",1);
        Log.v("TestReminder","Running alarm with ID: " + testID);
            Intent selectBTIntent = new Intent(context, LoginActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, selectBTIntent, 0);
            String message = context.getString(R.string.notif_text);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, ID.TEST_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_favorite_black_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.mipmap.ic_launcher_round))
                    .setContentTitle(context.getString(R.string.notif_title))
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(message))
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVibrate(new long[] { 500, 500});

            //NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Configure for Android Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(ID.TEST_CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
        }

            mNotificationManager.notify(testID, mBuilder.build());
        wl.release();


    }

    public boolean checkIfTestWasDoneToday() {
        //Connect to server and check if the user completed the orthostatic test
        //TODO: actual authentication against be.care server
        return ID.wasTestDoneToday;
    }
}
