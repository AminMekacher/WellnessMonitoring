package ch.epfl.esl.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.exp;

/**
 * Created by Lara on 3/7/2018.
 * Class to store and send TRIMP values to becare
 * All you need to do is instantiate a trimp object and it sends the data to becare
 */

public class TRIMP {

    float[] heartRateArray;
    float restingHeartRate;
    float maxHeartRate;
    String gender;
    int age;
    Context appContext;
    MyDatabase database;
    String TAG = "TRIMP";
    SharedPreferences sharedPref;

    public TRIMP(float[] hrArray, Context context) {
        heartRateArray = hrArray;
        appContext = context;
        database = MyDatabase.getDatabase(context); //Make sure context is for phone app, not wear
        sharedPref = context.getSharedPreferences("strings", Context.MODE_PRIVATE);
        getAgeAndGenderFromBeCare();
    }

    private void setTrimpBackgroundInformation() {
        if (age != 0) {
            maxHeartRate = 220 - age;
        }
        Log.v("TRIMP", "max HR is + " + maxHeartRate);

    }

//    private void getRestingHeartRateFromSharedPreferences(){
//        float RHR = sharedPref.getFloat("restingHeartRate",0);
//        if (RHR != 0) {
//            Log.v(TAG,"Getting RHR from SharedPreferences: " + RHR);
//            restingHeartRate = RHR;
//            computeAndSendTRIMP();
//        } else {
//            Log.v(TAG,"Getting RHR from SharedPreferences failed");
//            getRestingHRFromBeCare();
//        }
//    }

