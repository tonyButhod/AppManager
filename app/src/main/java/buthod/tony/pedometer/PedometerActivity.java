package buthod.tony.pedometer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * Created by Tony on 22/07/2017.
 */

public class PedometerActivity extends RootActivity {

    private TextView mStepsView = null;
    private GraphView mGraph = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pedometer);

        mStepsView = (TextView) findViewById(R.id.steps);
        mGraph = (GraphView) findViewById(R.id.graph);
        // Update steps view every time new steps are detected
        mPedometer.setOnNewStepsListener(new PedometerManager.OnNewStepsListener() {
            @Override
            public void onNewSteps() {
                mStepsView.setText(String.valueOf(mPedometer.getDailySteps()));
                mStepsView.invalidate();
            }
        });

        mGraph.getViewport().setScalableY(true); // enables vertical and horizontal zooming and scrolling
        mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    if ((int) value != value)
                        return "";
                    // show normal x values
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis((long) value*86400000);
                    return formatter.format(calendar.getTime());
                } else {
                    if ((int) value != value)
                        return "";
                    // show currency for y values
                    if (value >= 1000)
                        return String.valueOf(value/1000.0) + "k";
                    else
                        return super.formatLabel(value, isValueX);
                }
            }
        });
        mGraph.getGridLabelRenderer().setNumHorizontalLabels(7);
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinY(0.0);
        mGraph.getViewport().setMinX(0.0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateGraph();
    }

    private void updateGraph() {
        SortedMap<Date, Integer> steps = mPedometer.getAllSteps();
        DataPoint[] points = new DataPoint[steps.size()];
        int i = 0;
        double maxY = 1.0;
        double minX = steps.firstKey().getTime()/86400000;
        double maxX = steps.lastKey().getTime()/86400000;
        for (SortedMap.Entry<Date, Integer> entry : steps.entrySet()) {
            Date date = entry.getKey();
            int nb_steps = entry.getValue();
            if (nb_steps > maxY)
                maxY = (double) nb_steps;
            points[i] = new DataPoint(date.getTime()/86400000, nb_steps);
            i++;
        }
        mGraph.removeAllSeries();
        mGraph.getViewport().setMaxY(maxY);
        mGraph.getViewport().setMinX(minX);
        mGraph.getViewport().setMaxX(maxX);
        mGraph.addSeries(new LineGraphSeries<DataPoint>(points));
    }
}
