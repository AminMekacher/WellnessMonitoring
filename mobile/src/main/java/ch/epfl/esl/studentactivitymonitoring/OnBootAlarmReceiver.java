package ch.epfl.esl.studentactivitymonitoring;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ch.epfl.esl.commons.ID;
import ch.epfl.esl.commons.MyDatabase;

import java.util.Calendar;
import java.util.List;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by orlandic on 02/03/2018.
 * Reset alarms when user reboots device
 */

public class OnBootAlarmReceiver extends BroadcastReceiver {
    String TAG = "OnBootAlarmReceiver";

    Context mContext;

    //Default alarm time
    int alarmMinute = 0;
    int alarmHour = 9;

    //Database reference and Firebase listener to get questions
    private DatabaseReference databaseRef;
    FirebaseApp secondApp;
    private MyFirebaseProfileListener mFirebaseProfileListener;

    int mUserID = 0;
    MyDatabase database;

    @Override
    public void onReceive(Context context, Intent intent) {

        mContext = context;
        database = MyDatabase.getDatabase(context);
        GetDataFromDatabase gdfd = new GetDataFromDatabase();
        gdfd.execute();
        prepFirebase(context);

    }

    //Prepare firebase database instance
    private void prepFirebase(Context context) {
        boolean hasBeenInitialized = false;
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps(context);
        for(FirebaseApp app : firebaseApps){
            if(app.getName().equals("larasDatabase")){
                hasBeenInitialized=true;
            }
        }

        if (!hasBeenInitialized) {
            //Configure your firebase database
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:215861426391:android:0e834ce1b72faf88")
                    .setApiKey("AIzaSyCoWM5liO2dqxgLHQC9v3vnRqEzSnEEZ2s")
                    .setDatabaseUrl("https://studentactivitymontoring.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(context, options, "larasDatabase");
        }
    }

    //Get user ID from Room
    class GetDataFromDatabase extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            Integer userID =
                    database.userDataInterface().getLastValue();
            return userID;
        }

        @Override
        protected void onPostExecute(Integer userID) {
            mUserID = userID;
            Log.v("Room","Getting userID to Room: " + userID);
            secondApp = FirebaseApp.getInstance("larasDatabase");
            Log.v(TAG, "Preparing firebase");
            //Get user's preferred alarm times from Firebase
            String path = "users/" + mUserID + "/alarmTimes";
            databaseRef = FirebaseDatabase.getInstance(secondApp).getReference(path);
            Log.v(TAG, "Databaseref: " + databaseRef.toString());
            mFirebaseProfileListener = new MyFirebaseProfileListener();
            databaseRef.addValueEventListener(mFirebaseProfileListener);
        }
    }

    public long getNextDayOccurrenceInMillis(int dayOfWeek, int hourOfDay, int minuteOfHour) {
        Calendar cal = Calendar.getInstance();
        int diff = dayOfWeek - cal.get(Calendar.DAY_OF_WEEK);
        if (diff < 0) {
            diff += 7;
        }
        cal.add(Calendar.DAY_OF_WEEK, diff);
        cal.set(Calendar.HOUR_OF_DAY,hourOfDay);
        cal.set(Calendar.MINUTE, minuteOfHour);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Calendar now = Calendar.getInstance();
        if (diff == 0 && cal.getTimeInMillis() < now.getTimeInMillis()) {
            diff += 7;
            cal.add(Calendar.DAY_OF_WEEK,diff);
        }
        return cal.getTimeInMillis();
    }

    private void scheduleNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

//        //Set up alarm intent
        Intent intentTuesday = new Intent(context, TestReminderAlarmReceiver.class);
        intentTuesday.putExtra("ID",ID.tuesdayNotificationID);
        intentTuesday.setAction(ID.testReminderAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ID.tuesdayNotificationID,intentTuesday,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentThursday = new Intent(context, TestReminderAlarmReceiver.class);
        intentThursday.putExtra("ID", ID.thursdayNotificationRepeatID);
        intentThursday.setAction(ID.testReminderAction);
        PendingIntent pendingIntentThursday = PendingIntent.getBroadcast(context, ID.thursdayNotificationRepeatID,intentThursday,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentFriday = new Intent(context, TestReminderAlarmReceiver.class);
        intentFriday.putExtra("ID", ID.fridayNotificationTuesdayID);
        intentFriday.setAction(ID.testReminderAction);
        PendingIntent pendingIntentFriday = PendingIntent.getBroadcast(context, ID.fridayNotificationTuesdayID,intentFriday,PendingIntent.FLAG_UPDATE_CURRENT);


        long repeatTime = AlarmManager.INTERVAL_DAY*7; //repetition frequency of the alarm

        //Tuesday alarm
        long tuesday = getNextDayOccurrenceInMillis(Calendar.TUESDAY, alarmHour, alarmMinute);

        //Thursday alarm
        long thursday = getNextDayOccurrenceInMillis(Calendar.THURSDAY, alarmHour,  alarmMinute);

        //Friday alarm
        long friday = getNextDayOccurrenceInMillis(Calendar.FRIDAY, alarmHour, alarmMinute);

        long now = Calendar.getInstance().getTimeInMillis();

        Log.v(TAG,"Setting alarm for: " + tuesday);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, tuesday, repeatTime, pendingIntent);
        Log.v(TAG,"Setting alarm for: " + thursday);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, thursday, repeatTime, pendingIntentThursday);
        Log.v(TAG,"Setting alarm for: " + friday);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, friday, repeatTime, pendingIntentFriday);
    }

    //Firebase listener to fetch alarm time and detect changes in the database
    class MyFirebaseProfileListener implements ValueEventListener {
        private static final String TAG = "Firebase listener";

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Log.v(TAG,"Querying times from firebase");
            //Process each type of data in its respective algorithm
            if (dataSnapshot.child("minute").getValue() != null) {
                alarmMinute = dataSnapshot.child("minute").getValue(Integer.class);
                alarmHour = dataSnapshot.child("hour").getValue(Integer.class);
            }
            scheduleNotifications(mContext);
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.v(TAG, databaseError.toString());
        }
    }
}
