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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import buthod.tony.appManager.utils.CustomAlertDialog;
import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.utils.CustomSpinnerAdapter;
import buthod.tony.appManager.utils.Utils;
import buthod.tony.appManager.database.RecipesDAO;

/**
 * Created by Tony on 14/12/2018.
 */
public class IngredientsManagementActivity extends RootActivity {

    private static final int MIN_SPINNER_WIDTH = 50;

    private RecipesDAO mDao = null;

    private LinearLayout mIngredientsList;
    private LongSparseArray<View> mIngredientViews;
    private LongSparseArray<RecipesDAO.IngredientConversions> mConversions;
    private EditText mSearchField = null;

    private String[] mUnits;
    private CustomSpinnerAdapter mUnitsFromAdapter, mUnitsToAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ingredients_management_activity);

        mDao = new RecipesDAO(getBaseContext());
        mDao.open();

        mIngredientsList = (LinearLayout) findViewById(R.id.ingredients_list);
        mIngredientViews = new LongSparseArray<>();
        mConversions = new LongSparseArray<>();
        mSearchField = (EditText) findViewById(R.id.search_field);

        Resources res = getResources();
        mUnits = res.getStringArray(R.array.units_array);
        mUnitsFromAdapter = new CustomSpinnerAdapter(this,
                R.layout.simple_spinner_item,
                R.layout.simple_spinner_dropdown_item,
                mUnits);
        mUnitsToAdapter = new CustomSpinnerAdapter(this,
                R.layout.simple_spinner_item,
                R.layout.simple_spinner_dropdown_item,
                mUnits);
        mUnitsFromAdapter.setMinWidth(MIN_SPINNER_WIDTH);
        mUnitsToAdapter.setMinWidth(MIN_SPINNER_WIDTH);

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
        mConversions.clear();

        LayoutInflater inflater = getLayoutInflater();
        List<RecipesDAO.IngredientConversions> ingredients = mDao.getIngredientsWithConversions();
        for (int i = 0; i < ingredients.size(); ++i) {
            RecipesDAO.IngredientConversions ingredientConversions = ingredients.get(i);
            mConversions.append(ingredientConversions.ingredientId, ingredientConversions);
            // Create the ingredient view.
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
                addEditConversionView(ingredientConversions.ingredientId, conversion, null);
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
        mUnitFromSpinnerDialog.setAdapter(mUnitsFromAdapter);
        mUnitToSpinnerDialog = (Spinner) alertView.findViewById(R.id.unit_to_spinner);
        mUnitToSpinnerDialog.setAdapter(mUnitsToAdapter);
        final EditText factorEdit = (EditText) alertView.findViewById(R.id.factor_view);
        final TextView errorView = (TextView) alertView.findViewById(R.id.error_message);
        // Set default values if ingredient is not null
        if (v != null) {
            mUnitFromSpinnerDialog.setSelection(mUnitsFromAdapter.getPosition(
                    ((TextView)v.findViewById(R.id.unit_from_view)).getText().toString()));
            mUnitToSpinnerDialog.setSelection(mUnitsToAdapter.getPosition(
                    ((TextView)v.findViewById(R.id.unit_to_view)).getText().toString()));
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
                            deleteConversion(ingredientId, id);
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
                        else if (conversion.unitFrom == 0 || conversion.unitTo == 0)
                            errorMessage = res.getString(R.string.conversion_unit_error);
                        else if (id == -1 && conversionAlreadyExists(ingredientId, conversion))
                            errorMessage = res.getString(R.string.conversion_already_exists_error);
                        if (!errorMessage.isEmpty()) {
                            // Ingredient data are not valid
                            errorView.setText(errorMessage);
                            errorView.setVisibility(View.VISIBLE);
                            return;
                        }
                        // Add or edit the conversion
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

    private void addEditConversionView(long ingredientId, RecipesDAO.Conversion conversion, View v) {
        // Update view and fields.
        if (v == null) {
            LayoutInflater inflater = getLayoutInflater();
            v = inflater.inflate(R.layout.conversion_view, null);
            ((LinearLayout) mIngredientViews.get(ingredientId).findViewById(R.id.conversions_list)).addView(v);
            v.setOnClickListener(mOnConversionClickListener);
        }
        // Edit the view
        ((TextView) v.findViewById(R.id.id_conversion)).setText(String.valueOf(conversion.id));
        ((TextView) v.findViewById(R.id.unit_from_view)).setText(mUnits[conversion.unitFrom]);
        ((TextView) v.findViewById(R.id.unit_to_view)).setText(mUnits[conversion.unitTo]);
        ((TextView) v.findViewById(R.id.factor_view)).setText(Utils.floatToString(conversion.factor));
    }

    private void addEditConversion(long ingredientId, RecipesDAO.Conversion conversion, View v) {
        boolean isNew = (conversion.id == -1);
        conversion.id = mDao.addEditConversion(ingredientId, conversion);
        // Update the view
        addEditConversionView(ingredientId, conversion, v);
        // Update private field
        if (isNew) {
            // Add the conversion in private field.
            mConversions.get(ingredientId).conversions.add(conversion);
        }
        else {
            // Update values in field
            List<RecipesDAO.Conversion> conversions = mConversions.get(ingredientId).conversions;
            for (int i = 0; i < conversions.size(); ++i)
                if (conversions.get(i).id == conversion.id) {
                    conversions.get(i).unitTo = conversion.unitTo;
                    conversions.get(i).unitFrom = conversion.unitFrom;
                    conversions.get(i).factor = conversion.factor;
                }
        }
    }

    private void deleteConversion(long ingredientId, long conversionId) {
        if (conversionId != -1)
            mDao.deleteConversion(conversionId);
        List<RecipesDAO.Conversion> conversions =  mConversions.get(ingredientId).conversions;
        for (int i = 0; i < conversions.size(); i++)
            if (conversions.get(i).id == conversionId) {
                conversions.remove(i);
                break;
            }
    }

    private Spinner.OnItemSelectedListener mOnConversionUnitSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            // An item is selected in the unit from spinner.
            ArrayList<Integer> indexesToRemove = new ArrayList<>();
            if (i == UnitsConversion.g || i == UnitsConversion.kg) {
                indexesToRemove.add(UnitsConversion.g);
                indexesToRemove.add(UnitsConversion.kg);
            }
            else if (i == UnitsConversion.cL || i == UnitsConversion.L) {
                indexesToRemove.add(UnitsConversion.cL);
                indexesToRemove.add(UnitsConversion.L);
            }
            else if (i != 0) { // The default value
                indexesToRemove.add(i);
            }
            // Set positions to hide in unit to spinner.
            mUnitsToAdapter.setPositionsToHide(indexesToRemove);
            // If the selected item is hidden, set selection to default.
            if (indexesToRemove.contains(mUnitToSpinnerDialog.getSelectedItemPosition())) {
                mUnitToSpinnerDialog.setSelection(0);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    /**
     * Check if a conversion of the same kind already exists.
     */
    private boolean conversionAlreadyExists(long ingredientId, RecipesDAO.Conversion conversion) {
        boolean alreadyExists = false;
        List<RecipesDAO.Conversion> conversions = mConversions.get(ingredientId).conversions;
        for (int i = 0; i < conversions.size(); ++i) {
            RecipesDAO.Conversion c = conversions.get(i);
            if ((UnitsConversion.isSameMeasurementType(c.unitFrom, conversion.unitFrom)
                    && UnitsConversion.isSameMeasurementType(c.unitTo, conversion.unitTo))
                || (UnitsConversion.isSameMeasurementType(c.unitFrom, conversion.unitTo)
                    && UnitsConversion.isSameMeasurementType(c.unitTo, conversion.unitFrom))) {
                alreadyExists = true;
                break;
            }
        }
        return alreadyExists;
    }

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
