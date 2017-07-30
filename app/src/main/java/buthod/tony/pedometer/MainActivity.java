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

import java.util.Date;

import buthod.tony.pedometer.database.PedometerDAO;

/**
 * Created by Tony on 22/07/2017.
 */

public class MainActivity extends Activity {
    private static final int BACKUP_FREQUENCY = 20;

    private int mSteps = 0;
    private int mInitCounter = 0;
    private int mCounter = 0;
    private int mBackup = 0;
    private TextView mStepsView = null;
    private Button mLaunch = null;
    private Button mStop = null;

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
        // Initialize sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // Initialize DAO
        mDao = new PedometerDAO(getApplicationContext());
        // Recover daily steps
        mDao.open();
        int nb_steps = mDao.getDailySteps(new Date());
        mDao.close();
        mSteps = nb_steps;

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
                mSteps += mCounter;
                mCounter = 0;
                mInitCounter = 0;
                saveInDatabase();
                Toast.makeText(getApplicationContext(), R.string.stop_toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(mSensorListener, mStepSensor);
        mIsDetecting = false;
        saveInDatabase();
        super.onDestroy();
    }

    private void saveInDatabase() {
        mDao.open();
        mDao.storeSteps(new Date(), mSteps+mCounter);
        mDao.close();
        mBackup = mCounter;
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
            mStepsView.setText(String.valueOf(mSteps+mCounter));
            mStepsView.invalidate();
            // Save the number of steps in database in case of shutdown
            if (mCounter-mBackup >= BACKUP_FREQUENCY) {
                saveInDatabase();
            }
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
