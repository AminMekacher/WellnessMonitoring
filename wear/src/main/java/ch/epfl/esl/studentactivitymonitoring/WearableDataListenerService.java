package ch.epfl.esl.studentactivitymonitoring;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import ch.epfl.esl.commons.ID;
import ch.epfl.esl.commons.TRIMP;

/**
 * Created by Lara on 4/4/2018.
 */

public class WearableDataListenerService extends WearableListenerService implements SensorEventListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {
    // Tag for Logcat
    private static final String TAG = "DataListenerService";

    private boolean mResolvingError = false;
    Sensor heartRateSensor;
    final Handler mHandler = new Handler();

    private static ArrayList<Float> hrArrayList = new ArrayList<Float>();
    private static ArrayList<Long> timestampArrayList = new ArrayList<Long>();
    private static ArrayList<Integer> accuracyArrayList = new ArrayList<Integer>();

    private static boolean isSensorUpdateEnabled = true;

    final WearableListenerService mService = this;

    SensorManager sensorManager;

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
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }


        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            // Data on the Wear API channel has changed


        }

        private void startAppendingHR(){
            isSensorUpdateEnabled = true;
            int delay = 1000*1000;
            sensorManager.registerListener((SensorEventListener) mService,heartRateSensor,delay);

        }

        private void stopAppendingHR() {
            isSensorUpdateEnabled = false;
            Log.v(TAG,"HR array list length: " + hrArrayList.size());
            sendDataToPhone(hrArrayList, timestampArrayList, accuracyArrayList);
//            Intent intent = new Intent(this, OrthostatActivity.class);
//            float[] hrArray = new float[hrArrayList.size()];
//            for (int i = 0; i < hrArrayList.size(); i++) {
//                hrArray[i] = hrArrayList.get(i);
//            }
//            intent.putExtra(ID.heartRateArrayID,hrArray);
//            startActivity(intent);
        }

    public void sendDataToPhone(final ArrayList<Float> watchData, final ArrayList<Long> timestamps, final ArrayList<Integer> accuracies) {

//        if (mGoogleApiClient.isConnected()) {
            Log.v(TAG,"Google API client is connected");
            DataMap map = new DataMap();
            PutDataMapRequest dataMap = PutDataMapRequest.create(ID.STOP_ORTHO_PATH); //turn sensor data item into datamap
            dataMap.getDataMap().putDataMap(ID.STOP_ORTHO_KEY, map);
            Log.v(TAG, "Sending array to app");
            float[] hrArray = new float[watchData.size()];
            for (int i = 0; i < watchData.size(); i++) {
                hrArray[i] = watchData.get(i);
            }
            long[] tsArray = new long[timestamps.size()];
            for (int i = 0; i < timestamps.size(); i++) {
                tsArray[i] = timestamps.get(i);
            }
            dataMap.getDataMap().putFloatArray(ID.heartRateArrayID, hrArray);
            dataMap.getDataMap().putLongArray(ID.timeStampArrayID, tsArray);
            dataMap.getDataMap().putIntegerArrayList(ID.accuracyArrayID,accuracies);
            Log.v(TAG, "HR array: " + hrArray.toString());
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
//        } else {
//            Log.v(TAG,"Google API is not connected");
//        }

    }


        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            // A message has been received from the Wear API
            Log.v(TAG, "onMessageReceived() A message from watch was received:"
                    + messageEvent.getRequestId() + " " + messageEvent.getPath());
            Log.v(TAG, messageEvent.toString());

                    String path = messageEvent.getPath();
                    switch (path) { //Path specifies the type of sensor data coming from the watch
                        case (ID.START_ORTHO_PATH):
                            Log.v(TAG,"Received start ortho path");
                            //Start appending HR
                            hrArrayList.clear();
                            timestampArrayList.clear();
                            accuracyArrayList.clear();
                            startAppendingHR();
                            break;
                        case (ID.STOP_ORTHO_PATH):
                            Log.v(TAG,"Received stop ortho path");
                            //stop appending HR and send back
                            stopAppendingHR();
                            break;
                        default:
                            Log.v(TAG, "data received from unrecognized path: " + path);
                            break;
                    }

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
            sensorManager.unregisterListener((SensorEventListener) mService, heartRateSensor);
            if (!mResolvingError && (mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
            return true;
        }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //Append data to heart rate sensor and array
        if (!isSensorUpdateEnabled) {
            Log.v(TAG,"Received the command to unregister listener");
            sensorManager.unregisterListener((SensorEventListener) mService, heartRateSensor);
        } else {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                Log.v(TAG, "Receiving heart rate: " + sensorEvent.values[0] + " with accuracy " + sensorEvent.accuracy + " at timestamp " + Calendar.getInstance().getTimeInMillis());
                hrArrayList.add(sensorEvent.values[0]);
                timestampArrayList.add(Calendar.getInstance().getTimeInMillis());
                accuracyArrayList.add(sensorEvent.accuracy);

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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
