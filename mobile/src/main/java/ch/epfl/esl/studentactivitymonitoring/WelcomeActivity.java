package ch.epfl.esl.studentactivitymonitoring;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.service.autofill.Dataset;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.JsonArrayRequest;

import ch.epfl.esl.commons.ActivitySegment;
import ch.epfl.esl.commons.BeCare;
import ch.epfl.esl.commons.HeartRateDataPoint;
import ch.epfl.esl.commons.ID;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.PointsGraphSeries;

//import com.jjoe64.graphview.GraphView;
//import com.jjoe64.graphview.series.BarGraphSeries;
//import com.jjoe64.graphview.series.DataPoint;
//import com.jjoe64.graphview.series.DataPointInterface;
//import com.jjoe64.graphview.series.LineGraphSeries;
//import com.jjoe64.graphview.series.PointsGraphSeries;

import ch.epfl.esl.commons.Constants;
import ch.epfl.esl.commons.MyDatabase;
import ch.epfl.esl.commons.TRIMP;
import ch.epfl.esl.commons.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;


/**
 * Created by Lara on 2/19/2018.
 * Welcome activity used to start ortho test
 * User sets alarm time, which is saved on firebase
 * Schedules notifications
 */

public class WelcomeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    String TAG = "WelcomeActivity";

    private static RequestQueue MyRequestQueue;
    SharedPreferences sharedPref;
    public static Context context;

    LinearLayout welcomeLayout;
    ProgressBar progressBar;

    //Google Fit objects
    public static GoogleApiClient mClient;
    ArrayList<Integer> nonFitnessActivityValues = new ArrayList<>();
    ArrayList<HeartRateDataPoint> googleFitHeartRateArray = new ArrayList<>();
    ArrayList<ActivitySegment> googleFitActivitySegments = new ArrayList<>();
    long lastAnalyzedDateGoogleFit;
    private int hrDataCount;
    private int sampleDataCount;
    Integer hrDataMax;
    Integer samplesDataMax;



    //Minute and hour of test reminder notifications
    int alarmMinute = 0;
    int alarmHour = 9;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;

    //user id, retrieved from database
    public static int mUserID;
    MyDatabase database;

    //Database reference and Firebase listener to get questions
    private DatabaseReference databaseRef;
    FirebaseApp secondApp;
    private MyFirebaseProfileListener mFirebaseProfileListener;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        context = getApplicationContext();
        database = MyDatabase.getDatabase(context);

        welcomeLayout = findViewById(R.id.welcome_view);
        progressBar = findViewById(R.id.welcome_progress);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        //shared preferences
        sharedPref = this.getSharedPreferences("strings", Context.MODE_PRIVATE);

        MyRequestQueue = Volley.newRequestQueue(this);

        //Google Fit
        nonFitnessActivityValues.add(4);
        nonFitnessActivityValues.add(3);
        nonFitnessActivityValues.add(112);
        nonFitnessActivityValues.add(111);
        nonFitnessActivityValues.add(110);
        nonFitnessActivityValues.add(109);
        nonFitnessActivityValues.add(72);
        nonFitnessActivityValues.add(2);
        nonFitnessActivityValues.add(0);

        hrDataCount = 0;
        sampleDataCount = 0;

        //Ask for permission to get HR data from Google Fit
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission("android.permission.BODY_SENSORS") ==
                PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]
                    {"android.permission.BODY_SENSORS"},0);
        }

        Button startOrtho = findViewById(R.id.startOrthoTest);
        startOrtho.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress(true);
                checkHowManySessionsThereAre();
            }
        });

        Button startOrthoWhole = findViewById(R.id.startOrthoTestWhole);
        startOrthoWhole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress(true);
                startOrthoActivityWhole();
            }
        });

        showProgress(true);

        //Get user id from database
        GetDataFromDatabase gdfd = new GetDataFromDatabase();
        gdfd.execute();


    }

    @Override
    public void onPause() {
        super.onPause();
        if (mClient != null) {
            mClient.stopAutoManage(this);
            mClient.disconnect();
        }
    }

    private void prepGoogleFit() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            Log.v(TAG,"Google Account not yet connected");
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            subscribe();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG,"OnActivityResult");

        if (resultCode == Activity.RESULT_OK) {
            Log.v(TAG,"Activity result: ok");
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                Log.v("TAG", "RequestCode");
                subscribe();
            }
        }
        else {
            Log.v("GoogleFit", "Activity result failed: " + resultCode);
        }
    }

    public void subscribe() {
        Log.v(TAG,"Google Account already connected");
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.SENSORS_API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0,this)
                .build();

        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Successfully subscribed!");
                        showProgress(false);
                        //getDataFitData();
                        bundleServiceData();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "There was a problem subscribing.");
                        Log.v(TAG, e.toString());
                    }
                });
    }

    private void bundleServiceData(){

        Intent serviceIntent = new Intent(context, GoogleFitService.class);
        serviceIntent.putExtra("lastTime", lastAnalyzedDateGoogleFit);
        startService(serviceIntent);
        updateLastAnalyzedSampleTimeOnFirebase();
    }


    private void updateLastAnalyzedSampleTimeOnFirebase() {
        Calendar cal = Calendar.getInstance();
        databaseRef.child("lastAnalyzedFitTime").setValue(cal.getTimeInMillis());
    }



    @Override
    public void onStart() {
        super.onStart();
        prepFirebase();

    }

    private void prepFirebase(){
        boolean hasBeenInitialized = false;
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps(this);
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
            FirebaseApp.initializeApp(getApplicationContext(), options, "larasDatabase");
        }
    }

    private void  showProgress(final boolean show){
            // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
            // for very easy animations. If available, use these APIs to fade-in
            // the progress spinner.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

                //Hide login form and show loading animation if true
                welcomeLayout.setVisibility(show ? View.GONE : View.VISIBLE);
                welcomeLayout.animate().setDuration(shortAnimTime).alpha(
                        show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        welcomeLayout.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                progressBar.animate().setDuration(shortAnimTime).alpha(
                        show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
            } else {
                // The ViewPropertyAnimator APIs are not available, so simply show
                // and hide the relevant UI components.
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                welcomeLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            }
    }

    //start a short orthostatic test
    void startOrthoActivitySupine() {
        final Intent intent = new Intent(this, SelectBluetoothDevice.class);
        intent.putExtra(Constants.OrthostatTestType.ID,Constants.OrthostatTestType.Supine);
        startActivity(intent);
    }

    //start a whole ortho test
    void startOrthoActivityWhole() {
        showProgress(false);
        final Intent intent = new Intent(this, SelectBluetoothDevice.class);
        intent.putExtra(Constants.OrthostatTestType.ID,Constants.OrthostatTestType.Whole);
        startActivity(intent);
    }

    //start only questionnaire
    void startQuestionnaireActivity() {
        final Intent intent = new Intent(this, QuestionnaireActivity.class);
        intent.putExtra(Constants.OrthostatTestType.ID, BeCare.SessionType.questionnaire);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.set_alarms:
                showTimePickerDialog();
                return true;
            case R.id.previous_workouts:
                Intent history_intent = new Intent(this, HistoryActivity.class);
                startActivity(history_intent);
                return true;
            case R.id.help:
                String errorMessage = getString(R.string.help_welcome);
                HelpFragment dialog = HelpFragment.newInstance(errorMessage);
                dialog.show(getFragmentManager(), TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void showTimePickerDialog() {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), getString(R.string.time_picker));
    }

    //Schedule notifications reminding person to do the orthostatic test
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void scheduleNotifications() {
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

//        //Set up alarm intent
        Intent intent = new Intent(this, TestReminderAlarmReceiver.class);
        intent.putExtra("ID",ID.tuesdayNotificationID);
        intent.setAction(ID.testReminderAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, ID.tuesdayNotificationID,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentThursday = new Intent(this, TestReminderAlarmReceiver.class);
        intentThursday.putExtra("ID", ID.thursdayNotificationRepeatID);
        intentThursday.setAction(ID.testReminderAction);
        PendingIntent pendingIntentThursday = PendingIntent.getBroadcast(context, ID.thursdayNotificationRepeatID,intentThursday,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentFriday = new Intent(this, TestReminderAlarmReceiver.class);
        intentFriday.putExtra("ID", ID.fridayNotificationTuesdayID);
        intentFriday.setAction(ID.testReminderAction);
        PendingIntent pendingIntentFriday = PendingIntent.getBroadcast(context, ID.fridayNotificationTuesdayID,intentFriday,PendingIntent.FLAG_UPDATE_CURRENT);



        long repeatTime = AlarmManager.INTERVAL_DAY*7; //repetition frequency of the alarm

        //Get alarm times
        Log.v(TAG,"Firebase hour: " + alarmHour + " minute: " + alarmMinute);

        //Tuesday alarm
        long tuesday = getNextDayOccurrenceInMillis(Calendar.TUESDAY, alarmHour, alarmMinute);

        //Thursday alarm
        long thursday = getNextDayOccurrenceInMillis(Calendar.THURSDAY, alarmHour,  alarmMinute);

        //Friday alarm
        long friday = getNextDayOccurrenceInMillis(Calendar.FRIDAY, alarmHour, alarmMinute);

        alarmManager.cancel(pendingIntent);

            Log.v(TAG,"Setting alarm for: " + tuesday);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, tuesday, repeatTime, pendingIntent);
            Log.v(TAG,"Setting alarm for: " + thursday);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, thursday, repeatTime, pendingIntentThursday);
            Log.v(TAG,"Setting alarm for: " + friday);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, friday, repeatTime, pendingIntentFriday);

    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void processTimePickerResult(int hourOfDay, int minute) {
        //Save times to firebase

        databaseRef.child("alarmTimes").child("hour").setValue(hourOfDay);
        databaseRef.child("alarmTimes").child("minute").setValue(minute);

        // Convert time elements into strings.
        String hour_string = Integer.toString(hourOfDay);
        String minute_string = Integer.toString(minute);
        // Assign the concatenated strings to timeMessage.
        String timeMessage = (hour_string + ":" + minute_string);
        Toast.makeText(this, getString(R.string.time) + timeMessage, Toast.LENGTH_SHORT).show();
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

    boolean isDateValid(String date) {
        boolean out;
        String[] removeT = date.split("T");
        String[] split = removeT[0].split("-");
        int year = Integer.valueOf(split[0]);
        int month = Integer.valueOf(split[1]) - 1;
        int day = Integer.valueOf(split[2]);
        Log.v(TAG, "Year: " + year + " Month: " + month + " Day: " + day);
        Calendar birthday = Calendar.getInstance();
        birthday.set(Calendar.YEAR,year);
        birthday.set(Calendar.MONTH, month);
        birthday.set(Calendar.DAY_OF_MONTH, day);
        //Log.v(TAG,"Date object: " + birthday.toString());
        Calendar dataValidationDay = Calendar.getInstance();
        dataValidationDay.set(Calendar.YEAR, 2018);
        dataValidationDay.set(Calendar.MONTH, 4);
        dataValidationDay.set(Calendar.DAY_OF_MONTH, 18);
        DateFormat df = getDateInstance();
        //Log.v(TAG,"Validation date: " + df.format(new Date(dataValidationDay.getTimeInMillis())));
        if (dataValidationDay.getTimeInMillis() < birthday.getTimeInMillis()) {
            Log.v(TAG,"Date is after May 18");
            out = true;
        } else {
            out = false;
        }
        return out;
    }

    private void checkHowManySessionsThereAre(){

        String url = BeCare.baseURL + "session/sessions?offset=0&limit=50";

        JsonArrayRequest jsObjRequest = new JsonArrayRequest
                (Request.Method.GET, url, new JSONObject(), new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Send session","Response: " + response.toString());
                        int numberOfNonFatigueSessions = 0;
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject session = response.getJSONObject(i);
                                String sessionType = session.getString("sessionType");
                                String date = session.getString("begin");
                                boolean isTestAfterValidationDate = isDateValid(date);
                                if (sessionType.equals(BeCare.SessionType.questionnaire)) {
                                    JSONObject globalData = session.getJSONObject("globaldata");
                                    boolean fatigue = globalData.getBoolean("FATIGUE");
                                    if (!fatigue && isTestAfterValidationDate) {
                                        numberOfNonFatigueSessions++;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.v(TAG,"There are " + numberOfNonFatigueSessions + " sessions");
                        if (numberOfNonFatigueSessions < 3) {
                            startOrthoActivityWhole();
                            //startQuestionnaireActivity();

                        } else {
                            Calendar today = Calendar.getInstance();
                            startNextActivityBasedOnWeekday(today.get(Calendar.DAY_OF_WEEK));
                        }
                        showProgress(false);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("Send session","Error: " + error.toString());
                        showProgress(false);
                        Message message = uiHandler.obtainMessage(0);
                        message.sendToTarget();

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

    public static Handler uiHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    Toast.makeText(context, context.getString(R.string.try_again), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    //Start next activity based on what day it is
    private void startNextActivityBasedOnWeekday(int weekday) {
        switch (weekday) {
            case (Calendar.MONDAY): //Monday: nothing
                startOrthoActivityWhole();
                //startQuestionnaireActivity();
                break;
            case (Calendar.TUESDAY): //Tuesday: Whole Orthostatic test + Questionnaire
                startOrthoActivityWhole();
                //startQuestionnaireActivity();
                break;
            case (Calendar.WEDNESDAY): //Wednesday: nothing
                startOrthoActivityWhole();
                //startQuestionnaireActivity();
                break;
            case (Calendar.THURSDAY): //Thursday: Supine Orostatic test
                startOrthoActivitySupine();
                //startOrthoActivityWhole();
                break;
            case (Calendar.FRIDAY): //Friday: Questionnaire
                startQuestionnaireActivity();
                //startOrthoActivityWhole();
                break;
            case (Calendar.SATURDAY): //Saturday: nothing
                startOrthoActivityWhole();

                break;
            case (Calendar.SUNDAY): //Sunday: nothing
                startOrthoActivityWhole();

                break;
            default:
                Log.e(TAG,"Error: entered int is not a day of the week");
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v(TAG, "GoogleApi Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //Firebase listener to fetch questions and detect changes in the database
    class MyFirebaseProfileListener implements ValueEventListener {
        private static final String TAG = "Firebase listener";

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Log.v(TAG,"Querying times from firebase");
            //Process each type of data in its respective algorithm
            if (dataSnapshot.child("alarmTimes").child("minute").getValue() != null) {
                alarmMinute = dataSnapshot.child("alarmTimes").child("minute").getValue(Integer.class);
                alarmHour = dataSnapshot.child("alarmTimes").child("hour").getValue(Integer.class);
            }
            if (dataSnapshot.hasChild("lastAnalyzedFitTime")) {
                Log.v("Google Fit", "lastAnalyzedFitTime Exists on Firebase");
                lastAnalyzedDateGoogleFit = dataSnapshot.child("lastAnalyzedFitTime").getValue(Long.class);
            } else {
                Log.v("Google Fit", "lastAnalyzedFitTime does not exist on Firebase");
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MONTH, 3);
                cal.set(Calendar.DAY_OF_MONTH, 10);
                cal.set(Calendar.HOUR_OF_DAY, 11);
                lastAnalyzedDateGoogleFit = cal.getTimeInMillis();
            }
            scheduleNotifications();
            if (mClient == null) {
                prepGoogleFit();
            }
            showProgress(false);

        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.v(TAG, databaseError.toString());
        }
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
            secondApp = FirebaseApp.getInstance("larasDatabase");
            Log.v(TAG, "Preparing firebase");

            String path = "users/" + mUserID ;
            databaseRef = FirebaseDatabase.getInstance(secondApp).getReference(path);
            Log.v(TAG, "Databaseref: " + databaseRef.toString());
            mFirebaseProfileListener = new MyFirebaseProfileListener();
            databaseRef.addValueEventListener(mFirebaseProfileListener);

        }
    }

}
