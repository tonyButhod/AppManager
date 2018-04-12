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
                saveDataPublicStorage();
            }
        });
        mLoadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDataPublicStorage();
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

    /// SAVE AND RESTORE DATA PART ///

    private static String SAVE_JSON_FILE = "SavedData.json";

    /**
     * Function used to save all data stored on the database in an external file.
     * The data is saved in the file SAVE_JSON_FILE.
     */
    private void saveDataPublicStorage() {
        // First check if we have the permission to write the file
        verifyStoragePermissions();

        try {
            // Get data from other classes
            JSONObject objectToSave = new JSONObject();
            objectToSave.put(PedometerActivity.class.getName(), PedometerActivity.saveDataPublicStorage(this));
            objectToSave.put(AccountActivity.class.getName(), AccountActivity.saveDataPublicStorage(this));

            // Write file
            File file = new File(getExternalFilesDir(null), SAVE_JSON_FILE);
            FileWriter writer = new FileWriter(file);
            writer.write(objectToSave.toString());
            writer.flush();
            writer.close();
            Toast.makeText(this, getResources().getString(R.string.save_data_success), Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.write_error), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load data from an external file SAVE_JSON_FILE.
     * The data are added in the different databases.
     */
    private void loadDataPublicStorage() {
        // First check if we have the permission to write the file
        verifyStoragePermissions();

        File file = new File(getExternalFilesDir(null), SAVE_JSON_FILE);
        try {
            // Read the file
            FileReader reader = new FileReader(file);
            BufferedReader buffer = new BufferedReader(reader);
            String content = "", line;
            while ((line = buffer.readLine()) != null)
                content += line;
            // Parse content to populate databases
            JSONObject objectToLoad = new JSONObject(content);
            PedometerActivity.loadDataPublicStorage(this,
                    objectToLoad.getJSONArray(PedometerActivity.class.getName()));
            AccountActivity.loadDataPublicStorage(this,
                    objectToLoad.getJSONArray(AccountActivity.class.getName()));
            Toast.makeText(this, getResources().getString(R.string.load_data_success), Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.read_error), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Verify the storage permissions. If writing and reading are not allowed, then ask the user the access.
     */
    private void verifyStoragePermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
}
