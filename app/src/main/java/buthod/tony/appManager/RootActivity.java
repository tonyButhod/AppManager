package buthod.tony.appManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;

/**
 * Created by Tony on 10/08/2017.
 */

public class RootActivity extends Activity {

    protected PedometerManager mPedometer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize step counter
        SensorManager sensor = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPedometer = new PedometerManager(getBaseContext(), sensor);
        // Recover data from an intent
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(PedometerManager.IS_DETECTING_KEY, false)) {
            mPedometer.startDetection();
        }
        // Recover data from a saved instance state
        if (savedInstanceState != null) {
            String savedState = savedInstanceState.getString("mPedometer");
            if (savedState != null)
                mPedometer.deserialize(savedState);
        }
    }

    @Override
    protected void onDestroy() {
        mPedometer.stopDetection();
        super.onDestroy();
    }

    @Override
    public void startActivity(Intent intent) {
        startActivityForResult(intent, 0);
    }

    @Override
    public void startActivityForResult(Intent intent, int i) {
        intent.putExtra(PedometerManager.IS_DETECTING_KEY, mPedometer.isDetecting());
        mPedometer.stopDetection();
        super.startActivityForResult(intent, i);
    }

    @Override
    protected void onSaveInstanceState(Bundle data) {
        super.onSaveInstanceState(data);
        data.putString("mPedometer", mPedometer.serialize());
    }

    @Override
    public void finish() {
        Intent returnIntent = new Intent();
        finish(returnIntent);
    }

    public void finish(Intent returnIntent) {
        returnIntent.putExtra(PedometerManager.IS_DETECTING_KEY, mPedometer.isDetecting());
        mPedometer.stopDetection();
        setResult(Activity.RESULT_OK, returnIntent);
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // For every requestCode check IS_DETECTING_KEY to restart detection
        mPedometer.update();
        if (resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null && extras.getBoolean(PedometerManager.IS_DETECTING_KEY, false)) {
                mPedometer.startDetection();
            }
        }
    }
}
