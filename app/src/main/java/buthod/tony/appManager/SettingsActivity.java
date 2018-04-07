package buthod.tony.appManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

/**
 * Created by Tony on 06/08/2017.
 */

public class SettingsActivity extends PreferenceActivity {

    protected PedometerManager mPedometer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize step counter
        SensorManager sensor = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPedometer = new PedometerManager(getBaseContext(), sensor);
        // Recover data from an intent
        Bundle extras = getIntent().getExtras();
        if (extras.getBoolean(PedometerManager.IS_DETECTING_KEY, false)) {
            mPedometer.startDetection();
        }
        // Recover data from a saved instance state
        if (savedInstanceState != null) {
            String savedState = savedInstanceState.getString("mPedometer");
            if (savedState != null)
                mPedometer.deserialize(savedState);
        }
        // Display preferences
        getFragmentManager().beginTransaction().replace(android.R.id.content, new Prefs1Fragment()).commit();
    }

    public static class Prefs1Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
        }
    }

    @Override
    protected void onDestroy() {
        mPedometer.stopDetection();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PedometerManager.IS_DETECTING_KEY, mPedometer.isDetecting());
        mPedometer.stopDetection();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}
