package buthod.tony.appManager;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import buthod.tony.appManager.account.AccountActivity;
import buthod.tony.appManager.database.DAOBase;
import buthod.tony.appManager.recipes.RecipesActivity;

/**
 * Created by Tony on 06/08/2017.
 */

public class SettingsActivity extends RootActivity {

    public static String PREFERENCES_NAME = "AppManagerPreferences";
    public static String
            PREF_STEP_COUNTER_ON_START = "setCounterOnStart";

    protected SharedPreferences mPreferences = null;

    private ImageButton mBackButton = null;
    private Button mSaveDataButton = null;
    private Button mLoadDataButton = null; // Button used for development to avoid loosing data
    private CheckBox mStepCounterOnStart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        mSaveDataButton = (Button) findViewById(R.id.save_data);
        mLoadDataButton = (Button) findViewById(R.id.load_data); // This button is not displayed by default
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mStepCounterOnStart = (CheckBox) findViewById(R.id.step_counter_on_start);

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set value depending on preferences
        mStepCounterOnStart.setChecked(mPreferences.getBoolean(PREF_STEP_COUNTER_ON_START, false));
        // Add different listeners
        mSaveDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DAOBase.saveDataExternalStorage(SettingsActivity.this);
            }
        });
        mLoadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DAOBase.loadDataExternalStorage(SettingsActivity.this);
            }
        });
        mStepCounterOnStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save the change in preferences
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(PREF_STEP_COUNTER_ON_START, isChecked);
                editor.apply();
            }
        });
    }
}
