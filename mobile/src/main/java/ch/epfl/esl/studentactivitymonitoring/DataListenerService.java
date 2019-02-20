package ch.epfl.esl.studentactivitymonitoring;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import ch.epfl.esl.commons.ID;
import ch.epfl.esl.commons.MyDatabase;
import ch.epfl.esl.commons.TRIMP;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Lara on 3/7/2018.
 */

public class DataListenerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    // Tag for Logcat
    private static final String TAG = "DataListenerService";

    MyDatabase database;

    private boolean mResolvingError = false;
    public static float[] heartRateArray;
    public static float[] heartRateArrayWorkout;
    public static long[] timestampArray;
    public static ArrayList<Integer> accuracyArrayList = new ArrayList<Integer>();
    public static long workoutDurationInMilliseconds;

    // Member for the Wear API handle
    GoogleApiClient mGoogleApiClient;

    Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Created");
        // Start the Wear API connection
        context = this;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        database = MyDatabase.getDatabase(context);
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // Data on the Wear API channel has changed
        Log.v(TAG, "Data received");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                switch (path) { //Path specifies the type of sensor data coming from the watch
                    case (ID.WATCH_DATA_PATH): //workout data
                        Log.v(TAG,"Received data from watch");
                        heartRateArrayWorkout = dataMap.getFloatArray(ID.heartRateArrayID);
                        workoutDurationInMilliseconds = dataMap.getLong(ID.workoutDurationID);
                        Log.v(TAG,"Workout duration: " + workoutDurationInMilliseconds);
                        TRIMP trimp = new TRIMP(heartRateArrayWorkout, context);
                        GetDataFromDatabaseForWorkoutData gdfdfwd = new GetDataFromDatabaseForWorkoutData();
                        gdfdfwd.execute();
                        break;
                    case (ID.STOP_ORTHO_PATH): //orthostatic test
                        Log.v(TAG,"Received HR data from ortho test from watch");
                        sendDataToFirebase(dataMap);
                        break;
                    default:
                        Log.v(TAG, "data received from unrecognized path: " + path);
                        break;
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.v(TAG, "DataItem Deleted: " + event.getDataItem().toString());
            }
        }

    }

    private void sendDataToFirebase(DataMap dataMap) {
        heartRateArray = dataMap.getFloatArray(ID.heartRateArrayID);
        timestampArray = dataMap.getLongArray(ID.timeStampArrayID);
        accuracyArrayList = dataMap.getIntegerArrayList(ID.accuracyArrayID);
        GetDataFromDatabase gtfd = new GetDataFromDatabase();
        gtfd.execute();
    }

    class GetDataFromDatabase extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            Integer userID =
                    database.userDataInterface().getLastValue();
            return userID;
        }

        @Override
        protected void onPostExecute(Integer userID) {
            Log.v("Room","Getting userID to Room: " + userID);
            ArrayList<Float> hrArrayList = new ArrayList<Float>();
            for (float hr: heartRateArray) {
                hrArrayList.add(hr);
            }
            ArrayList<Long> tsArrayList = new ArrayList<Long>();
            for (long ts: timestampArray) {
                tsArrayList.add(ts);
            }
            sendBeltDataToFirebase(hrArrayList,tsArrayList,userID);
        }
    }

    class GetDataFromDatabaseForWorkoutData extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            Integer userID =
                    database.userDataInterface().getLastValue();
            return userID;
        }
        // Maybe not necessary?

        @Override
        protected void onPostExecute(Integer userID) {
            Log.v("Room","Getting userID to Room: " + userID);
            ArrayList<Float> hrArrayListWorkout = new ArrayList<Float>();

            for (float hr: heartRateArrayWorkout) {
                hrArrayListWorkout.add(hr);
            }


            sendWorkoutDataToFirebase(hrArrayListWorkout, workoutDurationInMilliseconds, userID);

        }
    }

    private void sendBeltDataToFirebase(final ArrayList<Float> hrArray, final ArrayList<Long> timestampArray, int mUserID) {

        FirebaseApp secondApp = FirebaseApp.getInstance("larasDatabase");
        FirebaseDatabase database = FirebaseDatabase.getInstance(secondApp);

        Calendar dataItemCal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String calendarString = dateFormat.format(dataItemCal.getTime());

        String userID = String.valueOf(mUserID);

        DatabaseReference usersRef = database.getReference().child("users").child(userID).child("HR_Watch").child(calendarString).push();
        Log.v(TAG, "Migraine sending to firebase: " + userID);
        usersRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutabieData) {
                mutabieData.child("heartRateArray").setValue(hrArray);
                mutabieData.child("timestamps").setValue(timestampArray);
                mutabieData.child("accuracies").setValue(accuracyArrayList);
                return Transaction.success(mutabieData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
            }
        });
    }

    public void sendWorkoutDataToFirebase(final ArrayList<Float> hrArrayListWorkout, final long workoutDurationInMilliseconds, int mUserID) {

        FirebaseApp secondApp = FirebaseApp.getInstance("larasDatabase");
        FirebaseDatabase database = FirebaseDatabase.getInstance(secondApp);


        Calendar dataItemCal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String calendarString = dateFormat.format(dataItemCal.getTime());

        String userID = String.valueOf(mUserID);

        DatabaseReference usersRef = database.getReference().child("users").child(userID).child("HR_Workout").child(calendarString).push();
        Log.v(TAG, "Workout data sent to Firebase: " + userID);
        usersRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                mutableData.child("heartRateWorkout").setValue(hrArrayListWorkout);
                mutableData.child("durationWorkout").setValue(workoutDurationInMilliseconds);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Log.v(TAG,"Workout data sent to firebase! Error? : " + databaseError);
            }
        });
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // A message has been received from the Wear API
        Log.v(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());
        Log.v(TAG, messageEvent.toString());
    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        // The Wear API has a changed ability
        Log.v(TAG, "onCapabilityChanged: " + capabilityInfo);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to the wear API is halted
        Log.v(TAG, "Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


    }

    @Override
    public boolean stopService(Intent service){
        // App is stopped, close the wear API connection
        if (!mResolvingError && (mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        return true;
    }
}
