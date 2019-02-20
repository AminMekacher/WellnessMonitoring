package ch.epfl.esl.studentactivitymonitoring;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
import ch.epfl.esl.commons.BeCare;
import ch.epfl.esl.commons.Constants;
import ch.epfl.esl.commons.DataToSend;
import ch.epfl.esl.commons.ID;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Lara on 2/21/2018.
 * Gets list of questions from Firebase
 * Stores user answers
 */

public class QuestionnaireActivity extends AppCompatActivity {
    private static final String TAG = "QuestionnaireActivity";

    private static RequestQueue MyRequestQueue;

    String startQuestionnaire;

    //Arrays of questions and answers to be displayed
    ArrayList<String> questions;
    ArrayList<Integer> seekBarAnswers;
    ArrayList<Integer> numericAnswers;

    //Constant numbers of questions
    final static int numberOfSeekbars = 8;
    final static int numberOfNumericQuestions = 2;
    final static int seekbarMax = 6;

    //Seekbars that user uses to answer questions
    ArrayList<SeekBar> seekBars;
    ArrayList<EditText> editTexts;
    ArrayList<TextView> textViews;

    //Database reference and Firebase listener to get questions
    private DatabaseReference databaseRef;
    FirebaseApp secondApp;
    private QuestionnaireActivity.MyFirebaseProfileListener mFirebaseProfileListener;

    LinearLayout layout;
    String languageCode;

