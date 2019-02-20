package ch.epfl.esl.studentactivitymonitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ch.epfl.esl.commons.ID;
import ch.epfl.esl.commons.WatchData;

/**
 * Created by Lara on 3/8/2018.
 */

public class OnConnectReceiver extends BroadcastReceiver implements CapabilityApi.CapabilityListener,
        MessageApi.MessageListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private final static String TAG = "OnConnectReceiver";

    // Member for the Wear API handle
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    MyDatabase database;
    int nodeCount = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG,"Phone connected to watch");

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        database = MyDatabase.getDatabase(context);
        getNodeCount(context);

    }

    private int getNodeCount(final Context context) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> nodes = getConnectedNodesResult.getNodes();
                nodeCount = nodes.size();
                // Do your stuff with connected nodes here
                Log.v(TAG, "Connected nodes: " + nodes.toString());
                if (nodes.size() > 0) {
                    Log.v(TAG,"Nodecount is nonzero");
                    GetDataFromDatabase gdfd = new GetDataFromDatabase();
                    gdfd.execute(context);
                } else {
                    getNodeCount(context);
                }
            }
        });

        return nodeCount;
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


    public void sendDataToPhone(final ArrayList<Float> watchData, long workoutLength, Context context) {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                new SendMessageTask(ID.WATCH_DATA_PATH).execute();
            }
        } else {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }
        DataMap map = new DataMap();
        PutDataMapRequest dataMap = PutDataMapRequest.create(ID.WATCH_DATA_PATH); //turn sensor data item into datamap
        dataMap.getDataMap().putDataMap(ID.WATCH_DATA_KEY, map);
        Log.v(TAG, "Sending array to app");
        float[] hrArray = new float[watchData.size()];
        for (int i = 0; i < watchData.size(); i++) {
            hrArray[i] = watchData.get(i);
        }
        dataMap.getDataMap().putFloatArray(ID.heartRateArrayID, hrArray);
        dataMap.getDataMap().putLong(ID.workoutDurationID, workoutLength);
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

    class GetDataFromDatabase extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... contexts) {
            // adding the measurement into the database
            //database.sensorDataDao().insertSensorData(heartRateData);
            // Retrieving cached data
            List<HeartRateData> hr_data = database.sensorDataDao().getLastValues();
            Log.v(TAG,"# of data lists in room: " + hr_data.size());
            // Send data to phone
            for (HeartRateData hr_data_value: hr_data) {
                ArrayList<Float> hrArray = hr_data_value.value;
                long workoutDuration = hr_data_value.workoutDuration;
                sendDataToPhone(hrArray, workoutDuration, contexts[0]);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Log.v(TAG,"Deleting data from Room");
            DeleteDataTask ddt = new DeleteDataTask();
            ddt.execute();
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {
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

    private class DeleteDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            database.sensorDataDao().deleteAll();
            return null;
        }
    }
}
