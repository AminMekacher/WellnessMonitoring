package ch.epfl.esl.studentactivitymonitoring;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.epfl.esl.commons.BeCare;
import ch.epfl.esl.commons.MyDatabase;

/**
 * Created by aminmekacher on 18.05.18.
 */

public class FatigueLevels extends AppCompatActivity {

    String TAG = "HistoryActivity";
    private static Context context;

    private static double MILLISEC_PER_DAY = 8.64E7;

    //Becare JSON Request objects
    SharedPreferences sharedPref;

    MyDatabase database;
    int mUserID = 0;

    ArrayList<Date> dateArray = new ArrayList<>();
    ArrayList<Float> fatigueArray = new ArrayList<>();

    private PopupWindow mPopupWindow;
    private RelativeLayout mRelativeLayout;

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'_'hh:mm:ss");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fatigue_levels);
        Log.e(TAG, "Fatigue Levels!");

        context = getApplicationContext();

        sharedPref = context.getSharedPreferences("strings", Context.MODE_PRIVATE);

        mRelativeLayout = findViewById(R.id.layout_fatigue);

        final Button FatigueHelp = findViewById(R.id.fatigue_help);
        FatigueHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);

                View customView = inflater.inflate(R.layout.popup_layout, null);

                mPopupWindow = new PopupWindow(
                        customView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                if(Build.VERSION.SDK_INT >= 21) {
                    mPopupWindow.setElevation(5.0f);
                }

                ImageButton closeButton = findViewById(R.id.ib_close);

                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPopupWindow.dismiss();
                    }
                });

                mPopupWindow.showAtLocation(mRelativeLayout, Gravity.CENTER, 0, 0);
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        getFatigueScoreFromBeCare();

    }

    //Prototype function for getting sessions from Becare
    private void getFatigueScoreFromBeCare(){
        String numberOfSessionsToAnalyze = "10"; //# of sessions you want to get data from (keep in mind 2 sessions = 1 orthostatic test report)
        String url = BeCare.baseURL + "session/sessions?offset=0&limit=" + numberOfSessionsToAnalyze;
        JSONObject loginCredentials = new JSONObject();

        JsonArrayRequest jsObjRequest = new JsonArrayRequest
                (Request.Method.GET, url, loginCredentials, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Update user","Response: " + response.toString());
                        iterateThroughSessionArrayForFatigueScore(response);

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("Update user","Error: " + error.toString());

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

        RequestQueue MyRequestQueue = Volley.newRequestQueue(context);
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyRequestQueue.add(jsObjRequest);
    }

    private void iterateThroughSessionArrayForFatigueScore(JSONArray response) {
        int arrayObjects = response.length();
        for (int i = 0; i < arrayObjects; i ++) {
            try {
                JSONObject data = response.getJSONObject(i);
                String sessionType = data.getString(BeCare.ID.sessionType);
                if (sessionType.equals(BeCare.SessionType.questionnaire)) {
                    String dateString = data.getString("end");
                    try {
                        Date dateTemp = format.parse(dateString);
                        dateArray.add(dateTemp);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    JSONObject phases = data.getJSONObject("globaldata");

                    String fatigueScore = phases.getString("FATIGUE_QUESTIONARY");
                    fatigueArray.add(Float.valueOf(fatigueScore));

                    Log.v(TAG,"Got fatigue score " + fatigueScore + " for date " + dateString);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        displayFatigueLevels();
    }

    private void displayFatigueLevels() {

        final GraphView FatigueGraph = findViewById(R.id.fatigue_graph);

        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calendar.getTime();

        int graphSize = fatigueArray.size();

        DataPoint[] Fatigue_levels = new DataPoint[graphSize + 1];

        for (int i = 0; i < graphSize; i ++) {
            Fatigue_levels[i] = new DataPoint(dateArray.get(i), fatigueArray.get(i));
        }

        Fatigue_levels[graphSize] = new DataPoint(tomorrow, 200.0);

        BarGraphSeries<DataPoint> Fatigue_Serie = new BarGraphSeries<>(Fatigue_levels);

        FatigueGraph.addSeries(Fatigue_Serie);

        Viewport Fatigue_VP = FatigueGraph.getViewport();

        Fatigue_VP.setXAxisBoundsManual(true);
        Fatigue_VP.setMinX(dateArray.get(graphSize - 1).getTime() - MILLISEC_PER_DAY);
        Fatigue_VP.setMaxX(tomorrow.getTime());

        Fatigue_VP.setYAxisBoundsManual(true);
        Fatigue_VP.setMinY(0.0);
        Fatigue_VP.setMaxY(4.0);




    }
}
