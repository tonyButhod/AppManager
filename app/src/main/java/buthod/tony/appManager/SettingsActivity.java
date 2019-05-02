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
import buthod.tony.appManager.utils.Utils;

/**
 * Created by Tony on 06/08/2017.
 */

public class SettingsActivity extends RootActivity {

    public static String PREFERENCES_NAME = "AppManagerPreferences";
    public static String
            PREF_STEP_COUNTER_ON_START = "setCounterOnStart";

    protected SharedPreferences mPreferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Finish the activity if back button is pressed
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set value depending on preferences
        // Add different listeners
        findViewById(R.id.save_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showConfirmDeleteDialog(SettingsActivity.this, R.string.confirm_save_data,
                        new Runnable() {
                            @Override
                            public void run() {
                                DAOBase.saveDataExternalStorage(SettingsActivity.this);
                            }
                        });
            }
        });
        findViewById(R.id.load_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showConfirmDeleteDialog(SettingsActivity.this, R.string.confirm_load_data,
                        new Runnable() {
                            @Override
                            public void run() {
                                DAOBase.loadDataExternalStorage(SettingsActivity.this);
                            }
                        });
            }
        });
        CheckBox stepCounterOnStart = (CheckBox) findViewById(R.id.step_counter_on_start);
        stepCounterOnStart.setChecked(mPreferences.getBoolean(PREF_STEP_COUNTER_ON_START, false));
        stepCounterOnStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
