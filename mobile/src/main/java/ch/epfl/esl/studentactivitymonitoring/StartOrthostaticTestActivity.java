package ch.epfl.esl.studentactivitymonitoring;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import ch.epfl.esl.commons.BeCare;
import ch.epfl.esl.commons.Constants;
import ch.epfl.esl.commons.DataToSend;
import ch.epfl.esl.commons.ID;
import ch.epfl.esl.commons.MyDatabase;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;

/**
 * Created by Lara on 2/20/2018.
 * Perform orthostatic test
 * Update UI with messages to user
 * Sample HR and RR data coming from the belt
 */

public class StartOrthostaticTestActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "StartOrthoTestActivity";

    public static String bleDeviceName;
    public static String bleDeviceAddress;

    public static BluetoothLeService mBluetoothLeService;
    boolean mConnected;
    SharedPreferences sharedPref;

    public Timer t = new Timer(); //Timer object to manage scheduled tasks
    public static Vibrator vibrator;
    private static Context context;

    private static RequestQueue MyRequestQueue;

    // Member for the Wear API handle
    GoogleApiClient mGoogleApiClient;

    //Start and end dates of the parts of the test
    String startSupine;
    String endSupine;
    String startStanding;
    String endStanding;

    //Arrays of HR, RR, and timestamp values
    private static ArrayList<Float> bluetoothHRStandingArray = new ArrayList<Float>();  //array of all HR values from belt during standing
    private static ArrayList<Integer> bluetoothRRStandingArray = new ArrayList<Integer>();
    private static ArrayList<Float> bluetoothHRSupineArray = new ArrayList<Float>(); //HR values while lying down
    private static ArrayList<Integer> bluetoothRRSupineArray = new ArrayList<Integer>();
    private static ArrayList<Long> timeStampsStanding = new ArrayList<Long>();
    private static ArrayList<Long> timeStampsSupine = new ArrayList<Long>();

    private static String testType; //type of ortho test (long, short)
    private static boolean standingData = false;

    private static String timerString; //string showing the elapsed time of the test
    private static int currentHR = 0; //HR data to be sampled
    private static int currentRR = 0;
    private static int previousRR = 0;
    private static int secondLastRR = 0;
    private static int secondsCounter = 0; //how many seconds have gone by in the test
    private static int stateCounter = 0;
    private static boolean beltErrorInProgress = false;
    private static boolean beltErrorFinished = false;

    BluetoothLeService bluetoothLeService;

    //time constants
    private static final int samplingPeriod = 1000; //BLE data sampling frequency in milliseconds
    private static final int preparationTime = 10*1000; //time given to user to prep for the experiment
    private static final int lyingDownTime = 5*60*1000; //time spent lying down in millis
    private static final int standingUpTime = 5*60*1000; //time spent standing up in millis
    private static final int vibrationTime = 1000; //Time that the vibrator vibrates in milliseconds



    Button startTestButton;
    public  static  ImageView orthostaticImage;

    boolean testInProgress;
    private static int testState; //what part of the test is going on
    private static TextView orthoTextView;
    private static TextView testTimer;

    //user id, retrieved from database
    int mUserID = 0;
    MyDatabase database;

    DataToSend becareData = new DataToSend();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_ortho_activity);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        context = getApplicationContext();

        database = MyDatabase.getDatabase(context);

        //Get user id from database
        GetDataFromDatabase gdfd = new GetDataFromDatabase();
        gdfd.execute();

        bluetoothLeService = new BluetoothLeService();

        testInProgress = false;
        sharedPref = this.getSharedPreferences("strings", Context.MODE_PRIVATE);
        orthoTextView = findViewById(R.id.orthoTestDescription);

        becareData.clear();

        MyRequestQueue = Volley.newRequestQueue(this);

        //Get previous device name from last activity
        final Intent intent = getIntent();
        bleDeviceName = intent.getStringExtra("bleDeviceName");
        bleDeviceAddress = intent.getStringExtra("bleDeviceAddress");

        //Identify the type of orthostatic test being performed
        testType = intent.getStringExtra(Constants.OrthostatTestType.ID);
        if (testType.isEmpty()) {
            Calendar today = Calendar.getInstance();
            testType = getTypeOfTestBasedOnDay(today.get(Calendar.DAY_OF_WEEK));
        }

        //Launch the BLE connection service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        secondsCounter = 0;
        stateCounter = 0;


        startTestButton = findViewById(R.id.startTest);
        startTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               startButtonPress();
            }
        });

        orthostaticImage = (ImageView) findViewById(R.id.orthostaticImage);
        testTimer = findViewById(R.id.testTimer);
        testTimer.setVisibility(View.INVISIBLE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_help_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                String errorMesssage = getString(R.string.help_ortho_test);
                HelpFragment dialog = HelpFragment.newInstance(errorMesssage);
                dialog.show(getFragmentManager(), TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void startButtonPress() {
        if (mGoogleApiClient.isConnected()) {
            new SendMessageTask(ID.START_ORTHO_PATH).execute();
        }
        startTestButton.setEnabled(false);
        startTestButton.setBackgroundResource(R.drawable.pressed_button);
        testInProgress = true;
        //Notify the user to get in position and give them 10 seconds
        t.scheduleAtFixedRate(getReadyTask,0,preparationTime);

    }

    //Disable back button if test in progress
    @Override
    public void onBackPressed() {
        if (testInProgress) {

        } else {
            super.onBackPressed();
        }
    }

    private void updateSecondsCounter() {
        secondsCounter++;
        int minutes = secondsCounter / 60;
        int seconds = secondsCounter % 60;
        if (minutes >= 10 && seconds >= 10) {
            timerString = "Time: " + minutes + ":" + seconds;
        } else if (minutes >= 10 && seconds < 10) {
            timerString = "Time: " + minutes + ":0" + seconds;
        } else if (minutes < 10 && seconds >= 10) {
            timerString = "Time: 0" + minutes + ":" + seconds;
        }
        else {
            timerString = "Time: 0" + minutes + ":0" + seconds;
        }
    }

    //MARK :-- Bluetooth data sampling
    public final TimerTask bluetoothSamplingTask = new TimerTask() {
        @Override
        public void run () {
            Log.v(TAG, "Seconds counter: " + secondsCounter);
            //Sample current HR and RR
            secondLastRR = previousRR;
            previousRR = currentRR;
            currentHR = bluetoothLeService.currentHRValue;
            currentRR = bluetoothLeService.currentRRValue;

            //Update timer string every second

            Message message = uiHandler.obtainMessage(4);
            message.sendToTarget();
            Long timestamp = Calendar.getInstance().getTimeInMillis();
            if ((currentRR == previousRR && currentRR == secondLastRR)) {
                //pause timers and notify user that the belt is misworn
                Message message2 = uiHandler.obtainMessage(5);
                message2.sendToTarget();
                beltErrorInProgress = true;

            } else {
                if (beltErrorInProgress) {
                    beltErrorFinished = true;
                    beltErrorInProgress = false;
                }
                updateSecondsCounter();
                if (!standingData) {
                    Log.v(TAG, "Sampling HR: " + currentHR + "and RR: " + currentRR + "for second " + timestamp + " supine");
                    if (currentHR > 10 && currentHR < 400) { //extreme physiological limits to remove zeros
                        bluetoothHRSupineArray.add((float) currentHR);
                    }
                    if (currentRR > 200 && currentRR < 4000) {
                        bluetoothRRSupineArray.add(currentRR);
                    }
                    timeStampsSupine.add(timestamp);
                } else {
                    Log.v(TAG, "Sampling HR: " + currentHR + "and RR: " + currentRR + "for second " + timestamp + " standing");
                    if (currentHR > 10 && currentHR < 400) {
                        bluetoothHRStandingArray.add((float) currentHR);
                        //Log.v(TAG,"BT Standing array length for now: " + bluetoothHRStandingArray.size());
                    }
                    if (currentRR > 200 && currentRR < 4000) {
                        bluetoothRRStandingArray.add(currentRR);
                    }
                    timeStampsStanding.add(timestamp);
                }
            }
        }
    };

    //MARK :-- Orthostatic test UI updates

    //Get ready for the test
    public final TimerTask getReadyTask = new TimerTask() {
        @Override
        public void run() {
            Log.v(TAG,"Notified user to lie down");
            if (beltErrorFinished) {
                Message message = uiHandler.obtainMessage(uiMessages.GetReady);
                message.sendToTarget();
                beltErrorFinished = false;
            }
            if (stateCounter == 0) {
                vibrator.vibrate(vibrationTime);
                standingData = false;
                Message message = uiHandler.obtainMessage(uiMessages.GetReady);
                message.sendToTarget();
                stateCounter++;
            } else if (stateCounter >=1) {
                //cancel this task and start liedownTask and samplingTask
                stateCounter = 0;
                getReadyTask.cancel();
                t.scheduleAtFixedRate(lieDownTask,0,samplingPeriod);
                t.scheduleAtFixedRate(bluetoothSamplingTask,0,samplingPeriod);

                //Get start timestamp
                Calendar supine = Calendar.getInstance();
                TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(tz);
                startSupine = df.format(supine.getTime());
            }
        }
    };

    public final TimerTask lieDownTask = new TimerTask() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            Log.v(TAG,"Lying down for 5 minutes, stateCounter = " + stateCounter );
            if (beltErrorFinished) {
                Message message = uiHandler.obtainMessage(uiMessages.LieDown);
                message.sendToTarget();
                beltErrorFinished = false;
            }
            if (stateCounter == 0) {
                vibrator.vibrate(vibrationTime);
                Message message = uiHandler.obtainMessage(uiMessages.LieDown);
                message.sendToTarget();
                stateCounter++;
            }
            if ((secondsCounter >= (lyingDownTime/1000)) && testType.equals(Constants.OrthostatTestType.Whole)) { //if we do the whole test, move onto the standing test
                //cancel this task and start standing
                stateCounter = 0;
                standingData = true;
                lieDownTask.cancel();
                t.scheduleAtFixedRate(standUpTask,0,samplingPeriod);

                //Get end timestamp
                Calendar endSupineCal = Calendar.getInstance();
                TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(tz);
                endSupine = df.format(endSupineCal.getTime());
                startStanding = endSupine;

            } else if (secondsCounter >= (lyingDownTime/1000)) { //if we just do the supine test, finish the test
                lieDownTask.cancel();
                sendSupineToBecare();
                finishOrthostaticTest();
            }
        }
    };

    public final TimerTask standUpTask = new TimerTask() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            //Log.v(TAG,"Standing up for 5 minutes");
            if (beltErrorFinished) {
                Message message = uiHandler.obtainMessage(uiMessages.StandUp);
                message.sendToTarget();
                beltErrorFinished = false;
            }
            if (stateCounter == 0) {
                vibrator.vibrate(vibrationTime);
                Message message = uiHandler.obtainMessage(uiMessages.StandUp);
                message.sendToTarget();
                stateCounter++;
            }
            if (secondsCounter >= (lyingDownTime + standingUpTime)/1000) {

                //Get end timestamp
                Calendar endStandingCal = Calendar.getInstance();
                TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(tz);
                endStanding = df.format(endStandingCal.getTime());

                //cancel this task and the BT sampling
                standUpTask.cancel();
                finishOrthostaticTest();
                sendStandingToBecare();
                //startQuestionnaire();
                startGraph();
            }
        }
    };

    //Set up supine data in beCareData object
    private void sendSupineToBecare() {
        Float sum = (float) 0;
        for (float i : bluetoothHRSupineArray) {
            sum += i;
        }
        Calendar endSupineCal = Calendar.getInstance();
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        endSupine = df.format(endSupineCal.getTime());
        ArrayList<Long> deltaTimestampsSupine = generateTimestampDeltas(timeStampsSupine);
        Float average = sum/bluetoothHRSupineArray.size();


        becareData.setSupine(BeCare.SessionType.energy,startSupine,startSupine,endSupine,bluetoothHRSupineArray,bluetoothRRSupineArray,
                deltaTimestampsSupine,String.valueOf(average));
        sendSupineDataToBeCare(becareData);

    }

    //If short ortho test is going on, set up the supine data
    private void sendSupineDataToBeCare(DataToSend data){
        JSONObject jsonObject = data.createJSONObjectSupine();
        Log.v(TAG,"Data JSON object: " + jsonObject.toString());

        String url = BeCare.baseURL + "session/sendSession";

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Send session","Response: " + response.toString());
                        goToWelcomeActivity();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.v("Send session","Error: " + error.toString());
                        goToWelcomeActivity();

                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = sharedPref.getString("authorizationToken","");
                Log.v("Volley","Setting token to " + token);
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization",token);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyRequestQueue.add(jsObjRequest);
    }

    private void goToWelcomeActivity() {
        Toast.makeText(context,getString(R.string.data_upload_success),Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, WelcomeActivity.class);
        startActivity(intent);
    }

    //package data for full test into becareData object
    private void sendStandingToBecare() {
        Float sum = (float) 0;
        for (float i : bluetoothHRSupineArray) {
            sum += i;
        }
        Float average = sum/bluetoothHRSupineArray.size();

        Float sum2 = (float) 0;
        for (float i : bluetoothHRStandingArray) {
            sum2 += i;
        }
        Float average2 = sum2/bluetoothHRStandingArray.size();

        ArrayList<Long> deltaTimestampsSupine = generateTimestampDeltas(timeStampsSupine);
        ArrayList<Long> deltaTimestampsStanding = generateTimestampDeltas(timeStampsStanding);
        Log.v(TAG,"End standing: " + endStanding);

        becareData.setStanding(startSupine,startSupine,endSupine, startStanding, endStanding, bluetoothHRSupineArray,bluetoothRRSupineArray,
                bluetoothHRStandingArray, bluetoothRRStandingArray,deltaTimestampsSupine, deltaTimestampsStanding, String.valueOf(average), String.valueOf(average2));
        Log.v(TAG,"supine timestamps: " + timeStampsSupine.toString());

    }

    private void sendBeltDataToFirebase(final ArrayList<Float> hrArray, final ArrayList<Integer> RRArray, final ArrayList<Long> timestampArray) {

        FirebaseApp secondApp = FirebaseApp.getInstance("larasDatabase");
        FirebaseDatabase database = FirebaseDatabase.getInstance(secondApp);

        Calendar dataItemCal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String calendarString = dateFormat.format(dataItemCal.getTime());

        String userID = String.valueOf(mUserID);

        DatabaseReference usersRef = database.getReference().child("users").child(userID).child("HR_Belt").child(calendarString).push();
        Log.v(TAG, "Data sending to firebase: " + userID);
        usersRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutabieData) {
                mutabieData.child("heartRateArray").setValue(hrArray);
                mutabieData.child("timestamps").setValue(timestampArray);
                return Transaction.success(mutabieData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
            }
        });

        DatabaseReference usersRefRR = database.getReference().child("users").child(userID).child("RR_Belt").child(calendarString).push();
        usersRefRR.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                mutableData.child("RRArray").setValue(RRArray);
                mutableData.child("timestamps").setValue(timestampArray);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }

    //set timestamps in becare format
    private ArrayList<Long> generateTimestampDeltas(ArrayList<Long> timestampArray){
        ArrayList<Long> output = new ArrayList<Long>();
        if (!timestampArray.isEmpty()) {
            output.add(timestampArray.get(0));
            for (int i = 0; i < timestampArray.size() - 1; i++) {
                output.add((timestampArray.get(i + 1) - timestampArray.get(i)));
            }
        }
        return output;
    }



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void finishOrthostaticTest() {
        if (mGoogleApiClient.isConnected()) {
            new SendMessageTask(ID.STOP_ORTHO_PATH).execute();
        }
        vibrator.vibrate(vibrationTime);
        Message message = uiHandler.obtainMessage(uiMessages.Done);
        message.sendToTarget();
        stateCounter = 0;
        secondsCounter = 0;
        bluetoothSamplingTask.cancel();
        t.cancel();
        t.purge();
        mBluetoothLeService.disconnect();

        ArrayList<Float> totalHRArray = new ArrayList<Float>();
        totalHRArray.addAll(bluetoothHRSupineArray);
        totalHRArray.addAll(bluetoothHRStandingArray);

        ArrayList<Long> totalTimestampArray = new ArrayList<Long>();
        totalTimestampArray.addAll(timeStampsSupine);
        totalTimestampArray.addAll(timeStampsStanding);

        ArrayList<Integer> totalRRArray = new ArrayList<>();
        totalRRArray.addAll(bluetoothRRSupineArray);
        totalRRArray.addAll(bluetoothRRStandingArray);
        for (int RR: totalRRArray) {
            Log.v(TAG,"RR in array: " + RR);
        }

        sendBeltDataToFirebase(totalHRArray, totalRRArray, totalTimestampArray);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putInt("stateCounter", stateCounter);
        savedInstanceState.putInt("secondsCounter", secondsCounter);
        savedInstanceState.putBoolean("standingData",standingData);
        savedInstanceState.putBoolean("testInProgress",testInProgress);
        if (testInProgress) {
            //BitmapDrawable bmd = (BitmapDrawable) orthostaticImage.getDrawable();
            //savedInstanceState.putParcelable("image",bmd.getBitmap());
            savedInstanceState.putInt("testState", testState);
        }

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        stateCounter = savedInstanceState.getInt("stateCounter");
        secondsCounter = savedInstanceState.getInt("secondsCounter");
        standingData = savedInstanceState.getBoolean("standingData");
        testInProgress = savedInstanceState.getBoolean("testInProgress");
        if (testInProgress) {
            startTestButton.setEnabled(false);
            testTimer.setVisibility(View.VISIBLE);
//            orthostaticImage.setImageBitmap((Bitmap) savedInstanceState.getParcelable("image"));
            testState = savedInstanceState.getInt("testState");
            switch (testState) {
                case uiMessages.LieDown:
                    orthostaticImage.setImageResource(R.mipmap.lying_orthostatic);
                    orthoTextView.setText(context.getString(R.string.lie_down));
                    startTestButton.setBackgroundResource(R.drawable.pressed_button);
                    break;
                case uiMessages.StandUp:
                    orthostaticImage.setImageResource(R.mipmap.standing_orthostatic);
                    orthoTextView.setText(context.getString(R.string.stand_up));
                    startTestButton.setBackgroundResource(R.drawable.pressed_button);
                    break;
                default:
                    break;
            }
        }
    }


    private void startGraph() {
        final Intent intent = new Intent(this,GraphAfterTestActivity.class);
        intent.putExtra(Constants.OrthostatTestType.ID,testType);
        Log.v(TAG,"BT supine array length: " + bluetoothHRSupineArray.size());
        Log.v(TAG,"BT standing array length: " + bluetoothHRStandingArray.size());
        ArrayList<Integer> totalHRArray = new ArrayList<>();
        for (float hr: bluetoothHRSupineArray) {
            totalHRArray.add((int) hr);
        }
        for (float hr2: bluetoothHRStandingArray) {
            totalHRArray.add((int) hr2);
        }
        Log.v(TAG,"HR array sending to graph length: " + totalHRArray.size());
        intent.putIntegerArrayListExtra("hrArray",totalHRArray);
        startActivity(intent);
    }

    //Handler used to communicate between UI thread and background tasks
    public static Handler uiHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case uiMessages.GetReady:
                    testState = uiMessages.GetReady;
                    orthoTextView.setText(context.getString(R.string.get_ready));
                    Toast.makeText(context, context.getString(R.string.get_ready), Toast.LENGTH_LONG).show();
                    break;
                case uiMessages.LieDown:
                    testState = uiMessages.LieDown;
                    orthoTextView.setText(context.getString(R.string.lie_down));
                    Toast.makeText(context, context.getString(R.string.lie_down), Toast.LENGTH_LONG).show();
                    orthostaticImage.setImageResource(R.mipmap.lying_orthostatic);

                    testTimer.setVisibility(View.VISIBLE);
                    break;
                case uiMessages.StandUp:
                    testState = uiMessages.StandUp;
                    orthoTextView.setText(context.getString(R.string.stand_up));
                    Toast.makeText(context, context.getString(R.string.stand_up), Toast.LENGTH_LONG).show();
                    orthostaticImage.setImageResource(R.mipmap.standing_orthostatic);
                    break;
                case uiMessages.Done:
                    testState = uiMessages.Done;
                    Toast.makeText(context, context.getString(R.string.test_complete), Toast.LENGTH_LONG).show();
                    break;
                case 4:
                    testTimer.setText(timerString);
                    break;
                case 5:
                    orthoTextView.setText(context.getString(R.string.ortho_test_error));
                    vibrator.vibrate(500);
                    break;
            }
        }
    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v(TAG,"Google API connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG,"Google API connection suspended");
        Toast.makeText(context,getString(R.string.wear_not_connected),Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v(TAG,"Google API connection failed");
        Toast.makeText(context,getString(R.string.wear_not_connected),Toast.LENGTH_LONG).show();
    }

    static class uiMessages {
        public static final int GetReady = 0;
        public static final int LieDown = 1;
        public static final int StandUp = 2;
        public static final int Done = 3;
    }

    //MARK :-- Bluetooth Connection Management
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");

            } else {
                Log.v(TAG,"Bluetooth initialized");
            }
            // Automatically connects to the device upon successful start-up initialization.
            boolean result = mBluetoothLeService.connect(bleDeviceAddress);
            Log.v(TAG,"Connected to device? " + result);
            Log.v(TAG,"Device address: " + bleDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Log.v(TAG, "BLE Connected");
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Log.v(TAG, "BLE Disconnected");
                invalidateOptionsMenu();
                Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.v(TAG, "BLE Services Discovered");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.v(TAG, "BLE Data Available");
//                secondLastRR = previousRR;
//                previousRR = currentRR;
//                //TODO: fix this
//                currentHR = intent.getIntExtra(BluetoothLeService.EXTRA_DATA_HR,0);
//                currentRR = intent.getIntExtra(BluetoothLeService.EXTRA_DATA_RR,0);

            }
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            // Find Heart Rate service (0x180D)
            if (SampleGattAttributes.lookup(uuid, "unknown").equals("Heart Rate Service")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                // Loops through available Characteristics
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    // Find Heart rate measurement (0x2A37)
                    if (SampleGattAttributes.lookup(uuid, "unknown").equals("Heart Rate Measurement")) {
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private String getTypeOfTestBasedOnDay(int weekday) {
        String output;
        switch (weekday) {
            case (Calendar.MONDAY): //Monday: Questionnaire
                output = Constants.OrthostatTestType.Supine;
                break;
            case (Calendar.TUESDAY): //Tuesday: Whole Orthostatic test
                output = Constants.OrthostatTestType.Whole;
                break;
            case (Calendar.WEDNESDAY): //Wednesday: Questionnaire
                output = Constants.OrthostatTestType.Supine;
                break;
            case (Calendar.THURSDAY): //Thursday: Supine Orostatic test
                output = Constants.OrthostatTestType.Supine;
                break;
            case (Calendar.FRIDAY): //Friday: Questionnaire
                output = BeCare.SessionType.questionnaire;
                break;
            case (Calendar.SATURDAY): //Saturday: Supine Orthostatic test
                output = Constants.OrthostatTestType.Supine;
                break;
            case (Calendar.SUNDAY): //Sunday: Questionnaire
                output = Constants.OrthostatTestType.Supine;
                break;
            default:
                output = Constants.OrthostatTestType.Supine;
                Log.e(TAG,"Error: entered int is not a day of the week");
                break;
        }
        return output;
    }


    //MARK :-- App lifecycle
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(bleDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    //MARK --: sending messages from phone to wear
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

        }
    }


}
