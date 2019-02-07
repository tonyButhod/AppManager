package buthod.tony.appManager.utils;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import buthod.tony.appManager.R;

/**
 * Custom spinner adapter which automatically resize to fit the selected item.
 */
public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private Context mContext;
    private String[] mValues;
    private int mResourceId, mDropdownResourceId;
    private int mMinWidth = 0;
    private boolean[] mViewIsGone = null;
    private int mTextColor;
    private Typeface mStyle;

    public CustomSpinnerAdapter(@NonNull Context context, @LayoutRes int resourceId, @NonNull String[] values) {
        this(context, resourceId, R.layout.simple_spinner_dropdown_item, values);
    }
    public CustomSpinnerAdapter(@NonNull Context context, @LayoutRes int resourceId,
                                @LayoutRes int dropdownResourceId, @NonNull String[] values) {
        super(context, resourceId, values);

        mContext = context;
        mValues = values;
        mResourceId = resourceId;
        mDropdownResourceId = R.layout.simple_spinner_dropdown_item;
        mViewIsGone = new boolean[mValues.length];
        mTextColor = mContext.getResources().getColor(R.color.dark_grey);
        mStyle = Typeface.DEFAULT;
    }

    @Override
    public void setDropDownViewResource(@LayoutRes int dropdownResourceId) {
        super.setDropDownViewResource(dropdownResourceId);
        mDropdownResourceId = dropdownResourceId;
    }

    @Override
    public View getView(final int position, final View convertView,
                        final ViewGroup parent) {
        int selectedItemPosition = position;
        // Return the view of the selected item for computation of the adapter width.
        // The spinner will then automatically resize.
        if (parent instanceof AdapterView) {
            selectedItemPosition = ((AdapterView) parent)
                    .getSelectedItemPosition();
        }
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(mContext).inflate(mResourceId, parent, false);
        }
        TextView textView = ((TextView) v.findViewById(R.id.text1));
        if (selectedItemPosition >= 0 && selectedItemPosition < mValues.length)
            textView.setText(mValues[selectedItemPosition]);
        textView.setMinimumWidth(mMinWidth);
        textView.setTextColor(mTextColor);
        textView.setTypeface(mStyle);

        return v;
    }

    @Override
    public View getDropDownView(final int position, final View convertView,
                                final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(mContext).inflate(mDropdownResourceId, parent, false);
        }
        ((TextView) v.findViewById(R.id.text1)).setText(mValues[position]);
        v.setVisibility(mViewIsGone[position] ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.text1).getLayoutParams().height =
                (mViewIsGone[position] ? 0 : ActionBar.LayoutParams.WRAP_CONTENT);

        return v;
    }

    /**
     * Set the min width of the text view spinner.
     * @param minWidth
     */
    public void setMinWidth(int minWidth) {
        mMinWidth = minWidth;
    }

    /**
     * For each position, the element at this position will be hide in the dropdown menu,
     * preventing the item selection.
     */
    public void setPositionsToHide(@NonNull List<Integer> positions) {
        for (int i = 0; i < mViewIsGone.length; ++i)
            mViewIsGone[i] = false;
        for (int j = 0; j < positions.size(); ++j)
            mViewIsGone[positions.get(j)] = true;
    }

    /**
     * Set the text color of the spinner view.
     */
    public void setTextColor(int color) {
        mTextColor = color;
    }

    /**
     * Set the text style of the spinner view.
     */
    public void setTextStyle(Typeface style) {
        mStyle = style;
    }
}