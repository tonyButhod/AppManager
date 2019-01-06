package buthod.tony.appManager.recipes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import buthod.tony.appManager.CustomAlertDialog;
import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.Utils;
import buthod.tony.appManager.database.RecipesDAO;

/**
 * Created by Tony on 14/12/2018.
 */

public class IngredientsManagementActivity extends RootActivity {

    private RecipesDAO mDao = null;

    private LinearLayout mIngredientsList;
    private LongSparseArray<View> mIngredientViews;
    private EditText mSearchField = null;

    private String[] mUnits;
    private ArrayAdapter<CharSequence> mUnitsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ingredients_management_activity);

        mDao = new RecipesDAO(getBaseContext());
        mDao.open();

        mIngredientsList = (LinearLayout) findViewById(R.id.ingredients_list);
        mIngredientViews = new LongSparseArray<>();
        mSearchField = (EditText) findViewById(R.id.search_field);

        Resources res = getResources();
        mUnits = res.getStringArray(R.array.units_array);
        mUnitsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mUnitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mUnitsAdapter.addAll(mUnits);

        // Finish the activity if back button is pressed
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Update search elements
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onSearchFieldChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        populateListWithIngredients();
    }

    private void populateListWithIngredients() {
        mIngredientsList.removeAllViews();
        mIngredientViews.clear();

        LayoutInflater inflater = getLayoutInflater();
        List<RecipesDAO.IngredientConversions> ingredients = mDao.getIngredientsWithConversions();
        for (int i = 0; i < ingredients.size(); ++i) {
            RecipesDAO.IngredientConversions ingredientConversions = ingredients.get(i);
            View v = inflater.inflate(R.layout.ingredient_management_view, null);
            ((TextView) v.findViewById(R.id.ingredient_id)).setText(String.valueOf(ingredientConversions.ingredientId));
            ((TextView) v.findViewById(R.id.ingredient_name)).setText(ingredientConversions.ingredientName);
            LinearLayout conversionsList = (LinearLayout) v.findViewById(R.id.conversions_list);
            View addConversionButton = v.findViewById(R.id.add_conversion_button);
            // Set the ingredient name as tag for search and add it to layout.
            v.setTag(ingredientConversions.ingredientName);
            mIngredientsList.addView(v);
            mIngredientViews.append(ingredientConversions.ingredientId, v);
            // Hide the conversion list and the add button by default and display it on click
            conversionsList.setVisibility(View.GONE);
            conversionsList.setScaleY(0);
            addConversionButton.setAlpha(0f);
            v.findViewById(R.id.ingredient_layout).setOnClickListener(mOnIngredientClickListener);
            addConversionButton.setOnClickListener(mOnAddConversionListener);
            for (int j = 0; j < ingredientConversions.conversions.size(); ++j) {
                RecipesDAO.Conversion conversion = ingredientConversions.conversions.get(j);
                addEditConversion(ingredientConversions.ingredientId, conversion, null);
            }
        }
    }

    //region OPEN/CLOSE INGREDIENT CONVERSIONS

    /** The animation duration on click on ingredients **/
    private long mAnimationDuration = 150;

    private View mPreviousClickedIngredient = null;

    private View.OnClickListener mOnIngredientClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            View currentView = (View)view.getParent();
            if (mPreviousClickedIngredient == currentView)
                currentView = null; // Deselect the current view
            else
                toggleIngredientState(currentView);
            if (mPreviousClickedIngredient != null)
                toggleIngredientState(mPreviousClickedIngredient);
            mPreviousClickedIngredient = currentView;
        }
    };

    /**
     * Open/close an ingredient view.
     * @param v The ingredient layout.
     */
    private void toggleIngredientState(View v) {
        final View conversionList = v.findViewById(R.id.conversions_list);
        View addConversionButton = v.findViewById(R.id.add_conversion_button);
        if (conversionList.getVisibility() != View.VISIBLE) {
            // Display the conversion list and the add button
            conversionList.animate().scaleY(1).setDuration(mAnimationDuration).withStartAction(new Runnable() {
                @Override
                public void run() {
                    conversionList.setVisibility(View.VISIBLE);
                }
            }).start();
            addConversionButton.animate().alpha(1f).setDuration(mAnimationDuration).start();
        }
        else {
            // Hide them back
            conversionList.animate().scaleY(0).setDuration(mAnimationDuration).withStartAction(new Runnable() {
                @Override
                public void run() {
                    conversionList.setVisibility(View.GONE);
                }
            }).start();
            addConversionButton.animate().alpha(0f).setDuration(mAnimationDuration).start();
        }
    }

    //endregion

    //region ADD_EDIT_CONVERSION

    private Spinner mUnitFromSpinnerDialog = null, mUnitToSpinnerDialog = null;

    private View.OnClickListener mOnConversionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Get the ingredient id from view
            View ingredientIdView = ((RelativeLayout) view.getParent().getParent()).findViewById(R.id.ingredient_id);
            long ingredientId = Long.parseLong(((TextView) ingredientIdView).getText().toString());
            showAddEditConversionDialog(ingredientId, view);
        }
    };

    private View.OnClickListener mOnAddConversionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Get the ingredient id from view
            View ingredientIdView = ((RelativeLayout) view.getParent().getParent()).findViewById(R.id.ingredient_id);
            long ingredientId = Long.parseLong(((TextView) ingredientIdView).getText().toString());
            showAddEditConversionDialog(ingredientId);
        }
    };

    /**
     * Show the dialog to add or modify a conversion.
     * @param v The conversion view. If new conversion, then v is null.
     */
    private void showAddEditConversionDialog(final long ingredientId, final View v) {
        final long id = (v == null ? -1 :
                Long.parseLong(((TextView)v.findViewById(R.id.id_conversion)).getText().toString()));
        // Initialize an alert dialog
        CustomAlertDialog.Builder builder = new CustomAlertDialog.Builder(this);
        builder.setTitle(R.string.add_conversion);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_edit_conversion, null);
        builder.setView(alertView);
        // Get and set useful widget
        mUnitFromSpinnerDialog = (Spinner) alertView.findViewById(R.id.unit_from_spinner);
        mUnitFromSpinnerDialog.setAdapter(mUnitsAdapter);
        mUnitToSpinnerDialog = (Spinner) alertView.findViewById(R.id.unit_to_spinner);
        mUnitToSpinnerDialog.setAdapter(mUnitsAdapter);
        final EditText factorEdit = (EditText) alertView.findViewById(R.id.factor_view);
        final TextView errorView = (TextView) alertView.findViewById(R.id.error_message);
        // Set default values if ingredient is not null
        if (v != null) {
            mUnitFromSpinnerDialog.setSelection(mUnitsAdapter.getPosition(
                    ((TextView)v.findViewById(R.id.unit_from_view)).getText()));
            mUnitToSpinnerDialog.setSelection(mUnitsAdapter.getPosition(
                    ((TextView)v.findViewById(R.id.unit_to_view)).getText()));
            factorEdit.setText(((TextView)v.findViewById(R.id.factor_view)).getText());
        }
        // Updates available units in spinners when one is selected.
        // It prevents for example to save a conversion between g and kg since it is already coded in the app.
        mUnitFromSpinnerDialog.setOnItemSelectedListener(mOnConversionUnitSelectedListener);
        // Set up dialog buttons
        final Resources res = getResources();
        if (v != null)
            builder.setNeutralButton(res.getString(R.string.delete),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((LinearLayout) v.getParent()).removeView(v);
                            if (id >= 0)
                                mDao.deleteConversion(id);
                            mUnitFromSpinnerDialog = null;
                            mUnitToSpinnerDialog = null;
                            dialog.cancel();
                        }
                    });
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mUnitFromSpinnerDialog = null;
                        mUnitToSpinnerDialog = null;
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(v == null ? res.getString(R.string.add) : res.getString(R.string.modify), null);
        final CustomAlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Get data from views
                        RecipesDAO.Conversion conversion = new RecipesDAO.Conversion();
                        conversion.id = id;
                        conversion.unitFrom = mUnitFromSpinnerDialog.getSelectedItemPosition();
                        conversion.unitTo = mUnitToSpinnerDialog.getSelectedItemPosition();
                        conversion.factor = Utils.parseFloatWithDefault(factorEdit.getText().toString(), 0);
                        // Check ingredient data
                        String errorMessage = "";
                        if (conversion.factor <= 0)
                            errorMessage = res.getString(R.string.conversion_factor_error);
                        if (!errorMessage.isEmpty()) {
                            // Ingredient data are not valid
                            errorView.setText(errorMessage);
                            errorView.setVisibility(View.VISIBLE);
                            return;
                        }
                        // Add or modify the ingredient
                        addEditConversion(ingredientId, conversion, v);
                        mUnitFromSpinnerDialog = null;
                        mUnitToSpinnerDialog = null;
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }
    private void showAddEditConversionDialog(long ingredientId) { showAddEditConversionDialog(ingredientId, null);}

    private void addEditConversion(long ingredientId, RecipesDAO.Conversion conversion, View v) {
        if (v == null) {
            LayoutInflater inflater = getLayoutInflater();
            v = inflater.inflate(R.layout.conversion_view, null);
            ((LinearLayout) mIngredientViews.get(ingredientId).findViewById(R.id.conversions_list)).addView(v);
            v.setOnClickListener(mOnConversionClickListener);
        }
        ((TextView) v.findViewById(R.id.id_conversion)).setText(String.valueOf(conversion.id));
        ((TextView) v.findViewById(R.id.unit_from_view)).setText(mUnits[conversion.unitFrom]);
        ((TextView) v.findViewById(R.id.unit_to_view)).setText(mUnits[conversion.unitTo]);
        ((TextView) v.findViewById(R.id.factor_view)).setText(Utils.floatToString(conversion.factor));
        mDao.addEditConversion(ingredientId, conversion);
    }

    private Spinner.OnItemSelectedListener mOnConversionUnitSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            // An item is selected in the unit from spinner.
            ArrayList<Integer> indexesToRemove = new ArrayList<>();
            if (i == 1 || i == 2) {
                indexesToRemove.add(1);
                indexesToRemove.add(2);
            }
            else if (i == 3 || i == 4) {
                indexesToRemove.add(3);
                indexesToRemove.add(4);
            }
            else if (i != 0) { // The default value
                indexesToRemove.add(i);
            }
            // Build the new adapter
            String[] newValues = new String[mUnits.length - indexesToRemove.size()];
            int index = 0;
            for (int j = 0; j < mUnits.length; ++j) {
                if (!indexesToRemove.contains(j))
                    newValues[index++] = mUnits[j];
            }
            ArrayAdapter<CharSequence> newAdapter= new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item);
            newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            newAdapter.addAll(newValues);
            // Try to keep the same item selected in unit to.
            String selectedItem = mUnitToSpinnerDialog.getSelectedItem().toString();
            mUnitToSpinnerDialog.setAdapter(newAdapter);
            for (int j = 0; j < newValues.length; ++j)
                if (newValues[j].equals(selectedItem)) {
                    mUnitToSpinnerDialog.setSelection(j);
                    break;
                }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    //endregion

    //region SEARCH

    private Handler handler = new Handler();

    /**
     * Called when the user changes the string to search.
     * @param searchString The new string to search.
     */
    private void onSearchFieldChanged(final String searchString) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (searchString.compareTo(mSearchField.getText().toString()) == 0) {
                    for (int i = 0; i < mIngredientsList.getChildCount(); ++i) {
                        View ingredientView = mIngredientsList.getChildAt(i);
                        String ingredientName = (String)ingredientView.getTag();
                        if (Utils.searchInString(searchString, ingredientName)) {
                            ingredientView.setVisibility(View.VISIBLE);
                        }
                        else {
                            ingredientView.setVisibility(View.GONE);
                        }
                    }
                }
            }
        }, 400);
    }

    //endregion

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
