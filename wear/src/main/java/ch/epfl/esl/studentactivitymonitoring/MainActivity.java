package ch.epfl.esl.studentactivitymonitoring;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.autofill.Dataset;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ch.epfl.esl.commons.*;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class MainActivity extends WearableActivity implements SensorEventListener, CapabilityApi.CapabilityListener,
        MessageApi.MessageListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    public static final String TAG = "MainActivityWear";

    // Member for the Wear API handle
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;

    //UI
    private TextView mTextView;
    private Button startButton;
    private Button stopButton;
    private ImageView imageWorkout;
    private TextView heartRateText;
    private TextView timeTextView;
    private TextView heartRateDisplay;

    public Timer t; //Timer object to manage scheduled tasks
    private TextView timeText; //text displaying time elapsed in workout

    private ArrayList<Float> tempHeartRate = new ArrayList<Float>();
    private ArrayList<Integer> accuracyArray = new ArrayList<Integer>();

    private static long startWorkoutTime = 0;
    private static long endWorkoutTime = 0;

    Sensor heartRateSensor;
    public static final int INTERVAL = 1000 * 25;

    //Backend
    Handler mHandler;
    Runnable mHandlerTask;

    int secondsCounter;
    String timerString;
    int startTime;

    final MainActivity mActivity = this;

    public static WatchData watchData = new WatchData(); //this is the object to store data about the workout

    MyDatabase database;
    SensorManager sensorManager;

    //Global variables for data sampling
    Boolean stopWorkout;
    float currentHR;
    int secondsForArrayBuffering;


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        database = MyDatabase.getDatabase(getApplicationContext());

        mTextView = (TextView) findViewById(R.id.text);
        startButton = findViewById(R.id.startWorkoutButton);
        stopButton = findViewById(R.id.stopWorkoutButton);
        imageWorkout = (ImageView) findViewById(R.id.image_workout);
        heartRateDisplay = findViewById(R.id.hrDisplay);
        heartRateText = findViewById(R.id.hrTextView);

        timeText = findViewById(R.id.timeDisplay);
        timeTextView = findViewById(R.id.timeTextView);

        heartRateText.setVisibility(View.INVISIBLE);
        heartRateDisplay.setVisibility(View.INVISIBLE);
        timeText.setVisibility(View.INVISIBLE);
        timeTextView.setVisibility(View.INVISIBLE);

        secondsCounter = 0;

        //Enable sensor permissions
        //ask for user permission to access sensors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission("android.permission.BODY_SENSORS") ==
                PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]
                    {"android.permission.BODY_SENSORS"},0);
        }

        // Enables Always-on
        setAmbientEnabled();

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWorkout();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopWorkout();
            }
        });
        stopButton.setEnabled(false);

        //Sensor manager
        sensorManager = (SensorManager)
                getSystemService(MainActivity.SENSOR_SERVICE);

        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

