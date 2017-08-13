package buthod.tony.pedometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

import java.util.Date;
import java.util.SortedMap;

import buthod.tony.pedometer.database.PedometerDAO;

/**
 * Created by Tony on 09/08/2017.
 */

public class PedometerManager {
    private static final int BACKUP_FREQUENCY = 20;
    public static final String IS_DETECTING_KEY = "buthod.tony.pedometer.isDetecting";

    private Context mContext = null;
    private PedometerDAO mDao = null;

    private int mSteps = 0;
    private int mInitCounter = 0;
    private int mCounter = 0;
    private boolean mIsDetecting = false;
    private SensorManager mSensorManager = null;
    private Sensor mStepSensor = null;

    private OnNewStepsListener mNewStepsListener = null;

    public PedometerManager(Context context, SensorManager sensorManager) {
        mContext = context;
        mSensorManager = sensorManager;
        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // Initialize DAO
        mDao = new PedometerDAO(mContext);
        // Recover daily steps
        mDao.open();
        mSteps = mDao.getDailySteps(new Date());
        mDao.close();
    }

    private void saveInDatabase() {
        mDao.open();
        mSteps = mDao.getDailySteps(new Date());
        mSteps += mCounter;
        mCounter = 0;
        mDao.storeSteps(new Date(), mSteps);
        mDao.close();
    }

    public void update() {
        mDao.open();
        mSteps = mDao.getDailySteps(new Date());
        mCounter = 0;
        mDao.close();
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {
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
            if (mNewStepsListener != null) {
                mNewStepsListener.onNewSteps();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something
        }
    };

    public void startDetection() {
        mInitCounter = 0;
        mSensorManager.registerListener(mSensorListener, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mIsDetecting = true;
    }

    public void stopDetection() {
        mSensorManager.unregisterListener(mSensorListener, mStepSensor);
        mIsDetecting = false;
        mInitCounter = 0;
        saveInDatabase();
    }

    public int getDailySteps() {
        return mSteps + mCounter;
    }

    public SortedMap<Date, Integer> getAllSteps() {
        mDao.open();
        SortedMap<Date, Integer> steps = mDao.getSteps();
        mDao.close();
        return steps;
    }

    public boolean isDetecting() {
        return mIsDetecting;
    }

    public String serialize() {
        String state =
            String.valueOf(mSteps)+";"+
            String.valueOf(mCounter)+";"+
            String.valueOf(mInitCounter)+";"+
            String.valueOf(mIsDetecting);
        return state;
    }

    public void deserialize(String savedState) {
        String[] states = savedState.split(";");
        if (states.length != 4)
            return;
        mSteps = Integer.valueOf(states[0]);
        mCounter = Integer.valueOf(states[1]);
        mInitCounter = Integer.valueOf(states[2]);
        mIsDetecting = Boolean.valueOf(states[3]);
        if (mIsDetecting)
            startDetection();
    }

    public static class OnNewStepsListener {
        /**
         * This function is called every time a new step is detected.
         */
        public void onNewSteps() {
            // Function to be override if needed
        }
    }

    public void setOnNewStepsListener(OnNewStepsListener newStepsListener) {
        mNewStepsListener = newStepsListener;
        mNewStepsListener.onNewSteps();
    }
}
