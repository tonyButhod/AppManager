package buthod.tony.appManager.recipes;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import buthod.tony.appManager.R;
import buthod.tony.appManager.database.RecipesDAO;
import buthod.tony.appManager.utils.CustomSpinnerAdapter;
import buthod.tony.appManager.utils.Utils;

/**
 * Ingredient view management class.
 * Facilitates the the display of an ingredient and its quantity in a recipe.
 * It provides methods to update the quantity automatically
 * depending on the number of people and conversions.
 */
public class IngredientViewManagement {

    private static String[] mUnits = null;
    private static String mOptionalLabel = null;
    private static String mAdditionalOptionalLabel = null;

    private RelativeLayout mRootLayout;
    private TextView mQuantityView;
    private TextView mUnitView;
    private Spinner mUnitSpinner;
    private TextView mIngredientName;
    private TextView mOptionalView;

    private int mDefaultUnitId, mCurrentUnitId;
    private float mDefaultQuantity, mCurrentQuantityFactor;
    private List<RecipesDAO.Conversion> mConversions;
    private float mAdditionalOptionalQuantity;

    /**
     * Main constructor inflating a new ingredient view.
     */
    public IngredientViewManagement(Activity activity) {
        if (mUnits == null) {
            Resources res = activity.getResources();
            mUnits = res.getStringArray(R.array.units_array);
            mOptionalLabel = res.getString(R.string.optional_label_parenthesis);
            mAdditionalOptionalLabel = res.getString(R.string.among_x_optional);
        }

        mRootLayout = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.ingredient_view, null);
        mQuantityView = (TextView) mRootLayout.findViewById(R.id.quantity_view);
        mUnitView = (TextView) mRootLayout.findViewById(R.id.unit_view);
        mUnitSpinner = (Spinner) mRootLayout.findViewById(R.id.spinner_unit_view);
        mUnitSpinner.setOnItemSelectedListener(mOnUnitSelectedListener);
        mIngredientName = (TextView) mRootLayout.findViewById(R.id.ingredient_name_view);
        mOptionalView = (TextView) mRootLayout.findViewById(R.id.optional_view);
        mOptionalView.setText(mOptionalLabel);
    }

    /**
     * Set the ingredient data to use in the view.
     */
    public void setIngredient(RecipesDAO.Ingredient ingredient) {
        mQuantityView.setText(Utils.floatToString(ingredient.quantity));
        mUnitView.setText(mUnits[ingredient.idUnit]);
        mUnitView.setVisibility(View.VISIBLE);
        mUnitSpinner.setVisibility(View.GONE);
        mIngredientName.setText(ingredient.name);
        mOptionalView.setVisibility(ingredient.type == RecipesDAO.Ingredient.OPTIONAL_TYPE ?
            View.VISIBLE : View.GONE);
        mOptionalView.setText(mOptionalLabel);

        mDefaultUnitId = ingredient.idUnit;
        mCurrentUnitId = ingredient.idUnit;
        mDefaultQuantity = ingredient.quantity;
        mAdditionalOptionalQuantity = 0;
        mCurrentQuantityFactor = 1;
        mConversions = null;
    }

    /**
     * Set conversions for this ingredient.
     * It changes the text view to spinner view for the unit,
     * and add a listener to update the quantity once another unit is selected.
     */
    public void setConversion(Context context, List<RecipesDAO.Conversion> conversions) {
        mConversions = conversions;
        // Populate the units spinner
        mUnitView.setVisibility(View.GONE);
        mUnitSpinner.setVisibility(View.VISIBLE);
        ArrayList<String> valuesList = new ArrayList<>();
        int defaultSelectedIndex = 0;
        for (int i = 1; i < mUnits.length; ++i) {
            if (UnitsConversion.convert(conversions, mDefaultUnitId, i) > 0)
                valuesList.add(mUnits[i]);
            if (i == mDefaultUnitId)
                defaultSelectedIndex = (valuesList.size() - 1);
        }
        String[] values = new String[valuesList.size()];
        valuesList.toArray(values);
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(context,
                R.layout.simple_spinner_item, R.layout.simple_spinner_dropdown_item, values);
        adapter.setTextColor(context.getResources().getColor(R.color.colorAccent));
        mUnitSpinner.setAdapter(adapter);
        mUnitSpinner.setSelection(defaultSelectedIndex);
    }

    /**
     * Set the quantity factor.
     * Default quantity factor is 1.
     */
    public void setQuantityFactor(float factor) {
        mCurrentQuantityFactor = factor;
        updateQuantityValue();
    }

    /**
     * Returns the view inflated in the constructor.
     */
    public View getView() {
        return mRootLayout;
    }

    /**
     * Add an optional quantity to the ingredient.
     */
    public void addOptionalQuantity(float quantity) {
        mAdditionalOptionalQuantity += quantity;
        updateQuantityValue();
    }

    /**
     * Listener when an unit is selected in the spinner.
     */
    private AdapterView.OnItemSelectedListener mOnUnitSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            String selectedUnit = (String) mUnitSpinner.getSelectedItem();
            int unitId;
            for (unitId = 0; unitId < mUnits.length; ++unitId)
                if (mUnits[unitId].equals(selectedUnit))
                    break;
            if (unitId < mUnits.length) {
                mCurrentUnitId = unitId;
                updateQuantityValue();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            // Do nothing
        }
    };

    /**
     * Update the quantity view using private fields.
     */
    private void updateQuantityValue() {
        float conversionFactor = UnitsConversion.convert(mConversions, mDefaultUnitId, mCurrentUnitId);
        float newQuantity = mDefaultQuantity * mCurrentQuantityFactor * conversionFactor;
        float newOptionalQuantity = mAdditionalOptionalQuantity * mCurrentQuantityFactor * conversionFactor;
        mQuantityView.setText(Utils.floatToString(newQuantity + newOptionalQuantity, 4));
        if (newOptionalQuantity != 0) {
            mOptionalView.setText(String.format(mAdditionalOptionalLabel,
                    Utils.floatToString(newOptionalQuantity, 4) + mUnits[mCurrentUnitId]));
            mOptionalView.setVisibility(View.VISIBLE);
        }
    }
}
