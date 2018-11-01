package buthod.tony.appManager.recipes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

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
    private Button mAddIngredientButton = null, mAddStepButton = null;
    private ImageView mRecipeImageView = null;
    private ImageButton mAddImageButton = null, mDeleteImageButton = null,
            mRotateLeftButton = null, mRotateRightButton = null;
    private TextView mErrorView = null;

    private ArrayAdapter<CharSequence>
            mRecipeTypes = null,
            mUnits = null,
            mIngredientsNames = null;

    // Fields containing recipes_activity information
    private long mRecipeId = -1; // If -1, new recipe_activity, otherwise, recipe_activity modified
    private ArrayList<RecipesDAO.Ingredient> mIngredients;
    private ArrayList<RecipesDAO.Step> mSteps;
    private ArrayList<Long> mIngredientsToDelete, mStepsToDelete;
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
        mPeopleEdit = (EditText) findViewById(R.id.people);
        mIngredientsLayout = (LinearLayout) findViewById(R.id.ingredients_list);
        mStepsLayout = (LinearLayout) findViewById(R.id.steps_list);
        mAddIngredientButton = (Button) findViewById(R.id.add_ingredient_button);
        mAddStepButton = (Button) findViewById(R.id.add_step_button);
        mAddImageButton = (ImageButton) findViewById(R.id.add_image);
        mRecipeImageView = (ImageView) findViewById(R.id.image_view);
        mDeleteImageButton = (ImageButton) findViewById(R.id.delete_image);
        mRotateLeftButton = (ImageButton) findViewById(R.id.rotate_left);
        mRotateRightButton = (ImageButton) findViewById(R.id.rotate_right);
        mErrorView = (TextView) findViewById(R.id.error_message);
        // Private lists
        mIngredients = new ArrayList<>();
        mSteps = new ArrayList<>();
        mIngredientsToDelete = new ArrayList<>();
        mStepsToDelete = new ArrayList<>();
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
                finish();
            }
        });
        // Add or edit recipe_activity
        mAddRecipeButton.setText(getResources().getString(
                mRecipeId == -1 ? R.string.add : R.string.modify));
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
        // Buttons to add ingredients or steps
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
        // Get all ingredients stored on database
        mIngredientsNames.addAll(mDao.getIngredients());

        // Put previous values if we are modifying a recipe_activity
        if (mRecipeId != -1) {
            fillActivityWithRecipe();
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
    }

    /**
     * Fill the activity with the given recipe_activity id.
     */
    private void fillActivityWithRecipe() {
        RecipesDAO.Recipe recipe = mDao.getRecipe(mRecipeId);
        mRecipeName.setText(recipe.name);
        mRecipeTypeSpinner.setSelection(recipe.type);
        mDifficultySeekBar.setProgress(recipe.difficulty - 1);
        mGradeSeekBar.setProgress(recipe.grade - 1);
        mTimeEdit.setText(String.valueOf(recipe.time));
        mPeopleEdit.setText(String.valueOf(recipe.people));
        for (int i = 0; i < recipe.ingredients.size(); ++i) {
            addEditIngredient(recipe.ingredients.get(i), -1);
        }
        for (int i = 0; i < recipe.steps.size(); ++i) {
            addEditStep(recipe.steps.get(i), -1);
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

    /**
     * Update steps number depending on their position in the list.
     */
    private void updateStepsNumber() {
        for (int i = 0; i < mSteps.size(); ++i)
            mSteps.get(i).number = i + 1;
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
            recipe.ingredients = mIngredients;
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
            // Otherwise, add/edit the recipe
            updateStepsNumber();
            recipe.steps = mSteps;
            mRecipeId = mDao.addEditRecipe(recipe);
            mDao.deleteQuantities(mIngredientsToDelete);
            mDao.deleteSteps(mStepsToDelete);
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

    //region ADD_EDIT_INGREDIENT

    /**
     * Show the dialog to add or modify an ingredient.
     * @param ingredientIndex The ingredient index in array. If new ingredient, put -1.
     *                           Otherwise modify a previous ingredient added.
     */
    private void showAddEditIngredientDialog(final int ingredientIndex) {
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
        final AutoCompleteTextView ingredientView = (AutoCompleteTextView) alertView.findViewById(R.id.ingredient);
        final TextView errorView = (TextView) alertView.findViewById(R.id.error_message);
        ingredientView.setAdapter(mIngredientsNames);
        // Set default values if ingredient is not null
        if (ingredientIndex != -1) {
            RecipesDAO.Ingredient ingredient = mIngredients.get(ingredientIndex);
            quantity.setText(Utils.floatToString(ingredient.quantity));
            unitSpinner.setSelection(ingredient.idUnit);
            ingredientView.setText(ingredient.name);
        }
        // Set up dialog buttons
        final Resources res = getResources();
        if (ingredientIndex != -1)
            builder.setNeutralButton(res.getString(R.string.delete),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mIngredientsLayout.removeViewAt(ingredientIndex);
                            RecipesDAO.Ingredient ingredient = mIngredients.remove(ingredientIndex);
                            if (ingredient.idQuantity != -1)
                                mIngredientsToDelete.add(ingredient.idQuantity);
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
        builder.setPositiveButton(
                ingredientIndex == -1 ? res.getString(R.string.add) : res.getString(R.string.modify),
                null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        // Get data from views
                        RecipesDAO.Ingredient ingredient =
                                (ingredientIndex == -1 ? new RecipesDAO.Ingredient() : mIngredients.get(ingredientIndex));
                        ingredient.idUnit = unitSpinner.getSelectedItemPosition();
                        ingredient.name = ingredientView.getText().toString();
                        ingredient.quantity = Utils.parseFloatWithDefault(quantity.getText().toString(), -1);
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
                        addEditIngredient(ingredient, ingredientIndex);
                        alertDialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }
    private void showAddEditIngredientDialog() { showAddEditIngredientDialog(-1);}

    /**
     * Add or modify an ingredient.
     * @param ingredient The ingredient to add or modify.
     * @param index The index of the ingredient. If -1, add the ingredient.
     *              Otherwise, modify it with the given index.
     */
    private void addEditIngredient(final RecipesDAO.Ingredient ingredient, int index) {
        // First check if the ingredient name is already in database, otherwise need to add it.
        ingredient.idIngredient = mDao.getIngredientId(ingredient.name);
        // Add or modify the view
        View ingredientView;
        if (index == -1) {
            // Add the ingredient in the list of ingredients for when the user really add the recipe_activity.
            mIngredients.add(ingredient);
            // Then add it to the view.
            ingredientView = getLayoutInflater().inflate(R.layout.ingredient_view, null);
            ingredientView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int ingredientPosition = mIngredientsLayout.indexOfChild(view);
                    showAddEditIngredientDialog(ingredientPosition);
                }
            });
            mIngredientsLayout.addView(ingredientView, mIngredientsLayout.getChildCount());
        }
        else {
            // Get the existing view
            ingredientView = mIngredientsLayout.getChildAt(index);
        }
        // Update text view information
        ((TextView) ingredientView.findViewById(R.id.quantity_view)).setText(String.valueOf(ingredient.quantity));
        ((TextView) ingredientView.findViewById(R.id.unit_view)).setText(mUnits.getItem(ingredient.idUnit));
        ((TextView) ingredientView.findViewById(R.id.ingredient_name_view)).setText(ingredient.name);
        ingredientView.invalidate();
    }

    //endregion

    //region ADD_EDIT_STEP

    /**
     * Show the dialog to add or modify a step.
     * @param stepIndex The step index in array. If new step, put -1.
     *                  Otherwise modify a previous ingredient added.
     */
    private void showAddEditStepDialog(final int stepIndex) {
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
        if (stepIndex != -1) {
            descriptionView.setText(mSteps.get(stepIndex).description);
        }
        // Set up dialog buttons
        final Resources res = getResources();
        if (stepIndex != -1)
            builder.setNeutralButton(res.getString(R.string.delete),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mStepsLayout.removeViewAt(stepIndex);
                            RecipesDAO.Step step = mSteps.remove(stepIndex);
                            if (step.id != -1)
                                mStepsToDelete.add(step.id);
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
        builder.setPositiveButton(
                stepIndex == -1 ? res.getString(R.string.add) : res.getString(R.string.modify),
                null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        // Recover data from views
                        RecipesDAO.Step step =
                                (stepIndex == -1 ? new RecipesDAO.Step() : mSteps.get(stepIndex));
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
                        addEditStep(step, stepIndex);
                        alertDialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }
    private void showAddEditStepDialog() { showAddEditStepDialog(-1);}

    /**
     * Add or modify a step.
     * @param step The step to add or modify.
     * @param index The index of the step. If -1, add the step.
     *              Otherwise, modify it with the given index.
     */
    private void addEditStep(final RecipesDAO.Step step, int index) {
        // Add or modify the view
        final View stepView;
        if (index == -1) {
            // Add the ingredient in the list of ingredients for when the user really add the recipe_activity.
            mSteps.add(step);
            // Then add it to the view.
            stepView = getLayoutInflater().inflate(R.layout.step_view, null);
            stepView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int stepPosition = mStepsLayout.indexOfChild(view);
                    showAddEditStepDialog(stepPosition);
                }
            });
            stepView.setOnLongClickListener(mStepOnLongClickListener);
            mStepsLayout.addView(stepView, mStepsLayout.getChildCount());
        }
        else {
            // Get the existing view
            stepView = mStepsLayout.getChildAt(index);
        }
        // Update text view information
        ((TextView) stepView.findViewById(R.id.description_view)).setText(step.description);
        stepView.invalidate();
    }

    private View.OnLongClickListener mStepOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            mDragAndDrop.StartDragging(mStepsLayout, view,
                    new DragAndDropLinearLayout.OnDragAndDropListener() {
                @Override
                public void onDrop(int dragIndex, int dropIndex) {
                    RecipesDAO.Step dragStep = mSteps.remove(dragIndex);
                    mSteps.add(dropIndex, dragStep);
                }
            });
            return true;
        }
    };

    //endregion

    //region ASSIGN_PICTURE

    public static int PICK_IMAGE = 1;

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
                setRecipeImage((Bitmap) data.getExtras().get("data"));
            }
        }
    }

    //endregion

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
