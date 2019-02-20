package ch.epfl.esl.studentactivitymonitoring;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import ch.epfl.esl.commons.ID;

/**
 * Created by Lara on 4/4/2018.
 */

public class OrthostatActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Member for the Wear API handle
    GoogleApiClient mGoogleApiClient;

    float[] hrArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("OrthostatActivity", "Created");
        setContentView(R.layout.orthostat_activity);
        // Start the Wear API connection

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        hrArray = getIntent().getFloatArrayExtra(ID.heartRateArrayID);


    }

    @Override
    public void onStart() {
        super.onStart();

//        if (mGoogleApiClient.isConnected()) {
//            DataMap map = new DataMap();
//            PutDataMapRequest dataMap = PutDataMapRequest.create(ID.STOP_ORTHO_PATH); //turn sensor data item into datamap
//            dataMap.getDataMap().putDataMap(ID.STOP_ORTHO_KEY, map);
//            Log.v("OrthostatActivity", "Sending array to app");
//            float[] hrArray = new float[0];
//
//            dataMap.getDataMap().putFloatArray(ID.heartRateArrayID, hrArray);
//            Log.v("OrthostatActivity", "HR array: " + hrArray);
//            PutDataRequest request = dataMap.asPutDataRequest();
//            request.setUrgent();
//            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
//                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
//                        @Override
//                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
//                            Log.v("OrthostatActivity", "Sending watch data was successful: " + dataItemResult.getStatus()
//                                    .isSuccess());
//                        }
//                    });
//        } else {
//            Log.v("OrthostatActivity","GoogleAPI is not connected");
//        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        DataMap map = new DataMap();
        PutDataMapRequest dataMap = PutDataMapRequest.create(ID.STOP_ORTHO_PATH); //turn sensor data item into datamap
        dataMap.getDataMap().putDataMap(ID.STOP_ORTHO_KEY, map);
        Log.v("OrthostatActivity", "Sending array to app");

        dataMap.getDataMap().putFloatArray(ID.heartRateArrayID, hrArray);
        Log.v("OrthostatActivity", "HR array: " + hrArray);
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        Log.v("OrthostatActivity", "Sending watch data was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