    String testType; //Test type: full orthostatic or questionnaire
    SharedPreferences sp;
    private static Context context;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.questionnaire);
        context = getApplicationContext();
        sp = this.getSharedPreferences("strings", Context.MODE_PRIVATE);;

        MyRequestQueue = Volley.newRequestQueue(this);

        Intent intent = getIntent();
        testType = intent.getStringExtra(Constants.OrthostatTestType.ID);

        //Initialize string arrays
        questions = new ArrayList<String>();
        seekBars = new ArrayList<SeekBar>();
        seekBarAnswers = new ArrayList<Integer>();
        editTexts = new ArrayList<EditText>();
        numericAnswers = new ArrayList<Integer>();
        textViews = new ArrayList<TextView>();

        layout = findViewById(R.id.questionnaireLayout);
        LinearLayout layout = new LinearLayout(this);

        //Get start date of questionnaire
        Calendar cal = Calendar.getInstance();
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        startQuestionnaire = df.format(cal.getTime());


        String language = Locale.getDefault().getDisplayLanguage();
        if (language.equals("français")) {
            languageCode = "fr";
        } else {
            languageCode = "en";
        }

        final Button doneButton = findViewById(R.id.doneQuestionnaire);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doneButtonPress();
            }
        });

    }

    private void doneButtonPress() {

        //cancel the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(ID.tuesdayNotificationID);

        //Cancel all repeat notifications
        ID.wasTestDoneToday = true;

        boolean cancel = false;
        for (EditText editText: editTexts) {
            int answer = 0;
            try {
                answer = Integer.valueOf(editText.getText().toString());

            } catch (NumberFormatException nfe) {
                editText.requestFocus();
                editText.setError(getString(R.string.input_must_be_integer));
                cancel = true;
            }
        }
        //Make sure user enters reasonable numbers
        if (editTexts.isEmpty()) {
            cancel = true;
            Toast.makeText(context, "Error submitting questionnaire, please try again",Toast.LENGTH_LONG).show();
        } else {
            try {
                int answer1 = Integer.valueOf(editTexts.get(0).getText().toString());
                int answer2 = Integer.valueOf(editTexts.get(1).getText().toString());
                if (answer1 < 0 || answer1 > 24) {
                    editTexts.get(0).requestFocus();
                    editTexts.get(0).setError(getString(R.string.enter_realistic_value));
                    cancel = true;
                }
                if (answer2 < 0 || answer2 > 1440) {
                    editTexts.get(1).requestFocus();
                    editTexts.get(1).setError(getString(R.string.enter_realistic_value));
                    cancel = true;
                }

            } catch (NumberFormatException nfe) {
            }
        }
        Log.v(TAG,"Cancel: " + cancel);
        if (!cancel) {
            Log.v(TAG,"Preparing data for becare");
            prepareDataForBeCare();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_help_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                String errorMesssage = getString(R.string.help_questionnaire);
                HelpFragment dialog = HelpFragment.newInstance(errorMesssage);
                dialog.show(getFragmentManager(), TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        prepFirebase();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    //When the activity is paused or destroyed, get rid of the seekbars
    @Override
    public void onPause() {
        super.onPause();
        getAnswers();
        Log.v(TAG,"Destroying seekbars");
        for (TextView tv : textViews){
            layout.removeView(tv);
        }
        for (SeekBar sb : seekBars){
            layout.removeView(sb);
        } for (EditText et: editTexts){
            layout.removeView(et);
        }
        seekBars.clear();
        editTexts.clear();
        questions.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (TextView tv : textViews){
            layout.removeView(tv);
        }
        for (SeekBar sb : seekBars){
            layout.removeView(sb);
        } for (EditText et: editTexts){
            layout.removeView(et);
        }
        seekBars.clear();
        editTexts.clear();
        questions.clear();
    }

    //Save seekbar answers when screen orientation changes
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        //getAnswers();
        //Log.v(TAG,"saving seekbar answers: " + seekBarAnswers);
        savedInstanceState.putIntegerArrayList("seekBarAnswers",seekBarAnswers);
        savedInstanceState.putIntegerArrayList("editTextAnswers",numericAnswers);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        seekBarAnswers = savedInstanceState.getIntegerArrayList("seekBarAnswers");
        numericAnswers = savedInstanceState.getIntegerArrayList("editTextAnswers");
        //Log.v(TAG,"retrieving seekbar answers: " + seekBarAnswers);

    }

    //Prepare firebase to get questions
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

        secondApp = FirebaseApp.getInstance("larasDatabase");
        Log.v(TAG,"Preparing firebase");
        String path = "questionnaire/";
        databaseRef = FirebaseDatabase.getInstance(secondApp).getReference(path);
        Log.v(TAG,"Databaseref: " + databaseRef.toString());
        mFirebaseProfileListener = new QuestionnaireActivity.MyFirebaseProfileListener();
        databaseRef.addListenerForSingleValueEvent(mFirebaseProfileListener);
    }

    //Add scale question to the array of questions
    private void addScaleQuestionToArray(String question){
        Log.v(TAG,"Adding question" + question);
        if (questions.size() < 10) {
            questions.add(question);
            updateScaleUI(question);
        }

    }

    //Add numeric question to the array of questions
    private void addNumericQuestionToArray(String question) {
        Log.v(TAG,"Adding question" + question);
        if (questions.size() < 10) {
            questions.add(question);
            updateNumericUI(question);
        }
    }

    //Add a number text field to the UI for numeric questions
    private void updateNumericUI(String question) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView tv = new TextView(this);
        tv.setLayoutParams(lparams);
        tv.setText(question);
        tv.setTextColor(getResources().getColor(R.color.white));
        //tv.setTextSize(24);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        textViews.add(tv);
        this.layout.addView(tv);

        LinearLayout.LayoutParams sbParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        EditText answerEditText = new EditText(this);
        answerEditText.setLayoutParams(sbParams);
        answerEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        answerEditText.setTextColor(getResources().getColor(R.color.white));
        answerEditText.setTypeface(Typeface.DEFAULT_BOLD);

        editTexts.add(answerEditText);
        this.layout.addView(answerEditText);

        if (!numericAnswers.isEmpty() && numericAnswers != null) {
            if (numericAnswers.get(editTexts.size() - 1) != 9999) {
                editTexts.get(editTexts.size() - 1).setText(String.valueOf(numericAnswers.get(editTexts.size() - 1)));
            }
        }
    }

    //Update the UI every time a new question is pulled from firebase
    private void updateScaleUI(String question) {
        //setListAdapter(new QuestionnaireArrayAdapter(this, questions)); //using array adapter

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView tv=new TextView(this);
        tv.setLayoutParams(lparams);
        tv.setText(question);
        //tv.setTextSize(24);
        tv.setTextColor(getResources().getColor(R.color.white));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        textViews.add(tv);
        this.layout.addView(tv);

        LinearLayout.LayoutParams sbParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        SeekBar answerSeekBar = new SeekBar(this);
        answerSeekBar.setLayoutParams(sbParams);
        answerSeekBar.setMax(seekbarMax);
        answerSeekBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress));
        seekBars.add(answerSeekBar);
        this.layout.addView(answerSeekBar);


        if (!seekBarAnswers.isEmpty() && seekBarAnswers != null ) {
            if (seekBarAnswers.size() <= numberOfSeekbars) {
                //Log.v("Seekbar problem","# of seekbars: " + seekBars.size() + " # of answers: " + seekBarAnswers.size());
                seekBars.get(seekBars.size() - 1).setProgress(seekBarAnswers.get(seekBars.size() - 1));
            }
        }

    }

    //Get the user's answers to the questions and prompt them is the anster is invalid
    private void getAnswers() {
        seekBarAnswers.clear();
        Log.v(TAG,"There are "+ seekBars.size() + " seekbars");
        for (SeekBar seekBar: seekBars){
            int answer = seekBar.getProgress();
            seekBarAnswers.add(answer);
        }
        numericAnswers.clear();
        boolean cancel = false;
        for (EditText editText: editTexts) {
            int answer = 9999;
            try {
                answer = Integer.valueOf(editText.getText().toString());
            } catch (NumberFormatException nfe) {
                cancel = true;
                editText.requestFocus();
                editText.setError(getString(R.string.input_must_be_integer));
            }
            numericAnswers.add(answer);
        }
        Log.v(TAG,"Answers: " + seekBarAnswers + numericAnswers);

    }

    //Set up the be.care json object with orthostatic test data
    private void prepareDataForBeCare(){
        getAnswers();

        //Get the average answer and transfer data to be.care format
        float sum = 0;
        ArrayList<String> answerStrings = new ArrayList<String>();
        for (float i: seekBarAnswers) {
            sum += (i * 99 / seekbarMax);
        } for (float j: numericAnswers) {
            sum += j;
        }
        float averageAnswer = sum/(seekBarAnswers.size() + numericAnswers.size());
        for (int i = 0; i < 7; i++) {
            answerStrings.add(String.valueOf((seekBarAnswers.get(i) * 99 / seekbarMax)));
        }
        answerStrings.add(String.valueOf(numericAnswers.get(0)));
        answerStrings.add(String.valueOf((seekBarAnswers.get(7) * 99 / seekbarMax)));
        answerStrings.add(String.valueOf(numericAnswers.get(1)));

        //Get end time of questionnaire
        Calendar cal = Calendar.getInstance();
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String timeStamp = df.format(cal.getTime());

        DataToSend dataToSend = new DataToSend();
        JSONObject jsonObject;
        if (testType.equals(BeCare.SessionType.questionnaire)) { //If it is just a questionnaire, only send Q answers
            dataToSend.clear();
            dataToSend.setOnlyQuestionnaire(BeCare.SessionType.questionnaire,startQuestionnaire,timeStamp,answerStrings,String.valueOf(averageAnswer));
            jsonObject = dataToSend.createJSONObjectQuestionnaire();
            sendDataToBeCare(jsonObject, true);
        }
        //If you did a full ortho test, first send the whole test and then just the questionnaire data
        else {
            dataToSend.setQuestionnaire(BeCare.SessionType.energy,answerStrings,String.valueOf(averageAnswer),timeStamp);
            jsonObject = dataToSend.createJSONObjectWhole();
            sendDataToBeCare(jsonObject, false);
            dataToSend.clear();
            dataToSend.setOnlyQuestionnaire(BeCare.SessionType.questionnaire, startQuestionnaire, timeStamp, answerStrings, String.valueOf(averageAnswer));
            JSONObject jsonObject1 = dataToSend.createJSONObjectQuestionnaire();
            sendDataToBeCare(jsonObject1, true);
        }

        Log.v(TAG,"Data JSON object: " + jsonObject.toString());


    }

    private void goToWelcomeActivity() {
        Toast.makeText(context,getString(R.string.data_upload_success),Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, WelcomeActivity.class);
        startActivity(intent);
    }

    //Send the session to be.care
    private void sendDataToBeCare(final JSONObject jsonObject, final boolean sendingQuestionnaire){

        String url = BeCare.baseURL + "session/sendSession";
        Log.v(TAG,"Sending questionnaire? " + sendingQuestionnaire);
        Log.v(TAG,"JSON object: " + jsonObject.toString());

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Send session","Response: " + response.toString());
                        if (sendingQuestionnaire) {
                            goToWelcomeActivity();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //If the sessions sending fails, try renewing the token
                        Log.v("Send session","Error: " + error.toString());
                        //renewToken(jsonObject, sendingQuestionnaire);

                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = sp.getString("authorizationToken","");
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

    //Renew authorization token
    private void renewToken(final JSONObject jsonObject, final boolean sendingQuestionnaire) {
        Log.v(TAG,"Renewing token");
        String url = BeCare.baseURL + "secure/renewToken";

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Renewing Token","Response: " + response.toString());
                        try {
                            JSONObject headers = response.getJSONObject("headers");
                            String bearer = headers.getString("Authorization");
                            String[] split = bearer.split(" ");
                            String token = split[1];
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("authorizationToken", token);
                            editor.commit();
                            Log.v("Volley", "Token: " + token);
                            sendDataToBeCare(jsonObject, sendingQuestionnaire);
                        } catch (JSONException e) {
                            Log.v("Volley","Getting headers failed");
                            e.printStackTrace();
                            renewTokenFailed();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("Renewing Token","Error: " + error.toString());
                        //TODO: Implement error handling
                        renewTokenFailed();
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = sp.getString("authorizationToken","");
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

    private void renewTokenFailed() {

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

    //Firebase listener to fetch questions and detect changes in the database
    class MyFirebaseProfileListener implements ValueEventListener {
        private static final String TAG = "Firebase listener";

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Log.v(TAG,"Querying questions from firebase");
            //Process each type of data in its respective algorithm
            for (final DataSnapshot object : dataSnapshot.getChildren()) {
                    String questionText = object.child(languageCode).getValue(String.class);
                    String questionType = object.child("type").getValue(String.class);
                    switch (questionType) {
                        case (Constants.QuestionType.scale):
                            addScaleQuestionToArray(questionText);
                            break;
                        case (Constants.QuestionType.numeric):
                            addNumericQuestionToArray(questionText);
                            break;
                        default:
                            Log.v(TAG, "Unrecognized question type");
                            break;
                    }
            }

        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.v(TAG, databaseError.toString());
        }
    }

}
