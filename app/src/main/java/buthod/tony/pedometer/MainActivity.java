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

/**
 * Created by Tony on 22/07/2017.
 */

public class MainActivity extends Activity {

    private int mSteps = 0;
    private int mInitSteps = 0;
    private TextView counterView = null;
    private Button mLaunch = null;
    private Button mStop = null;

    private SensorManager mSensorManager = null;
    private Sensor mStep = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        counterView = (TextView) findViewById(R.id.counter);
        mLaunch = (Button) findViewById(R.id.launch);
        mStop = (Button) findViewById(R.id.stop);
        // Initialize the textView.
        counterView.setText(String.valueOf(mSteps));
        // Initialize sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mStep = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        mLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.registerListener(mSensorListener, mStep, SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.unregisterListener(mSensorListener, mStep);
                mSteps = 0;
                mInitSteps = 0;
                counterView.setText(String.valueOf(mSteps));
                counterView.invalidate();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            /*
            A step counter event contains the total number of steps since the listener
            was first registered. We need to keep track of this initial value to calculate the
            number of steps taken, as the first value a listener receives is undefined.
             */
            if (mInitSteps < 1) {
                // initial value
                mInitSteps = (int) event.values[0];
            }
            // Calculate steps taken based on first counter value received.
            mSteps = (int) event.values[0] - mInitSteps;
            counterView.setText(String.valueOf(mSteps));
            counterView.invalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something
        }
    };

}
