package ch.epfl.esl.studentactivitymonitoring;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ch.epfl.esl.commons.BeCare;
import ch.epfl.esl.commons.MyDatabase;

/**
 * Created by aminmekacher on 24.04.18.
 */

public class HistoryActivity extends AppCompatActivity {

    String TAG = "HistoryActivity";
    private static Context context;
    private static double MILLISEC_PER_DAY = 8.64E7;

    //Becare JSON Request objects
    SharedPreferences sharedPref;

    // Firebase variables

    int mUserID = 0;
    MyDatabase database;

    boolean legend_displayed = false;

    private DatabaseReference databaseRef;
    FirebaseApp secondApp;
    private HistoryActivity.MyFirebaseProfileListener mFirebaseProfileListener;

    //Variables used to store the Firebase datas (Heart rate and workout time)
    ArrayList<Float> heartRateMax = new ArrayList<>();
    ArrayList<Float> heartRateMin = new ArrayList<>();
    ArrayList<Float> heartRateAvg = new ArrayList<>();

    ArrayList<Date> dateArray = new ArrayList<>();

    ArrayList<Long> durationWorkout = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity);
        Log.e(TAG, "You called me again!");

        context = getApplicationContext();

        database = MyDatabase.getDatabase(context);
        sharedPref = context.getSharedPreferences("strings", Context.MODE_PRIVATE);

        final TextView HR_Text = findViewById(R.id.hr_graph_text);
        final GraphView HR_Graph = findViewById(R.id.heartrate_graph);
        final TextView HR_Instruction = findViewById(R.id.hr_instruction);

        final TextView Duration_Text = findViewById(R.id.duration_text);
        final GraphView Duration_Graph = findViewById(R.id.duration_graph);
        final TextView Duration_Instruction = findViewById(R.id.duration_instruction);

        final TextView No_Workout = findViewById(R.id.no_workout_text);

        //Get user id from database
        HistoryActivity.GetDataFromDatabase gdfd = new HistoryActivity.GetDataFromDatabase();
        gdfd.execute();

        final Button displayHR = findViewById(R.id.display_hr);
        displayHR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (heartRateMax.size() > 0) {

                    HR_Text.setVisibility(View.VISIBLE);
                    HR_Graph.setVisibility(View.VISIBLE);
                    HR_Instruction.setVisibility(View.VISIBLE);

                    Duration_Text.setVisibility(View.GONE);
                    Duration_Graph.setVisibility(View.GONE);
                    Duration_Instruction.setVisibility(View.GONE);

                    No_Workout.setVisibility(View.GONE);

                    displayPlots();

                }

                else {

                    No_Workout.setVisibility(View.VISIBLE);
                }

            }
        });

        Button displayDuration = findViewById(R.id.display_duration);
        displayDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (heartRateMax.size() > 0) {

                    Duration_Text.setVisibility(View.VISIBLE);
                    Duration_Graph.setVisibility(View.VISIBLE);
                    Duration_Instruction.setVisibility(View.VISIBLE);

                    HR_Text.setVisibility(View.GONE);
                    HR_Graph.setVisibility(View.GONE);
                    HR_Instruction.setVisibility(View.GONE);

                    No_Workout.setVisibility(View.GONE);

                    displayPlots();

                }

                else {

                    No_Workout.setVisibility(View.VISIBLE);
                }


            }
        });

    }

    // Display the plots of the previous workouts when the menu option is selected

    private void displayPlots() {

        // HEART RATE GRAPH

        float min_HR = 200;
        float max_HR = 0;

        GraphView HR_Graph = findViewById(R.id.heartrate_graph);

        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calendar.getTime();

        int n = heartRateMax.size();

        DataPoint[] Avg_values = new DataPoint[n + 1];

        for (int i = 0; i < n; i++) {
            Avg_values[i] = new DataPoint(dateArray.get(i), heartRateAvg.get(i));
        }

        for (int i = 0; i < n; i++) {
            Log.i(TAG, "Graph avg!" + Avg_values[i]);
        }

        Avg_values[n] = new DataPoint(tomorrow, 200.0);

        DataPoint[] Max_values = new DataPoint[n + 1];

        for (int i = 0; i < n; i++) {
            Max_values[i] = new DataPoint(dateArray.get(i), heartRateMax.get(i));

            if (heartRateMax.get(i) > max_HR) {
                max_HR = heartRateMax.get(i);
            }
        }

        Max_values[n] = new DataPoint(tomorrow, 200.0);

        DataPoint[] Min_values = new DataPoint[n + 1];

        for (int i = 0; i < n; i++) {
            Min_values[i] = new DataPoint(dateArray.get(i), heartRateMin.get(i));

            if (heartRateMin.get(i) < min_HR) {
                min_HR = heartRateMin.get(i);
            }
        }

        Min_values[n] = new DataPoint(tomorrow, 200.0);

        PointsGraphSeries<DataPoint> HR_Mean = new PointsGraphSeries<>(Avg_values);

        HR_Graph.addSeries(HR_Mean);

        HR_Mean.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(context, getString(R.string.avg_toast) + " " +  Math.round(dataPoint.getY()) + " " + getString(R.string.bpm_toast) + "\n" +  getString(R.string.date_toast) + " " +new SimpleDateFormat("MM/dd/yyyy").format(new Date((long) dataPoint.getX())), Toast.LENGTH_SHORT).show();
            }
        });

        HR_Mean.setTitle(getString(R.string.avg_hr));
        HR_Mean.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float v, float v1, DataPointInterface dataPoint) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                canvas.drawCircle(v, v1, 15, paint);
            }
        });

        PointsGraphSeries<DataPoint> HR_Max = new PointsGraphSeries<>(Max_values);

        HR_Graph.addSeries(HR_Max);

        /* HR_Max.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Log.e("TAG", "Toast AVG!");
                Toast.makeText(context, getString(R.string.max_toast) + dataPoint.getY() + getString(R.string.bpm_toast) + "\n" +  getString(R.string.date_toast) + new SimpleDateFormat("MM/dd/yyyy").format(new Date((long) dataPoint.getX())), Toast.LENGTH_SHORT).show();
            }
        }); */
        HR_Max.setColor(Color.RED);
        HR_Max.setTitle(getString(R.string.max_hr));
        HR_Max.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y,
                             DataPointInterface dataPoint) {
                paint.setStrokeWidth(5);
                canvas.drawLine(x - 10, y - 10,  x + 10, y + 10, paint);
                canvas.drawLine(x + 10, y - 10, x - 10, y + 10, paint);
            }

        });

        PointsGraphSeries<DataPoint> HR_Min = new PointsGraphSeries<>(Min_values);
        HR_Graph.addSeries(HR_Min);

        /*HR_Min.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(context, getString(R.string.min_toast) + dataPoint.getY() + getString(R.string.bpm_toast) +  "\n" + getString(R.string.date_toast) + new SimpleDateFormat("MM/dd/yyyy").format(new Date((long) dataPoint.getX())), Toast.LENGTH_SHORT).show();
            }
        });*/
        HR_Min.setColor(Color.GREEN);
        HR_Min.setTitle(getString(R.string.min_hr));
        HR_Min.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y,
                             DataPointInterface dataPoint) {
                paint.setStrokeWidth(5);
                canvas.drawLine(x - 10, y - 10,  x + 10, y + 10, paint);
                canvas.drawLine(x + 10, y - 10, x - 10, y + 10, paint);
            }

        });

        HR_Graph.getViewport().setXAxisBoundsManual(true);

        HR_Graph.getViewport().setMinX(dateArray.get(n - 1).getTime() - MILLISEC_PER_DAY);
        HR_Graph.getViewport().setMaxX(tomorrow.getTime());

        HR_Graph.getViewport().setYAxisBoundsManual(true);
        HR_Graph.getViewport().setMinY(min_HR);
        HR_Graph.getViewport().setMaxY(max_HR);

        HR_Graph.getViewport().setScrollable(true);

        GridLabelRenderer HR_Label = HR_Graph.getGridLabelRenderer();
        HR_Label.setHorizontalAxisTitle(getString(R.string.workout_number));
        HR_Label.setVerticalAxisTitle(getString(R.string.heart_rate_bpm));
        HR_Label.setLabelFormatter(new DateAsXAxisLabelFormatter(context));
        HR_Label.setHumanRounding(false);
        HR_Label.setNumHorizontalLabels(3);
        HR_Label.setNumVerticalLabels(2);

        HR_Label.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        HR_Graph.getViewport().setDrawBorder(true);

        if (legend_displayed == false) {

            Log.e(TAG, "Toast!");

            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.legend_toast,
                    (ViewGroup)findViewById(R.id.toast_layout));

            Toast toast = new Toast(this);
            toast.setView(view);
            toast.show();

            legend_displayed = true;
        }

        // WORKOUT DURATION GRAPH

        long durationMin = 300000000, durationMax = 0;

        GraphView durationGraph = findViewById(R.id.duration_graph);

        int m = durationWorkout.size();

        DataPoint[] Duration_values = new DataPoint[m + 1];

        for (int i = 0; i < m; i++) {
            Duration_values[i] = new DataPoint(dateArray.get(i), durationWorkout.get(i) / 1000);

            if (durationWorkout.get(i) < durationMin) {
                durationMin = durationWorkout.get(i);
            }

            if (durationWorkout.get(i) > durationMax) {
                durationMax = durationWorkout.get(i);
            }
        }

        Duration_values[m] = new DataPoint(tomorrow, 0.0);

        PointsGraphSeries<DataPoint> WorkoutDuration = new PointsGraphSeries<>(Duration_values);
        durationGraph.addSeries(WorkoutDuration);

        WorkoutDuration.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(context, getString(R.string.duration_toast) + " " +  dataPoint.getY() + " " + getString(R.string.seconds_toast) + "\n" + getString(R.string.date_toast) + "  " + new SimpleDateFormat("MM/dd/yyyy").format(new Date((long) dataPoint.getX())), Toast.LENGTH_SHORT).show();
            }
        });
        WorkoutDuration.setColor(Color.BLUE);
        WorkoutDuration.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float v, float v1, DataPointInterface dataPoint) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                canvas.drawCircle(v, v1, 15, paint);
            }
        });

        durationGraph.getViewport().setXAxisBoundsManual(true);

        durationGraph.getViewport().setMinX(dateArray.get(m - 1).getTime() - MILLISEC_PER_DAY);
        durationGraph.getViewport().setMaxX(tomorrow.getTime());

        durationGraph.getViewport().setYAxisBoundsManual(true);
        durationGraph.getViewport().setMinY(durationMin / 1000 - 1);
        durationGraph.getViewport().setMaxY(durationMax / 1000 + 1);

        durationGraph.getViewport().setScrollable(true);

        GridLabelRenderer duration_Label = durationGraph.getGridLabelRenderer();
        duration_Label.setHorizontalAxisTitle(getString(R.string.workout_number));
        duration_Label.setVerticalAxisTitle(getString(R.string.workout_duration_graph));
        duration_Label.setLabelFormatter(new DateAsXAxisLabelFormatter(context));
        duration_Label.setHumanRounding(false);
        duration_Label.setNumHorizontalLabels(2);
        duration_Label.setNumVerticalLabels(2);
        duration_Label.setVerticalLabelsAlign(Paint.Align.LEFT);
        duration_Label.setLabelsSpace(10);

        duration_Label.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        durationGraph.getViewport().setDrawBorder(true);
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

    @Override
    public void onStart() {
        super.onStart();
        prepFirebase();

    }

    //Firebase listener to fetch questions and detect changes in the database
    class MyFirebaseProfileListener implements ValueEventListener {
        private static final String TAG = "Firebase listener";

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'_'hh:mm:ss");
        Date dateTemp;

        float hrMax = 0, hrMin = 200, hrTotal = 0;
        int n = 0;

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Log.v(TAG,"Querying times from firebase");
            //Process each type of data in its respective algorithm

            if (dataSnapshot.getValue() != null) {

                for (DataSnapshot dataSnapshot1 : dataSnapshot.child("HR_Workout").getChildren()) {

                    try {
                        dateTemp = format.parse(dataSnapshot1.getKey());
                        dateArray.add(dateTemp);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    Log.e(TAG, "Date is " +  dataSnapshot1.getKey());

                    for (DataSnapshot dataSnapshot2 : dataSnapshot1.getChildren()) {

                        for (DataSnapshot dataSnapshot3 : dataSnapshot2.child("heartRateWorkout").getChildren()) {
                            //heartRateArrayWorkout.add(dataSnapshot3.getValue(Float.class));
                            n++;

                            if (dataSnapshot3.getValue(Float.class) > hrMax) {
                                hrMax = dataSnapshot3.getValue(Float.class);
                            }

                            if (dataSnapshot3.getValue(Float.class) < hrMin) {
                                hrMin = dataSnapshot3.getValue(Float.class);
                            }

                            hrTotal += dataSnapshot3.getValue(Float.class);

                            Log.i(TAG, "Workout new value read!" + dataSnapshot3.getValue(Float.class));

                        }

                        heartRateMax.add(hrMax);
                        heartRateMin.add(hrMin);
                        heartRateAvg.add(hrTotal / n);

                        hrMax = 0;
                        hrMin = 200;
                        hrTotal = 0;
                        n = 0;

                        for (int i = 0; i < heartRateAvg.size(); i++) {
                            Log.i(TAG, "Workout new entry!" + heartRateMax.size());
                        }

                    }

                    //Log.v(TAG, "Is array empty? " + heartRateArrayWorkout.isEmpty());

                }

                for (DataSnapshot dataSnapshot1 : dataSnapshot.child("HR_Workout").getChildren()) {

                    for (DataSnapshot dataSnapshot2 : dataSnapshot1.getChildren()) {

                        durationWorkout.add(dataSnapshot2.child("durationWorkout").getValue(Long.class));

                        for (int i = 0; i < durationWorkout.size(); i++) {
                            Log.i(TAG, "Workout new duration added!" + durationWorkout.get(i));
                        }

                    }
                }

            }

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
            userID = 3;

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
