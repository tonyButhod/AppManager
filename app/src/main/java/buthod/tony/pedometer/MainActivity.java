package buthod.tony.pedometer;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import buthod.tony.pedometer.database.PedometerDAO;

/**
 * Created by Tony on 22/07/2017.
 */

public class MainActivity extends Activity {
    private static final int BACKUP_FREQUENCY = 20;

    private int mSteps = 0;
    private int mInitCounter = 0;
    private int mCounter = 0;
    private TextView mStepsView = null;
    private Button mLaunch = null;
    private Button mStop = null;
    private GraphView mGraph = null;

    private SensorManager mSensorManager = null;
    private Sensor mStepSensor = null;
    private Boolean mIsDetecting = false;

    private PedometerDAO mDao = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mStepsView = (TextView) findViewById(R.id.steps);
        mLaunch = (Button) findViewById(R.id.launch);
        mStop = (Button) findViewById(R.id.stop);
        mGraph = (GraphView) findViewById(R.id.graph);
        // Initialize sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // Initialize DAO
        mDao = new PedometerDAO(getApplicationContext());
        // Recover daily steps
        mDao.open();
        mSteps = mDao.getDailySteps(new Date());
        mDao.close();

        // Recover data from a saved instance state
        if (savedInstanceState != null) {
            mSteps = savedInstanceState.getInt("mSteps");
            mCounter = savedInstanceState.getInt("mCounter");
            mInitCounter = savedInstanceState.getInt("mInitCounter");
            mIsDetecting = savedInstanceState.getBoolean("mIsDetecting");
            if (mIsDetecting) {
                mSensorManager.registerListener(mSensorListener, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        // Initialize the textView.
        mStepsView.setText(String.valueOf(mSteps + mCounter));

        mLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.registerListener(mSensorListener, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                Toast.makeText(getApplicationContext(), R.string.launch_toast, Toast.LENGTH_SHORT).show();
                mIsDetecting = true;
            }
        });

        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.unregisterListener(mSensorListener, mStepSensor);
                mIsDetecting = false;
                mInitCounter = 0;
                saveInDatabase();
                Toast.makeText(getApplicationContext(), R.string.stop_toast, Toast.LENGTH_SHORT).show();
                updateGraph();
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

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(mSensorListener, mStepSensor);
        mIsDetecting = false;
        saveInDatabase();
        super.onDestroy();
    }

    private void updateGraph() {
        mDao.open();
        SortedMap<Date, Integer> steps = mDao.getSteps();
        mDao.close();
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

    private void saveInDatabase() {
        mDao.open();
        mSteps = mDao.getDailySteps(new Date());
        mSteps += mCounter;
        mCounter = 0;
        mDao.storeSteps(new Date(), mSteps);
        mDao.close();
    }

    final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            /*
            A step counter event contains the total number of steps since the listener
            was first registered. We need to keep track of this initial value to calculate the
            number of steps taken, as the first value a listener receives is undefined.
             */
            if (mInitCounter < 1) {
                // initial value
                mInitCounter = (int) event.values[0];
            }
            // Calculate steps taken based on first counter value received.
            mCounter = (int) event.values[0] - mInitCounter;
            // Save the number of steps in database in case of shutdown
            if (mCounter >= BACKUP_FREQUENCY) {
                saveInDatabase();
                mInitCounter = (int) event.values[0];
            }
            mStepsView.setText(String.valueOf(mSteps+mCounter));
            mStepsView.invalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle data) {
        super.onSaveInstanceState(data);
        data.putInt("mSteps", mSteps);
        data.putInt("mCounter", mCounter);
        data.putInt("mInitCounter", mInitCounter);
        data.putBoolean("mIsDetecting", mIsDetecting);
    }

}
