package buthod.tony.appManager;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SortedMap;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import org.json.JSONArray;
import org.json.JSONException;

import buthod.tony.appManager.database.PedometerDAO;

/**
 * Created by Tony on 22/07/2017.
 */

public class PedometerActivity extends RootActivity {

    private ImageButton mBackButton = null;
    private TextView mStepsView = null;
    private TextView mTapInformation = null;
    private GraphView mGraph = null;

    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pedometer);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mStepsView = (TextView) findViewById(R.id.steps);
        mTapInformation = (TextView) findViewById(R.id.tap_information);
        mGraph = (GraphView) findViewById(R.id.graph);

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Update steps view every time new steps are detected
        mPedometer.setOnNewStepsListener(new PedometerManager.OnNewStepsListener() {
            @Override
            public void onNewSteps() {
                mStepsView.setText(String.valueOf(mPedometer.getDailySteps()));
                mStepsView.invalidate();
            }
        });
        calendar = Calendar.getInstance();

        mGraph.getViewport().setScalable(true);
        mGraph.getViewport().setScalableY(false); // enables vertical and horizontal zooming and scrolling
        mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    if ((int) value != value)
                        return "";
                    // show normal x values
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM");
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
        mGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
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
        if (steps.size() == 0)
            return;

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

        // Draw line series
        LineGraphSeries<DataPoint> lineSeries = new LineGraphSeries<DataPoint>(points);
        lineSeries.setColor(Color.LTGRAY);
        lineSeries.setThickness(2);
        mGraph.addSeries(lineSeries);
        // Draw points series
        PointsGraphSeries<DataPoint> pointsSeries = new PointsGraphSeries<DataPoint>(points);
        pointsSeries.setShape(PointsGraphSeries.Shape.POINT);
        pointsSeries.setSize(5.0f);
        // Add listener to show a particular data information
        pointsSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                calendar.setTimeInMillis((long) dataPoint.getX()*86400000);
                String tapDate = formatter.format(calendar.getTime());
                int tapSteps = (int) dataPoint.getY();
                mTapInformation.setText("Le " + tapDate + " : " + tapSteps + " pas");
            }
        });
        mGraph.addSeries(pointsSeries);
    }

    /**
     * Function used to save all data stored on the database in a external file.
     * @param context The context of the application.
     * @return The JSONArray containing all data stored on the database.
     */
    public static JSONArray saveDataPublicStorage(Context context) throws JSONException {
        PedometerDAO dao = new PedometerDAO(context);
        dao.open();
        SortedMap<Date, Integer> stepsData = dao.getSteps();
        dao.close();

        JSONArray stepsJson = new JSONArray();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);
        for (SortedMap.Entry<Date, Integer> entry : stepsData.entrySet()) {
            JSONArray entryJson = new JSONArray();
            entryJson.put(formatter.format(entry.getKey()));
            entryJson.put(entry.getValue());
            stepsJson.put(entryJson);
        }

        return stepsJson;
    }

    public static void loadDataPublicStorage(Context context, JSONArray data)
            throws JSONException, ParseException {
        PedometerDAO dao = new PedometerDAO(context);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);

        dao.open();
        for (int i = 0; i < data.length(); ++i) {
            JSONArray entryJson = data.getJSONArray(i);
            dao.storeSteps(formatter.parse(entryJson.getString(0)), entryJson.getInt(1));
        }
        dao.close();
    }
}