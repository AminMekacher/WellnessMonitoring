package ch.epfl.esl.studentactivitymonitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.widget.Toast;

/**
 * Created by Lara on 3/11/2018.
 */

public class OnStopWorkoutReminder extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(500);
        Toast.makeText(context,"Remember to click 'stop workout' when your workout ends",Toast.LENGTH_LONG).show();
    }
}