    private float iterateThroughSessionArray(JSONArray response, int counter) {
        float output = 0;
        try {
            JSONObject data = response.getJSONObject(counter);
            String sessionType = data.getString(BeCare.ID.sessionType);
            if (sessionType.equals(BeCare.SessionType.energy)) {
                JSONArray phases = data.getJSONArray("phases");
                JSONObject globalData = phases.getJSONObject(0).getJSONObject("globaldata");
                output = (float) globalData.getDouble(BeCare.GlobalData.AverageHR);
                Log.v(TAG,"Got resting HR " + output + " during iteration " + counter);
            } else {
                output = iterateThroughSessionArray(response,++counter);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return output;
    }

    private void getRestingHRFromBeCare() {
        String url = BeCare.baseURL + "session/sessions?offset=0&limit=10";
        JSONObject loginCredentials = new JSONObject();

        JsonArrayRequest jsObjRequest = new JsonArrayRequest
                (Request.Method.GET, url, loginCredentials, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Update user","Response: " + response.toString());
                        int counter = 0;
                        restingHeartRate = iterateThroughSessionArray(response, counter);
                        if (restingHeartRate != 0) {
                            Log.v(TAG, "Getting average HR from becare worked, going to compute trimp");
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putFloat("restingHeartRate", restingHeartRate);
                            editor.commit();
                            computeAndSendTRIMP();
                        } else {
                            Log.v(TAG,"Getting average HR from becare failed");
                            saveHRDataInRoomIfShitFails();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("Update user","Error: " + error.toString());
                        //Save HR data to room if this fails
                        saveHRDataInRoomIfShitFails();
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                String token = sharedPref.getString("authorizationToken","");
                headers.put("Authorization",token);
                return headers;
            }
        };

        RequestQueue MyRequestQueue = Volley.newRequestQueue(appContext);
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyRequestQueue.add(jsObjRequest);
    }


    private void getAgeAndGenderFromBeCare(){
        String url = BeCare.baseURL + "user/details";
        JSONObject loginCredentials = new JSONObject();

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, loginCredentials, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Update user","Response: " + response.toString());
                        try {
                            String bday = response.getString("birthdate");
                            gender = response.getString("gender");
                            Log.v("TRIMP", "User gender is " + gender);
                            birthdayToAge(bday);

                            //Next, get last resting HR
                            getRestingHRFromBeCare();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            //Save HR data to room if this fails
                            saveHRDataInRoomIfShitFails();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("Update user","Error: " + error.toString());
                        //Save HR data to room if this fails
                        saveHRDataInRoomIfShitFails();
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                String token = sharedPref.getString("authorizationToken","");
                headers.put("Authorization",token);
                return headers;
            }
        };

        RequestQueue MyRequestQueue = Volley.newRequestQueue(appContext);
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyRequestQueue.add(jsObjRequest);

    }

    private void birthdayToAge(String bday) {
        String[] split = bday.split("-");
        int year = Integer.valueOf(split[0]);
        int month = Integer.valueOf(split[1]);
        int day = Integer.valueOf(split[2]);
        Log.v("TRIMP", "Year: " + year + " Month: " + month + " Day: " + day);
        Calendar birthday = Calendar.getInstance();
        birthday.set(Calendar.YEAR,year);
        birthday.set(Calendar.MONTH, month);
        birthday.set(Calendar.DAY_OF_MONTH, day);
        Log.v(TAG,"Birthday object: " + birthday.toString());
        Calendar today = Calendar.getInstance();
        long difference = today.getTimeInMillis() - birthday.getTimeInMillis();
        long msInAYear = (long) 1000 * 60 * 60 * 24 * 365;
        long ageLong =  difference / msInAYear;
        age = (int) ageLong;
        Log.v("TRIMP", "The user is " + age + " years old");
        setTrimpBackgroundInformation();
    }

    public void computeAndSendTRIMP() {
        double output = 0;

        double sumThing = 0;
        for (float hr: heartRateArray) {
            sumThing += ((double) hr - (double) restingHeartRate) / ((double) maxHeartRate - (double) restingHeartRate);
        }

        if (gender.equals(Constants.Gender.F)) {
            output = sumThing*0.86*exp(1.67*sumThing);
        } else if (gender.equals(Constants.Gender.M)) {
            output = sumThing*0.64*exp(1.92*sumThing);
        }

        Log.v("TRIMP","Calculated TRIMP: " + output);

        String url = BeCare.baseURL + "biodata/setBiodata";
        JSONObject loginCredentials = new JSONObject();
        try {
            loginCredentials.put("type","TRIMP");
            loginCredentials.put("value",output);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, loginCredentials, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("TRIMP","Success! Response: " + response.toString());

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("TRIMP","Error: " + error.toString());


                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = sharedPref.getString("authorizationToken","");
                Log.v("Volley","Setting token to " + token);
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", token);
                return headers;
            }
        };

        RequestQueue MyRequestQueue = Volley.newRequestQueue(appContext);
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyRequestQueue.add(jsObjRequest);
    }

    public void saveHRDataInRoomIfShitFails(){
        StoreToDatabase stdb = new StoreToDatabase();
        ArrayList<Float> hrArray = new ArrayList<Float>();
        for (float hr: heartRateArray) {
            hrArray.add(hr);
        }
        stdb.execute(hrArray);
    }

    class StoreToDatabase extends AsyncTask<ArrayList<Float>, Void, ArrayList<Float>> {

        @Override
        protected ArrayList<Float> doInBackground(ArrayList<Float>... watchHR) {
            HeartRateData heartRateData = new HeartRateData();
            heartRateData.timestamp = System.nanoTime();
            heartRateData.value = watchHR[0];
            // adding the measurement into the database
            database.sensorDataDao().insertSensorData(heartRateData);
            // Retrieving the last measurement
            List<HeartRateData> hr_data =
                    database.sensorDataDao().getLastValues();
            Log.v(TAG,"# of data lists in room: " + hr_data.size());
            // Merge two lists for returning all at once
            ArrayList<Float> last_data = hr_data.get(hr_data.size() - 1).value;
            return last_data;
        }

        @Override
        protected void onPostExecute(ArrayList<Float> last_data) {
            Log.v(TAG,"Saving data to Room: " + last_data.toString());
        }
    }
}
