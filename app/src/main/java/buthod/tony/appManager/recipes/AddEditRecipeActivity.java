package buthod.tony.appManager.recipes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import buthod.tony.appManager.DragAndDropLinearLayout;
import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.Utils;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.database.RecipesDAO;

public class AddEditRecipeActivity extends RootActivity {

    private RecipesDAO mDao = null;

    // UI elements
    private RelativeLayout mRootLayout = null;
    private ImageButton mBackButton = null;
    private Button mAddRecipeButton = null;
    private Spinner mRecipeTypeSpinner = null;
    private EditText mRecipeName = null;
    private SeekBar mDifficultySeekBar = null;
    private TextView mDifficultyView = null;
    private SeekBar mGradeSeekBar = null;
    private TextView mGradeView = null;
    private EditText mTimeEdit = null;
    private EditText mPeopleEdit = null;
    private LinearLayout mIngredientsLayout = null, mStepsLayout = null;
    private Button mAddIngredientButton = null, mAddStepButton = null,
        mAddIngredientSeparationButton = null, mAddStepSeparationButton = null;
    private ImageView mRecipeImageView = null;
    private ImageButton mAddImageButton = null, mDeleteImageButton = null,
            mRotateLeftButton = null, mRotateRightButton = null;
    private TextView mErrorView = null;
    private Button mGetWebRecipeButton = null;

    private ArrayAdapter<CharSequence>
            mRecipeTypes = null,
            mUnits = null,
            mIngredientsNames = null;

    // Fields containing recipes_activity information
    private long mRecipeId = -1; // If -1, new recipe_activity, otherwise, recipe_activity modified
    private ArrayList<Long> mIngredientsToDelete, mStepsToDelete, mSeparationsToDelete;
    private Bitmap mRecipeImage = null;

