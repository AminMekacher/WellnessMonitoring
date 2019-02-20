package ch.epfl.esl.studentactivitymonitoring;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ch.epfl.esl.commons.ActivitySegment;
import ch.epfl.esl.commons.HeartRateDataPoint;
import ch.epfl.esl.commons.TRIMP;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

/**
 * Created by Lara on 4/30/2018.
 */

public class GoogleFitService extends Service {
    String TAG = "GoogleFitService";

    ArrayList<HeartRateDataPoint> googleFitHeartRateArray = new ArrayList<>();
    ArrayList<ActivitySegment> googleFitActivitySegments = new ArrayList<>();
    long lastAnalyzedDateGoogleFit;
    private int hrDataCount;
    private int sampleDataCount;
    Integer hrDataMax;
    Integer samplesDataMax;

    Context context;
    GoogleApiClient client;

    int mUserID;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Fit Service binded");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Fit Service created");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Fit Service started");
        lastAnalyzedDateGoogleFit = intent.getLongExtra("lastTime",Calendar.getInstance().getTimeInMillis());
        WelcomeActivity welcomeActivity = new WelcomeActivity();
        client = welcomeActivity.mClient;
        context = welcomeActivity.context;
        mUserID = WelcomeActivity.mUserID;
        //code here
        try {
            getDataFitData();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    private void getDataFitData() throws InterruptedException {
        Handler mHandler = new Handler();

        Calendar cal = Calendar.getInstance();
        long endTime = cal.getTimeInMillis();
        long startTime = lastAnalyzedDateGoogleFit;
        long difference = endTime - startTime;
        getFitDataParallel(startTime, endTime);


    }

    private void getFitDataParallel(long startTime, long endTime) {
        java.text.DateFormat dateFormat = getDateInstance();
        DateFormat timeformat = getTimeInstance();

        Log.i("Google Fit", "Range Start: " + dateFormat.format(startTime) + " " + timeformat.format(startTime));
        Log.i("Google Fit", "Range End: " + dateFormat.format(endTime) + " " + timeformat.format(endTime));
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        PendingResult<DataReadResult> pendingDataReadResult = Fitness.HistoryApi.readData(client, readRequest);
        pendingDataReadResult.setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                Log.v("Google Fit", "Datasets: " + dataReadResult.getDataSets().size() + " buckets: " + dataReadResult.getBuckets().size());
                for (DataSet dataset: dataReadResult.getDataSets()) {
                    dumpDataSet(dataset, context);
                }
            }
        });
    }

    private void dumpDataSet(DataSet dataSet, Context context) {
        Log.i("Google Fit", "Data returned for Data type: " + dataSet.getDataType().getName() + " count: " + dataSet.getDataPoints().size());

        for (DataPoint dp : dataSet.getDataPoints()) {

            for (Field field : dp.getDataType().getFields()) {
                if (dp.getDataType().getName().equals(DataType.TYPE_HEART_RATE_BPM.getName())) {
                    HeartRateDataPoint hrDataPoint = new HeartRateDataPoint(dp.getValue(field).asFloat(),dp.getStartTime(TimeUnit.MILLISECONDS));
                    hrDataCount++;
                    hrDataMax = dataSet.getDataPoints().size();
                    googleFitHeartRateArray.add(hrDataPoint);

                } else if (dp.getDataType().getName().equals(DataType.TYPE_ACTIVITY_SEGMENT.getName())){
                    ActivitySegment activitySegment = new ActivitySegment(dp.getStartTime(TimeUnit.MILLISECONDS), dp.getEndTime(TimeUnit.MILLISECONDS), dp.getValue(field).asInt());
                    sampleDataCount++;
                    samplesDataMax = dataSet.getDataPoints().size();
                }
            }

            if (hrDataMax != null && samplesDataMax != null) {
                if (hrDataCount >= hrDataMax && sampleDataCount >= samplesDataMax) {
                    Log.v("Google Fit", "DONE!!!");
                    analyzeFitWorkoutData(context);

                }
            }

        }

    }


    private void analyzeFitWorkoutData(Context context){
        ArrayList<ArrayList<Float>> realWorkouts = new ArrayList<>();
        ArrayList<Float> currentHRList = new ArrayList<>();
        long timestampCurrentDataPoint = 0;
        long timestampPreviousDataPoint = 0;
        long oneSecond = 1000;
        boolean brokeStreak;
        int sampleCount = 0;
        int hrArraySize = googleFitHeartRateArray.size();
        for (HeartRateDataPoint hr: googleFitHeartRateArray) {
            sampleCount++;
            timestampCurrentDataPoint = hr.timeStamp;
            if ((timestampCurrentDataPoint <= timestampPreviousDataPoint + 10*oneSecond) && (timestampCurrentDataPoint - timestampPreviousDataPoint > 500)) {
                DateFormat dataFormat = getDateInstance();
                DateFormat timeFormat = getTimeInstance();
                //Log.v("Google Fit", "Start time: " + timeFormat.format(new Date(timestampPreviousDataPoint)) + " End: " + timeFormat.format(new Date(timestampCurrentDataPoint)));
                currentHRList.add(hr.value);
                //Log.v("Google Fit", "HR Start time: " + dataFormat.format(new Date(hr.timeStamp)) + " " + timeFormat.format(new Date(hr.timeStamp)));
                brokeStreak = false;
            } else {
                //Log.v("Google Fit", "Broke Streak");
                brokeStreak = true;
            }
            if ( (brokeStreak && !currentHRList.isEmpty()) || (sampleCount == hrArraySize)) {
                if (currentHRList.size() > 30) {
                    Log.v("Google Fit", "Detected workout size: " + currentHRList.size());
                    makeHRVectorIntoTRIMP(currentHRList, context);
                    DataListenerService dls = new DataListenerService();
                    ArrayList<Float> hrArrayThatDoesntDie = new ArrayList<>();
                    hrArrayThatDoesntDie.addAll(currentHRList);
                    dls.sendWorkoutDataToFirebase(hrArrayThatDoesntDie,(Long.valueOf(currentHRList.size()) * 1000),mUserID);
                }
                currentHRList.clear();
            }
            timestampPreviousDataPoint = hr.timeStamp;
        }

    }

    //Turn array of 1 Hz samples into TRIMP (10 Hz sampling)
    private void makeHRVectorIntoTRIMP(ArrayList<Float> hrArray, Context context) {
        int finalArrayLength = (hrArray.size() / 10);
        float[] finalArray = new float[finalArrayLength];
        float sum = 0;
        for (int i = 0; i < hrArray.size(); i++ ) {
            if (i == 0) {
                sum += hrArray.get(0);
            } else if ((i % 10) != 0) {
                sum += hrArray.get(i);
            } else {
                float average = sum / 10;
                finalArray[(i/10) - 1] = average;
                sum = 0;
            }
        }
        TRIMP trimp = new TRIMP(finalArray, context);
    }


}