//        FitnessOptions fitnessOptions =
//                FitnessOptions.builder().addDataType(DataType.TYPE_ACTIVITY_SAMPLES).build();
//        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
//            Log.v(TAG,"Google Account not yet connected");
//            GoogleSignIn.requestPermissions(
//                    this,
//                    REQUEST_OAUTH_REQUEST_CODE,
//                    GoogleSignIn.getLastSignedInAccount(this),
//                    fitnessOptions);
//        } else {
//            subscribe();
//        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.v(TAG,"OnActivityResult");
//        if (resultCode == Activity.RESULT_OK) {
//            Log.v(TAG,"Activity result: ok");
//            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
//                Log.v("TAG", "RequestCode");
//                subscribe();
//            }
//        } else {
//            Log.v(TAG, "OnActivityResult failed");
//        }
//    }
//
//    public void subscribe() {
//        Log.v(TAG,"Google Account already connected");
//        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
//                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.i(TAG, "Successfully subscribed!");
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.i(TAG, "There was a problem subscribing.");
//                        Log.v(TAG, e.toString());
//                    }
//                });
//    }


    //MARK :-- Bluetooth data sampling
    public TimerTask secondInterrupt;

    private void startWorkout(){
        //getOtherAppWorkoutInfo();
        int SensorSamplingRate = SensorManager.SENSOR_DELAY_FASTEST;
        sensorManager.registerListener((SensorEventListener) mActivity,heartRateSensor,SensorSamplingRate);

        stopWorkout = false;
        secondsForArrayBuffering = 0;

        startWorkoutTime = Calendar.getInstance().getTimeInMillis();
        heartRateText.setVisibility(View.VISIBLE);
        heartRateDisplay.setVisibility(View.VISIBLE);
        timeText.setVisibility(View.VISIBLE);
        imageWorkout.setVisibility(View.INVISIBLE);
        timeTextView.setVisibility(View.VISIBLE);
        watchData.clear();
        stopButton.setEnabled(true);
        startButton.setEnabled(false);
        startButton.setBackgroundResource(R.drawable.button_pressed);
        stopButton.setBackgroundResource(R.drawable.button_stop);
        mTextView.setText("Remember to press stop when you finish");
        //imageWorkout.setImageResource(R.mipmap.workout);
        setWorkoutEndReminderAlarms();

        //Update timer on watch face
        secondInterrupt = new TimerTask() {
            @Override
            public void run () {
                secondsCounter++;
                secondsForArrayBuffering++;
                int minutes = secondsCounter / 60;
                int hours = minutes / 60;
                if (hours > 0) {
                    minutes = minutes - 60*hours;
                }
                int seconds = secondsCounter % 60;
                if (minutes >= 10 && seconds >= 10) {
                    timerString = hours + ":" + minutes + ":" + seconds;
                } else if (minutes >= 10 && seconds < 10) {
                    timerString = hours + ":" + minutes + ":0" + seconds;
                } else if (minutes < 10 && seconds >= 10) {
                    timerString = hours + ":0" + minutes + ":" + seconds;
                }
                else {
                    timerString = hours + ":0" + minutes + ":0" + seconds;
                }
                Message message = uiHandler.obtainMessage(4);
                message.sendToTarget();

            }
        };

        t = new Timer();
        t.scheduleAtFixedRate(secondInterrupt,0,1000);
    }

    private void getOtherAppWorkoutInfo() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long start = cal.getTimeInMillis();
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_RATE_BPM, DataType.AGGREGATE_HEART_RATE_SUMMARY)
                .bucketByActivityType(1, TimeUnit.SECONDS)
                .setTimeRange(start, endTime, TimeUnit.MILLISECONDS)
                .build();
        Task<DataReadResponse> response = Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)).readData(readRequest);
        List<DataSet> dataSets = response.getResult().getDataSets();
        for (DataSet dataSet: dataSets) {
            Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
            DateFormat dateFormat = getTimeInstance();

            for (DataPoint dp : dataSet.getDataPoints()) {
                Log.i(TAG, "Data point:");
                Log.i(TAG, "\tType: " + dp.getDataType().getName());
                Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                for (Field field : dp.getDataType().getFields()) {
                    Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
                }
            }
        }
    }

    private void setWorkoutEndReminderAlarms() {
        Intent intentFriday = new Intent(getApplicationContext(), OnStopWorkoutReminder.class);
        intentFriday.setAction(ID.stopWorkoutReminderAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, ID.fridayNotificationTuesdayID,intentFriday,PendingIntent.FLAG_UPDATE_CURRENT);
        int repeatTime = 1000*60*20; //repetition frequency of the alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + repeatTime, repeatTime, pendingIntent);
    }

    private void stopAlarms() {
        Intent intentFriday = new Intent(getApplicationContext(), OnStopWorkoutReminder.class);
        intentFriday.setAction(ID.stopWorkoutReminderAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, ID.fridayNotificationTuesdayID,intentFriday,PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }


    //Handler used to communicate between UI thread and background tasks
    public Handler uiHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 4:
                    timeText.setText(timerString);
                    break;
            }
        }
    };

    private void stopWorkout(){
        stopWorkout = true;

        endWorkoutTime = Calendar.getInstance().getTimeInMillis();
        heartRateText.setVisibility(View.INVISIBLE);
        heartRateDisplay.setVisibility(View.INVISIBLE);
        timeText.setVisibility(View.INVISIBLE);
        imageWorkout.setVisibility(View.VISIBLE);
        timeTextView.setVisibility(View.INVISIBLE);
        SensorManager sensorManager = (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
        sensorManager.unregisterListener(mActivity,heartRateSensor);
        stopButton.setEnabled(false);
        stopButton.setBackgroundResource(R.drawable.button_pressed);
        startButton.setBackgroundResource(R.drawable.button_start);
        imageWorkout.setImageResource(R.mipmap.workout_begin);
        stopAlarms();
        //startButton.setEnabled(true); //do this later
        Log.v(TAG,"Watch data heart rate: " + watchData.heartRateArray.toString());
        tryToSendData(watchData);
        secondsCounter = 0;
        secondInterrupt.cancel();
        t.cancel();
        t.purge();
    }

    public void tryToSendData(final WatchData watchData){

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<com.google.android.gms.wearable.Node> nodes = getConnectedNodesResult.getNodes();
                // Do your stuff with connected nodes here
                Log.v(TAG,"Connected nodes: " + nodes.toString());
                if (nodes.size() > 0) {
                    resetUI();
                    sendDataToPhone(watchData);
                } else {
                    //try to upload data using wifi
                    tryToConnectToTheInternet(watchData);
                }
            }
        });
    }

    private void resetUI(){
        startButton.setEnabled(true);
        mTextView.setText("Press the button to start");
    }

    public void sendDataToPhone(final WatchData watchData) {

//        if (mGoogleApiClient.isConnected()) {
//            new SendMessageTask(ID.WATCH_DATA_PATH).execute();
//        }
        //TODO:add stuff to datamap to send it to the phone
        DataMap map = new DataMap(); //Datamap object where you stuff all data going to the phone
        PutDataMapRequest dataMap = PutDataMapRequest.create(ID.WATCH_DATA_PATH); //turn sensor data item into datamap
        dataMap.getDataMap().putDataMap(ID.WATCH_DATA_KEY, map);
        Log.v(TAG, "Sending array to app");
        float[] hrArray = new float[watchData.heartRateArray.size()];
        for (int i = 0; i < watchData.heartRateArray.size(); i++) {
            hrArray[i] = watchData.heartRateArray.get(i);
        }
        dataMap.getDataMap().putFloatArray(ID.heartRateArrayID, hrArray);
        long duration = endWorkoutTime - startWorkoutTime;
        dataMap.getDataMap().putLong(ID.workoutDurationID,duration);
//        long[] tsArray = new long[watchData.timestampArray.size()];
//        for (int i=0; i< watchData.timestampArray.size(); i++){
//            tsArray[i] = watchData.timestampArray.get(i);
//        }
//        dataMap.getDataMap().putLongArray(ID.timeStampArrayID,tsArray);
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        Log.v(TAG, "Sending watch data was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        SensorManager sensorManager = (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
        //Append data to heart rate sensor and array
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            Log.v(TAG,"Receiving heart rate: " + sensorEvent.values[0] + "with accuracy " + sensorEvent.accuracy);
            heartRateDisplay.setText(String.valueOf(sensorEvent.values[0]));
            int textColor = changeColorBasedOnAccuracy(sensorEvent.accuracy);
            heartRateDisplay.setTextColor(textColor);

            if (secondsForArrayBuffering < 10) {
                tempHeartRate.add(sensorEvent.values[0]);
                accuracyArray.add(sensorEvent.accuracy);
            } else if (!accuracyArray.isEmpty() && !tempHeartRate.isEmpty()) {
                secondsForArrayBuffering = 0;
                int maxAccuracy = Collections.max(accuracyArray);
                Log.v(TAG, "Max accuracy: " + maxAccuracy);
                float sum = 0;
                float size = 0;
                for (int i = 0; i < tempHeartRate.size(); i++) {
                    Log.v(TAG,"Sensor value: " + tempHeartRate.get(i) + " accuracy: " + accuracyArray.get(i));
                    if (accuracyArray.get(i) == maxAccuracy) {
                        sum = sum + tempHeartRate.get(i);
                        Log.v(TAG,"sum = " + sum);
                        size ++;
                    }
                }
                if (size != 0) {
                    float average = sum / size;
                    Log.v(TAG, "Calculated average HR: " + average);
                    watchData.heartRateArray.add(average);
                    long timestamp = Calendar.getInstance().getTimeInMillis();
                    watchData.timestampArray.add(timestamp);
                }
                accuracyArray.clear();
                tempHeartRate.clear();
            } else {
                secondsForArrayBuffering = 0;
            }
            if (stopWorkout) {
                sensorManager.unregisterListener(mActivity, heartRateSensor);
            }
        }
    }

    private int changeColorBasedOnAccuracy(int accuracy) {
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_NO_CONTACT:
                return Color.WHITE;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                return Color.RED;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return Color.MAGENTA;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return Color.CYAN;
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return Color.GREEN;
            default:
                return Color.GRAY;
        }
    }

    private void tryToConnectToTheInternet(WatchData watchData) {
        //TODO: check if internet is connected and try to send data
        bufferData(watchData);
    }

    private void tryToUploadData(WatchData watchData) {
        //TODO:log in and get token
        float[] hrArray = new float[watchData.heartRateArray.size()];
        for (int i = 0; i < watchData.heartRateArray.size(); i++) {
            hrArray[i] = watchData.heartRateArray.get(i);
        }
        TRIMP trimp = new TRIMP(hrArray, getApplicationContext());

    }

    //Store data to SQLite if connection isnt established
    private void bufferData(WatchData watchData) {
        StoreToDatabase stdb = new StoreToDatabase();
        stdb.execute(watchData.heartRateArray);
        resetUI();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Connection to the wear API
        Log.v(TAG, "Google API Client was connected");
        mResolvingError = false;
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addListener(
                mGoogleApiClient, this, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to the wear API is halted
        Log.v(TAG, "Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Connection to the wear API is halted
        Log.v(TAG, "Connection to Google API client failed");
        mResolvingError = true;
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }

    //If phone is not connected, cache data in Room
    class StoreToDatabase extends AsyncTask<ArrayList<Float>, Void, ArrayList<Float>> {

        @Override
        protected ArrayList<Float> doInBackground(ArrayList<Float>... watchHR) {
            Log.v(TAG,"Watch HR: " + watchHR[0].toString());
            HeartRateData heartRateData = new HeartRateData();
            heartRateData.timestamp = System.nanoTime();
            heartRateData.value = watchHR[0];
            long duration = endWorkoutTime - startWorkoutTime;
            heartRateData.workoutDuration = duration;
            // adding the measurement into the database
            database.sensorDataDao().insertSensorData(heartRateData);
            // Retrieving the last measurement
            List<HeartRateData> hr_data =
                    database.sensorDataDao().getLastValues();
            Log.v(TAG,"# of data lists in room: " + hr_data.size());
            // Merge two lists for returning all at once
            Log.v(TAG, "Last workout length: " + hr_data.get(hr_data.size() - 1).workoutDuration);
            ArrayList<Float> last_data = hr_data.get(hr_data.size() - 1).value;
            return last_data;
        }

        @Override
        protected void onPostExecute(ArrayList<Float> last_data) {
            Log.v(TAG,"Saving data to Room: " + last_data.toString());
        }
    }

    public class SendMessageTask extends AsyncTask<Void, Void, Void> {
        // Asynchronous background task to send a message through the Wear API
        private final String message;

        SendMessageTask(String message) {
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... args) {
            // doInBackground is the function executed when running the AsyncTask
            Collection<String> nodes = getNodes();
            Log.v(TAG, "Sending '" + message + "' to all " + nodes.size() + " connected nodes");
            for (String node : nodes) {
                sendMessage(message, node);
            }
            return null;
        }

        private void sendMessage(final String message, String node) {
            // Convenience function to send a message through the Wear API
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, node, message, new byte[0]).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Failed to send message " + message + " with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        }
    }

    private Collection<String> getNodes() {
        // Lists all the nodes (devices) connected to the Wear API
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (com.google.android.gms.wearable.Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }


}