    // Object used for drag and drop
    private DragAndDropLinearLayout mDragAndDrop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_recipe);
        // Get the recipe_activity id if exists
        mRecipeId = getIntent().getLongExtra(DatabaseHandler.RECIPES_KEY, -1);
        /* Set private variables */
        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        // UI elements
        mRootLayout = (RelativeLayout) findViewById(R.id.root_layout);
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mAddRecipeButton = (Button) findViewById(R.id.add_button);
        mRecipeTypeSpinner = (Spinner) findViewById(R.id.type);
        mRecipeName = (EditText) findViewById(R.id.name);
        mDifficultySeekBar = (SeekBar) findViewById(R.id.difficulty_seek_bar);
        mDifficultyView = (TextView) findViewById(R.id.difficulty);
        mGradeSeekBar = (SeekBar) findViewById(R.id.grade_seek_bar);
        mGradeView = (TextView) findViewById(R.id.grade);
        mTimeEdit = (EditText) findViewById(R.id.time);
        mPeopleEdit = (EditText) findViewById(R.id.people_edit);
        mIngredientsLayout = (LinearLayout) findViewById(R.id.ingredients_list);
        mStepsLayout = (LinearLayout) findViewById(R.id.steps_list);
        mAddIngredientButton = (Button) findViewById(R.id.add_ingredient_button);
        mAddStepButton = (Button) findViewById(R.id.add_step_button);
        mAddIngredientSeparationButton = (Button) findViewById(R.id.add_ingredient_separation_button);
        mAddStepSeparationButton = (Button) findViewById(R.id.add_step_separation_button);
        mAddImageButton = (ImageButton) findViewById(R.id.add_image);
        mRecipeImageView = (ImageView) findViewById(R.id.image_view);
        mDeleteImageButton = (ImageButton) findViewById(R.id.delete_image);
        mRotateLeftButton = (ImageButton) findViewById(R.id.rotate_left);
        mRotateRightButton = (ImageButton) findViewById(R.id.rotate_right);
        mErrorView = (TextView) findViewById(R.id.error_message);
        mGetWebRecipeButton = (Button) findViewById(R.id.get_web_recipe_button);
        // Private lists
        mIngredientsToDelete = new ArrayList<>();
        mStepsToDelete = new ArrayList<>();
        mSeparationsToDelete = new ArrayList<>();
        // Get string array resources for spinners
        mRecipeTypes = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        mRecipeTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRecipeTypes.addAll(getResources().getStringArray(R.array.recipe_types));
        mUnits = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mUnits.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mUnits.addAll(getResources().getStringArray(R.array.units_array));
        mIngredientsNames = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        mIngredientsNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDragAndDrop = new DragAndDropLinearLayout(getBaseContext());

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showConfirmDeleteDialog(AddEditRecipeActivity.this,
                        R.string.changes_might_be_loosed_confirm, new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        });
            }
        });
        // Add or edit recipe_activity
        mAddRecipeButton.setText(getResources().getString(R.string.save));
        mAddRecipeButton.setOnClickListener(mOnAddEditRecipeClickListener);
        // Set recipe_activity types
        mRecipeTypeSpinner.setAdapter(mRecipeTypes);
        // Changes on seek bars
        mDifficultySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mDifficultyView.setText(String.valueOf(i + 1));
                mDifficultyView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        mGradeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mGradeView.setText(String.valueOf(i + 1));
                mGradeView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        // Buttons to add ingredients, steps or separations
        mAddIngredientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddEditIngredientDialog();
            }
        });
        mAddStepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddEditStepDialog();
            }
        });
        mAddIngredientSeparationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddEditSeparationDialog(RecipesDAO.Separation.INGREDIENT_TYPE);
            }
        });
        mAddStepSeparationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddEditSeparationDialog(RecipesDAO.Separation.STEP_TYPE);
            }
        });
        // Get all ingredients stored on database
        mIngredientsNames.addAll(mDao.getIngredients());

        // Put previous values if we are modifying a recipe_activity
        if (mRecipeId != -1) {
            fillActivityWithRecipe(mDao.getRecipe(mRecipeId));
        }
        // Add, rotate or delete the image
        mAddImageButton.setOnClickListener(mAddImageListener);
        mDeleteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRecipeImage(null);
            }
        });
        mRotateLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRecipeImage(Utils.rotateBitmapImage(mRecipeImage, -90));
            }
        });
        mRotateRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRecipeImage(Utils.rotateBitmapImage(mRecipeImage, 90));
            }
        });
        // Get web recipe
        mGetWebRecipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //importWebRecipeFromUrl("https://www.marmiton.org/recettes/recette_tartiflette-facile_15733.aspx");
                Intent intent = new Intent(AddEditRecipeActivity.this, WebRecipeActivity.class);
                startActivityForResult(intent, IMPORT_FROM_INTERNET);
            }
        });
    }

    /**
     * Fill the activity with the given recipe_activity id.
     */
    private void fillActivityWithRecipe(RecipesDAO.Recipe recipe) {
        mRecipeName.setText(recipe.name);
        mRecipeTypeSpinner.setSelection(recipe.type);
        mDifficultySeekBar.setProgress(recipe.difficulty - 1);
        mGradeSeekBar.setProgress(recipe.grade - 1);
        mTimeEdit.setText(String.valueOf(recipe.time));
        mPeopleEdit.setText(String.valueOf(recipe.people));
        for (int i = 0; i < recipe.ingredients.size(); ++i) {
            addEditIngredient(recipe.ingredients.get(i), null);
        }
        for (int i = 0; i < recipe.steps.size(); ++i) {
            addEditStep(recipe.steps.get(i), null);
        }
        for (int i = 0; i < recipe.separations.size(); ++i) {
            addEditSeparation(recipe.separations.get(i), null);
        }
        // Load the image
        setRecipeImage(Utils.loadLocalImage(this, getExternalFilesDir(null),
                RecipesActivity.getImageNameFromId(mRecipeId)));
    }

    /**
     * Set the recipe image and update image buttons.
     * @param bitmap The image bitmap.
     */
    private void setRecipeImage(Bitmap bitmap) {
        mRecipeImage = bitmap;
        mRecipeImageView.setImageBitmap(bitmap);
        if (bitmap != null) {
            mAddImageButton.setVisibility(View.GONE);
            mDeleteImageButton.setVisibility(View.VISIBLE);
            mRotateLeftButton.setVisibility(View.VISIBLE);
            mRotateRightButton.setVisibility(View.VISIBLE);
        }
        else {
            mAddImageButton.setVisibility(View.VISIBLE);
            mDeleteImageButton.setVisibility(View.GONE);
            mRotateLeftButton.setVisibility(View.GONE);
            mRotateRightButton.setVisibility(View.GONE);
        }
    }

    private View.OnClickListener mOnAddEditRecipeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Add the recipe_activity and exit the activity
            RecipesDAO.Recipe recipe = new RecipesDAO.Recipe();
            recipe.id = mRecipeId;
            recipe.name = mRecipeName.getText().toString();
            recipe.type = mRecipeTypeSpinner.getSelectedItemPosition();
            recipe.difficulty = Utils.parseIntWithDefault(mDifficultyView.getText().toString(), 0);
            recipe.grade = Utils.parseIntWithDefault(mGradeView.getText().toString(), 0);
            recipe.time = Utils.parseIntWithDefault(mTimeEdit.getText().toString(), -1);
            recipe.people = Utils.parseIntWithDefault(mPeopleEdit.getText().toString(), -1);
            // Check if data are correct
            String errorMessage = "";
            Resources res = getResources();
            if (recipe.name == null || recipe.name.isEmpty())
                errorMessage = res.getString(R.string.recipe_name_error);
            else if (recipe.time < 0)
                errorMessage = res.getString(R.string.recipe_time_error);
            else if (recipe.people < 0)
                errorMessage = res.getString(R.string.recipe_people_error);
            if (!errorMessage.isEmpty()) {
                // An error occurred
                mErrorView.setVisibility(View.VISIBLE);
                mErrorView.setText(errorMessage);
                return;
            }
            // Get ingredients, steps and separations from views
            recipe.ingredients = new ArrayList<>();
            recipe.steps = new ArrayList<>();
            recipe.separations = new ArrayList<>();
            for (int i = 0; i < mIngredientsLayout.getChildCount(); ++i) {
                View v = mIngredientsLayout.getChildAt(i);
                if (v.getTag().equals(DatabaseHandler.INGREDIENTS_TABLE_NAME)) {
                    RecipesDAO.Ingredient ingredient = getIngredientFromView(v);
                    ingredient.number = i;
                    recipe.ingredients.add(ingredient);
                }
                else {
                    RecipesDAO.Separation separation = getSeparationFromView(v);
                    separation.number = i;
                    separation.type = RecipesDAO.Separation.INGREDIENT_TYPE;
                    recipe.separations.add(separation);
                }
            }
            for (int i = 0; i < mStepsLayout.getChildCount(); ++i) {
                View v = mStepsLayout.getChildAt(i);
                if (v.getTag().equals(DatabaseHandler.STEPS_TABLE_NAME)) {
                    RecipesDAO.Step step = getStepFromView(v);
                    step.number = i;
                    recipe.steps.add(step);
                }
                else {
                    RecipesDAO.Separation separation = getSeparationFromView(v);
                    separation.number = i;
                    separation.type = RecipesDAO.Separation.STEP_TYPE;
                    recipe.separations.add(separation);
                }
            }
            // Otherwise, add/edit the recipe
            mRecipeId = mDao.addEditRecipe(recipe);
            mDao.deleteQuantities(mIngredientsToDelete);
            mDao.deleteSteps(mStepsToDelete);
            mDao.deleteSeparations(mSeparationsToDelete);
            // Save the image to external storage
            if (mRecipeImage == null)
                Utils.deleteLocalFile(AddEditRecipeActivity.this, getExternalFilesDir(null),
                        RecipesActivity.getImageNameFromId(mRecipeId));
            else
                Utils.saveLocalImage(AddEditRecipeActivity.this, mRecipeImage,
                        getExternalFilesDir(null), RecipesActivity.getImageNameFromId(mRecipeId));
            finish();
        }
    };

    /**
     * Get the ingredient object from the view.
     */
    private RecipesDAO.Ingredient getIngredientFromView(View v) {
        RecipesDAO.Ingredient ingredient = new RecipesDAO.Ingredient();
        ingredient.idQuantity = Long.parseLong(
                ((TextView)v.findViewById(R.id.id_view)).getText().toString());
        ingredient.quantity = Float.parseFloat(
                ((TextView)v.findViewById(R.id.quantity_view)).getText().toString());
        ingredient.name = ((TextView)v.findViewById(R.id.ingredient_name_view)).getText().toString();
        ingredient.idUnit = Integer.parseInt(
                ((TextView)v.findViewById(R.id.id_unit_view)).getText().toString());
        ingredient.idIngredient = Long.parseLong(
                ((TextView)v.findViewById(R.id.id_ingredient_view)).getText().toString());
        ingredient.type = (v.findViewById(R.id.optional_view).getVisibility() == View.VISIBLE ?
                RecipesDAO.Ingredient.OPTIONAL_TYPE : RecipesDAO.Ingredient.NORMAL_TYPE);
        return ingredient;
    }

    /**
     * Get the step object from the view.
     */
    private RecipesDAO.Step getStepFromView(View v) {
        RecipesDAO.Step step = new RecipesDAO.Step();
        step.id = Long.parseLong(
                ((TextView)v.findViewById(R.id.id_view)).getText().toString());
        step.description = ((TextView)v.findViewById(R.id.description_view)).getText().toString();
        return step;
    }

    /**
     * Get the separation object from the view.
     */
    private RecipesDAO.Separation getSeparationFromView(View v) {
        RecipesDAO.Separation separation = new RecipesDAO.Separation();
        separation.id = Long.parseLong(
                ((TextView)v.findViewById(R.id.id_view)).getText().toString());
        separation.description = ((TextView)v.findViewById(R.id.description_view)).getText().toString();
        return separation;
    }

    //region ADD_EDIT_INGREDIENT

    /**
     * Show the dialog to add or modify an ingredient.
     * @param v The ingredient view. If new ingredient, then v is null.
     */
    private void showAddEditIngredientDialog(final View v) {
        final long id = (v == null ? -1 :
                Long.parseLong(((TextView)v.findViewById(R.id.id_view)).getText().toString()));
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_ingredient);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_edit_ingredient, null);
        builder.setView(alertView);
        // Get and set useful widget
        final EditText quantity = (EditText) alertView.findViewById(R.id.quantity);
        final Spinner unitSpinner = (Spinner) alertView.findViewById(R.id.unit);
        unitSpinner.setAdapter(mUnits);
        final AutoCompleteTextView autoCompleteView =
                (AutoCompleteTextView) alertView.findViewById(R.id.ingredient_auto_complete);
        final CheckBox optionalCheckBox = (CheckBox) alertView.findViewById(R.id.optional_check_box);
        final TextView errorView = (TextView) alertView.findViewById(R.id.error_message);
        autoCompleteView.setAdapter(mIngredientsNames);
        // Set default values if ingredient is not null
        if (v != null) {
            quantity.setText(((TextView)v.findViewById(R.id.quantity_view)).getText());
            unitSpinner.setSelection(Integer.parseInt(((TextView)v.findViewById(R.id.id_unit_view)).getText().toString()));
            autoCompleteView.setText(((TextView)v.findViewById(R.id.ingredient_name_view)).getText());
            optionalCheckBox.setChecked(v.findViewById(R.id.optional_view).getVisibility() == View.VISIBLE);
        }
        // Set up dialog buttons
        final Resources res = getResources();
        if (v != null)
            builder.setNeutralButton(res.getString(R.string.delete),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mIngredientsLayout.removeView(v);
                            if (id != -1)
                                mIngredientsToDelete.add(id);
                            dialog.cancel();
                        }
                    });
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(v == null ? res.getString(R.string.add) : res.getString(R.string.modify),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        // Get data from views
                        RecipesDAO.Ingredient ingredient = new RecipesDAO.Ingredient();
                        ingredient.idQuantity = id;
                        ingredient.idUnit = unitSpinner.getSelectedItemPosition();
                        ingredient.name = autoCompleteView.getText().toString();
                        ingredient.quantity = Utils.parseFloatWithDefault(quantity.getText().toString(), -1);
                        ingredient.type = optionalCheckBox.isChecked() ?
                                RecipesDAO.Ingredient.OPTIONAL_TYPE : RecipesDAO.Ingredient.NORMAL_TYPE;
                        // Check ingredient data
                        String errorMessage = "";
                        if (ingredient.quantity < 0)
                            errorMessage = res.getString(R.string.ingredient_quantity_error);
                        else if (ingredient.name == null || ingredient.name.isEmpty())
                            errorMessage = res.getString(R.string.ingredient_name_error);
                        if (!errorMessage.isEmpty()) {
                            // Ingredient data are not valid
                            errorView.setText(errorMessage);
                            errorView.setVisibility(View.VISIBLE);
                            return;
                        }
                        // Add or modify the ingredient
                        addEditIngredient(ingredient, v);
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }
    private void showAddEditIngredientDialog() { showAddEditIngredientDialog(null);}

    /**
     * Add or modify an ingredient.
     * @param ingredient The ingredient to add or modify.
     * @param v The view to edit. If null, inflate a new one.
     */
    private void addEditIngredient(final RecipesDAO.Ingredient ingredient, View v) {
        // First check if the ingredient name is already in database, otherwise need to add it.
        if (ingredient.name != null)
            ingredient.idIngredient = mDao.getIngredientId(ingredient.name);
        // Add or modify the view
        if (v == null) {
            // Then add it to the view.
            v = getLayoutInflater().inflate(R.layout.ingredient_view, null);
            v.setTag(DatabaseHandler.INGREDIENTS_TABLE_NAME);
            v.setOnClickListener(mIngredientOnClickListener);
            v.setOnLongClickListener(mIngredientOnLongClickListener);
            mIngredientsLayout.addView(v, mIngredientsLayout.getChildCount());
        }
        // Update text view information
        ((TextView) v.findViewById(R.id.id_view)).setText(String.valueOf(ingredient.idQuantity));
        ((TextView) v.findViewById(R.id.quantity_view)).setText(String.valueOf(ingredient.quantity));
        ((TextView) v.findViewById(R.id.id_unit_view)).setText(String.valueOf(ingredient.idUnit));
        ((TextView) v.findViewById(R.id.unit_view)).setText(mUnits.getItem(ingredient.idUnit));
        ((TextView) v.findViewById(R.id.ingredient_name_view)).setText(ingredient.name);
        ((TextView) v.findViewById(R.id.id_ingredient_view)).setText(String.valueOf(ingredient.idIngredient));
        v.findViewById(R.id.optional_view).setVisibility(ingredient.type == RecipesDAO.Ingredient.OPTIONAL_TYPE ?
                View.VISIBLE : View.GONE);
        v.invalidate();
    }

    private View.OnClickListener mIngredientOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showAddEditIngredientDialog(view);
        }
    };

    private View.OnLongClickListener mIngredientOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            mDragAndDrop.StartDragging(mIngredientsLayout, view, null);
            return true;
        }
    };

    //endregion

    //region ADD_EDIT_STEP

    /**
     * Show the dialog to add or modify a step.
     * @param v The step view. If new step, then v is null.
     */
    private void showAddEditStepDialog(final View v) {
        final long id = (v == null ? -1 :
                Long.parseLong(((TextView)v.findViewById(R.id.id_view)).getText().toString()));
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_step);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_edit_step, null);
        builder.setView(alertView);
        // Get useful views
        final EditText descriptionView = (EditText) alertView.findViewById(R.id.description);
        final TextView errorView = (TextView) alertView.findViewById(R.id.error_message);
        // Set default values
        if (v != null) {
            descriptionView.setText(((TextView)v.findViewById(R.id.description_view)).getText());
        }
        // Set up dialog buttons
        final Resources res = getResources();
        if (v != null)
            builder.setNeutralButton(res.getString(R.string.delete),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mStepsLayout.removeView(v);
                            if (id != -1)
                                mStepsToDelete.add(id);
                            dialog.cancel();
                        }
                    });
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(v == null ? res.getString(R.string.add) : res.getString(R.string.modify),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        // Recover data from views
                        RecipesDAO.Step step = new RecipesDAO.Step();
                        step.id = id;
                        step.description = descriptionView.getText().toString();
                        // Check if data are correct
                        String errorMessage = "";
                        if (step.description.isEmpty())
                            errorMessage = res.getString(R.string.step_description_error);
                        if (!errorMessage.isEmpty()) {
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setText(errorMessage);
                            return;
                        }
                        // Add or modify the step
                        addEditStep(step, v);
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }
    private void showAddEditStepDialog() { showAddEditStepDialog(null);}

    /**
     * Add or modify a step.
     * @param step The step to add or modify.
     * @param v The view to edit. If null, inflate a new one.
     */
    private void addEditStep(final RecipesDAO.Step step, View v) {
        // Add or modify the view
        if (v == null) {
            // Then add it to the view.
            v = getLayoutInflater().inflate(R.layout.step_view, null);
            v.setTag(DatabaseHandler.STEPS_TABLE_NAME);
            v.setOnClickListener(mStepOnClickListener);
            v.setOnLongClickListener(mStepOnLongClickListener);
            mStepsLayout.addView(v, mStepsLayout.getChildCount());
        }
        // Update text view information
        ((TextView) v.findViewById(R.id.id_view)).setText(String.valueOf(step.id));
        ((TextView) v.findViewById(R.id.description_view)).setText(step.description);
        v.invalidate();
    }

    private View.OnClickListener mStepOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showAddEditStepDialog(view);
        }
    };

    private View.OnLongClickListener mStepOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            mDragAndDrop.StartDragging(mStepsLayout, view, null);
            return true;
        }
    };

    //endregion

    //region ADD_EDIT_SEPARATION

    /**
     * Show the dialog to add or edit a separation.
     * @param type The type of separation, depending on if the separation is for
     *             ingredients or steps.
     * @param v The separation view. If new separation, then v is null.
     */
    private void showAddEditSeparationDialog(final int type, final View v) {
        final long id = (v == null ? -1 :
                Long.parseLong(((TextView)v.findViewById(R.id.id_view)).getText().toString()));
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_separation);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_edit_separation, null);
        builder.setView(alertView);
        // Get useful views
        final EditText descriptionView = (EditText) alertView.findViewById(R.id.description);
        final TextView errorView = (TextView) alertView.findViewById(R.id.error_message);
        // Set default values
        if (v != null) {
            descriptionView.setText(((TextView)v.findViewById(R.id.description_view)).getText());
        }
        // Set up dialog buttons
        final Resources res = getResources();
        if (v != null)
            builder.setNeutralButton(res.getString(R.string.delete),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (type == RecipesDAO.Separation.INGREDIENT_TYPE)
                                mIngredientsLayout.removeView(v);
                            else
                                mStepsLayout.removeView(v);
                            if (id != -1)
                                mSeparationsToDelete.add(id);
                            dialog.cancel();
                        }
                    });
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(v == null ? res.getString(R.string.add) : res.getString(R.string.modify),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        // Recover data from views
                        RecipesDAO.Separation separation = new RecipesDAO.Separation();
                        separation.id = id;
                        separation.description = descriptionView.getText().toString();
                        separation.type = type;
                        separation.number = (type == RecipesDAO.Separation.INGREDIENT_TYPE ?
                                mIngredientsLayout.getChildCount() : mStepsLayout.getChildCount());
                        // Check if data are correct
                        String errorMessage = "";
                        if (separation.description.isEmpty())
                            errorMessage = res.getString(R.string.step_description_error);
                        if (!errorMessage.isEmpty()) {
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setText(errorMessage);
                            return;
                        }
                        // Add or edit the separation
                        addEditSeparation(separation, v);
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }
    private void showAddEditSeparationDialog(int type) { showAddEditSeparationDialog(type, null);}

    /**
     * Add or modify a separation.
     * @param separation The separation to add or modify.
     * @param v The separation view. If null, inflate a new one.
     */
    private void addEditSeparation(final RecipesDAO.Separation separation, View v) {
        // Add or modify the view
        if (v == null) {
            // Then add it to the view.
            v = getLayoutInflater().inflate(R.layout.separation_view, null);
            v.setTag(DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME);
            v.setOnClickListener(mSeparationOnClickListener);
            if (separation.type == RecipesDAO.Separation.INGREDIENT_TYPE) {
                v.setOnLongClickListener(mIngredientOnLongClickListener);
                mIngredientsLayout.addView(v, separation.number);
            }
            else {
                v.setOnLongClickListener(mStepOnLongClickListener);
                mStepsLayout.addView(v, separation.number);
            }
        }
        // Update text view information
        ((TextView) v.findViewById(R.id.id_view)).setText(String.valueOf(separation.id));
        ((TextView) v.findViewById(R.id.description_view)).setText(separation.description);
        v.invalidate();
    }

    private View.OnClickListener mSeparationOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int type = (mIngredientsLayout.indexOfChild(view) >= 0 ?
                    RecipesDAO.Separation.INGREDIENT_TYPE : RecipesDAO.Separation.STEP_TYPE);
            showAddEditSeparationDialog(type, view);
        }
    };

    //endregion

    //region ASSIGN_PICTURE

    public static final int PICK_IMAGE = 1;

    private View.OnClickListener mAddImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data.getData() != null) {
                Uri uri = data.getData();
                try {
                    mRecipeImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    setRecipeImage(Utils.getSquareBitmap(mRecipeImage, 512));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                if (data.getExtras() != null && data.getExtras().containsKey("data")) {
                    mRecipeImage = (Bitmap) data.getExtras().get("data");
                    if (mRecipeImage != null)
                        setRecipeImage(Utils.getSquareBitmap(mRecipeImage, 512));
                }
            }
        }
        else if (requestCode == IMPORT_FROM_INTERNET && resultCode == Activity.RESULT_OK) {
            Bundle bundle = data.getExtras();
            String url = bundle.getString(WebRecipeActivity.URL_EXTRA);
            importWebRecipeFromUrl(url);
        }
    }

    //endregion

    //region WEB_PAGE

    public static final int IMPORT_FROM_INTERNET = 2;

    /**
     * Import web recipe from an url.
     * For now, only urls from Marmiton are supported.
     * @param url The url were to find recipe's data.
     */
    public void importWebRecipeFromUrl(String url) {
        GetWebPageContentTask task = new GetWebPageContentTask();
        task.execute(url);
    }

    /**
     * Task getting the page content from an URL.
     */
    private class GetWebPageContentTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String ... urls) {
            String content = null;
            try {
                URL urlNet = new URL(urls[0]);
                URLConnection urlConnection = urlNet.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader br = new BufferedReader(inputStreamReader);
                StringBuilder contentBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = br.readLine()) != null)
                    contentBuilder.append(inputLine);
                br.close();
                content = contentBuilder.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return content;
        }

        @Override
        protected void onPostExecute(String result) {
            getRecipeFromContent(result);
        }
    }

    private class GetWebBitmapImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String ... urls) {
            try {
                URL url = new URL(urls[0]);
                URLConnection connection = url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                setRecipeImage(Utils.getSquareBitmap(result, 512));
            }
        }
    }

    /**
     * Get the recipe from content and fill the activity with this recipe in case of success.
     * @param content The web page content.
     */
    private void getRecipeFromContent(String content) {
        if (content == null || content.isEmpty())
            return;
        MarmitonManager.parseWebPageContent(content);
        // Recipe data
        RecipesDAO.Recipe recipe = MarmitonManager.getParsedRecipe();
        if (recipe != null)
            fillActivityWithRecipe(recipe);
        else
            Toast.makeText(this, getResources().getString(R.string.error_parsing_web_recipe),
                    Toast.LENGTH_LONG).show();
        // Download recipe's image
        String imageUrl = MarmitonManager.getParsedImageUrl();
        if (imageUrl != null) {
            GetWebBitmapImageTask task = new GetWebBitmapImageTask();
            task.execute(imageUrl);
        }
    }

    //endregion

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
