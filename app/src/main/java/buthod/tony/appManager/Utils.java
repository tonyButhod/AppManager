package buthod.tony.appManager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {

    /**
     * Convert a float to string without displaying useless 0.
     */
    public static String floatToString(float f) {
        if ((long) f == f)
            return String.valueOf((long) f);
        else
            return String.valueOf(f);
    }

    /**
     * Verify the storage permissions.
     * If writing and reading are not allowed, then ask the user the access.
     */
    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity.getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    /**
     * Write a content to a file in external storage.
     * @param context The activity context.
     * @param filename The filename to write.
     * @param content The content of the file.
     * @return Return true in case of success, false otherwise.
     */
    public static boolean writeToExternalStorage(Context context, String filename, String content) {
        try {
            // Write file
            File file = new File(context.getExternalFilesDir(null), filename);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            writer.close();
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * Read the file content from external storage.
     * @param context The activity context.
     * @param filename The filename to read.
     * @return Return the file content in case of success, null otherwise.
     */
    public static String readFromExternalStorage(Context context, String filename) {
        try {
            // Read the file
            File file = new File(context.getExternalFilesDir(null), filename);
            FileReader reader = new FileReader(file);
            BufferedReader buffer = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = buffer.readLine()) != null)
                sb.append(line);
            return sb.toString();
        }
        catch (IOException e) {
            Log.d("Debug", e.getLocalizedMessage());
            return null;
        }
    }
}
