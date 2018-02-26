package buthod.tony.pedometer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;

/**
 * Created by Tony on 06/08/2017.
 */

public class MainActivity extends RootActivity {

    private Button mSettings = null;
    private Button mGraphPedometer = null;
    private Button mOnOff = null;
    private Button mExpenses = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        ////////////// Settings //////////////
        mSettings = (Button) findViewById(R.id.settings);
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(v.getContext(), SettingsActivity.class);
                startActivityForResult(settingsIntent, 0);
            }
        });

        ///////////// Pedometer ///////////////
        // Button to access graph view
        mGraphPedometer = (Button) findViewById(R.id.show_graph);
        mGraphPedometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pedometerIntent = new Intent(v.getContext(), PedometerActivity.class);
                startActivity(pedometerIntent);
            }
        });
        mPedometer.setOnNewStepsListener(new PedometerManager.OnNewStepsListener() {
            @Override
            public void onNewSteps() {
                String text = String.valueOf(mPedometer.getDailySteps()) + "   steps";
                mGraphPedometer.setText(text);
                mGraphPedometer.invalidate();
            }
        });

        boolean detectOnStart = preferences.getBoolean("detectOnStart", false);
        mOnOff = (Button) findViewById(R.id.detection_on_off);
        if (savedInstanceState == null && detectOnStart) {
            mPedometer.startDetection();
        }
        mOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPedometer.isDetecting()) {
                    mPedometer.stopDetection();
                    mOnOff.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.red));
                    mOnOff.setText(R.string.off);
                }
                else {
                    mPedometer.startDetection();
                    mOnOff.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.green));
                    mOnOff.setText(R.string.on);
                }
            }
        });

        ///////////////// Expenses ///////////////
        mExpenses = (Button) findViewById(R.id.expenses_section);
        mExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent expensesIntent = new Intent(getBaseContext(), AccountActivity.class);
                startActivity(expensesIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String text = String.valueOf(mPedometer.getDailySteps()) + "   steps";
        mGraphPedometer.setText(text);
        mGraphPedometer.invalidate();
        if (mPedometer.isDetecting()) {
            mOnOff.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.green));
            mOnOff.setText(R.string.on);
            mOnOff.invalidate();
        }
        else {
            mOnOff.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.red));
            mOnOff.setText(R.string.off);
            mOnOff.invalidate();
        }
    }
}
