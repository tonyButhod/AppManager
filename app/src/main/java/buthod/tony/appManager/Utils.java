package buthod.tony.appManager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {

    //region PARSE
    /**
     * Convert a float to string without displaying useless 0.
     * @param f The float to convert.
     * @param precision The number of decimal displayed. If negative, then displays all decimals.
     */
    public static String floatToString(float f, int precision) {
        if ((long) f == f)
            return String.valueOf((long) f);
        else if (precision < 0)
            return String.valueOf(f);
        else {
            String res = String.format("%." + precision + "f", f);
            // Remove 0 at the end
            int firstZeroEnd = res.length();
            while (res.charAt(firstZeroEnd - 1) == '0')
                firstZeroEnd--;
            return res.substring(0, firstZeroEnd);
        }
    }
    public static String floatToString(float f) {
        return floatToString(f, -1);
    }

    /**
     * Parse a string to int with a default value in case of error.
     */
    public static int parseIntWithDefault(String s, int defaultValue) {
        int result;
        try {
            result = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Parse a string to float with a default value in case of error.
     */
    public static float parseFloatWithDefault(String s, float defaultValue) {
        float result;
        try {
            result = Float.parseFloat(s);
        }
        catch (NumberFormatException e) {
            result = defaultValue;
        }
        return result;
    }

    //endregion

    /**
     * Search a pattern in a content with case insensitive.
     * @param pattern The pattern to search in content.
     * @param content The content string to search in.
     * @return True if a pattern is found, false otherwise.
     */
    public static boolean searchInString(String pattern, String content) {
        return content.toLowerCase().contains(pattern.toLowerCase());
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

    //region IMAGES

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

    /**
     * Save an image to local storage.
     * @param context The activity context.
     * @param bitmap The bitmap to save.
     * @param directory The directory where to save it.
     * @param filename The filename of the image.
     * @return True in case of success, false otherwise.
     */
    public static boolean saveLocalImage(Context context, Bitmap bitmap,
                                         File directory, String filename) {
        directory.mkdirs();
        File file = new File(directory, filename);
        if (file.exists())
            file.delete();
        boolean success = true;
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    /**
     * Load an image from local storage.
     * @param context The activity context.
     * @param directory The image's directory.
     * @param filename The image's filename.
     * @return The bitmap in case of success, null otherwise.
     */
    public static Bitmap loadLocalImage(Context context, File directory, String filename) {
        File file = new File(directory, filename);
        if (!file.exists())
            return null;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * Delete a local file.
     * @param context The activity context.
     * @param directory The file's directory.
     * @param filename The file's name to delete.
     * @return True in case of success, false otherwise.
     */
    public static boolean deleteLocalFile(Context context, File directory, String filename) {
        File file = new File(directory, filename);
        if (!file.exists())
            return false;
        return file.delete();
    }

    /**
     * Rotate the bitmap and return the new bitmap
     * @param bitmap The source bitmap.
     * @param angle The angle to rotate the image.
     * @return The new bitmap image rotated.
     */
    public static Bitmap rotateBitmapImage(Bitmap bitmap, int angle) {
        if (bitmap == null)
            return null;
        Matrix matrix = new Matrix();
        matrix.setRotate(angle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Get a square image from bitmap with the given size.
     * It first resize the bitmap so that the shortest side is equal to <c>size</c> ,
     * and then the image is cropped in the center.
     * @param image The source bitmap image.
     * @param size The final size of the image.
     * @return The square image.
     */
    public static Bitmap getSquareBitmap(Bitmap image, int size) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        int offsetX = 0, offsetY = 0;
        if (width > height) {
            height = size;
            width = (int) (height * bitmapRatio);
            offsetX = (width - size) / 2;
        }
        else {
            width = size;
            height = (int) (width / bitmapRatio);
            offsetY = (height - size) / 2;
        }
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(image, width, height, true);
        return Bitmap.createBitmap(resizedBitmap, offsetX, offsetY, size, size);
    }

    //endregion

    //region UI

    /**
     * Show a confirmation popup, and then execute onConfirm runnable it confirmed.
     */
    public static void showConfirmDeleteDialog(Activity activity, int titleResource, final Runnable onConfirm) {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(titleResource);
        // Set up dialog buttons
        final Resources res = activity.getResources();
        builder.setNegativeButton(res.getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        onConfirm.run();
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    /**
     * Show a confirmation popup, and then execute onConfirm runnable it confirmed.
     */
    public static void showOneTextFieldPopup(Activity activity, int titleResource, String placeholder,
                                             final Runnable onConfirm) {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(titleResource);
        // Set up dialog buttons
        final Resources res = activity.getResources();
        builder.setNegativeButton(res.getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        onConfirm.run();
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    //endregion
}
