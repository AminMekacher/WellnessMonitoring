package ch.epfl.esl.studentactivitymonitoring;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ch.epfl.esl.commons.Constants;

/**
 * Created by Lara on 5/17/2018.
 */

public class GraphAfterTestActivity extends AppCompatActivity {

    String TAG = "GraphAfterTestActivity";

    ArrayList<Integer> heartRateArray = new ArrayList<>();
    GraphView heartRateGraph;
    private static String testType;
    Button goToQuestionnaireButton;

    Integer minimumHR;
    Integer maximumHR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_after_test);

        heartRateGraph = findViewById(R.id.ortho_heartrate_graph);
        goToQuestionnaireButton = findViewById(R.id.go_to_questionnaire_from_graph);

        Intent intent = getIntent();
        heartRateArray = intent.getIntegerArrayListExtra("hrArray");
        testType = intent.getStringExtra(Constants.OrthostatTestType.ID);
        Log.v(TAG,"array length: " + heartRateArray.size());

        goToQuestionnaireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQuestionnaire();
            }
        });

        displayPlot();
    }

    private void displayPlot() {
        // HEART RATE GRAPH
        getMinAndMaxHR(heartRateArray);
        float min_HR = minimumHR;
        float max_HR = maximumHR;


        int n = heartRateArray.size();

        DataPoint[] Avg_values = new DataPoint[n];

        for (int i = 0; i < n; i++) {
            Avg_values[i] = new DataPoint(i, heartRateArray.get(i));
        }



        LineGraphSeries<DataPoint> HR_Mean = new LineGraphSeries<>(Avg_values);

        heartRateGraph.addSeries(HR_Mean);


        HR_Mean.setTitle(getString(R.string.avg_hr));
        HR_Mean.setColor(Color.WHITE);
        HR_Mean.setDrawAsPath(true);
        HR_Mean.setThickness(5);


        heartRateGraph.getViewport().setXAxisBoundsManual(true);

        heartRateGraph.getViewport().setMinX(0);
        heartRateGraph.getViewport().setMaxX(n);

        heartRateGraph.getViewport().setYAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinY(min_HR);
        heartRateGraph.getViewport().setMaxY(max_HR);

        heartRateGraph.getViewport().setScrollable(true);

        GridLabelRenderer HR_Label = heartRateGraph.getGridLabelRenderer();
        HR_Label.setHorizontalAxisTitle(getString(R.string.ortho_time));
        HR_Label.setVerticalAxisTitle(getString(R.string.heart_rate_bpm));
        //HR_Label.setLabelFormatter(new DateAsXAxisLabelFormatter(getApplicationContext()));
        HR_Label.setHumanRounding(false);
        HR_Label.setNumHorizontalLabels(3);
        HR_Label.setNumVerticalLabels(2);
        //heartRateGraph.getGridLabelRenderer().setLabelVerticalWidth(3);

        HR_Label.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        heartRateGraph.getViewport().setDrawBorder(true);

    }

    public void getMinAndMaxHR(ArrayList<Integer> hrArray) {

        for (Integer hr: hrArray) {
            if (minimumHR == null && maximumHR == null) {
                minimumHR = hr;
                maximumHR = hr;
            }
            if (hr < minimumHR) {
                minimumHR = hr;
            }
            if (hr > maximumHR) {
                maximumHR = hr;
            }
        }
    }

    private void startQuestionnaire() {
        final Intent intent = new Intent(this, QuestionnaireActivity.class);
        intent.putExtra(Constants.OrthostatTestType.ID,testType);
        startActivity(intent);
    }
}
