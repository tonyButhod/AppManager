package buthod.tony.appManager.checkList;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.database.CheckListDAO;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.utils.Utils;

/**
 * Activity showing check list groups.
 */
public class CheckListElementsActivity extends RootActivity {

    private static String LineSeparator = System.getProperty("line.separator");

    private Handler mHandler = new Handler();

    private CheckListDAO mDao = null;
    private CheckListDAO.Group mGroup = new CheckListDAO.Group();

    private EditText mGroupTitleView;
    private LinearLayout mElementsLayout;

    private List<ElementView> mElementViewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_list_elements_activity);
        mDao = new CheckListDAO(this);
        mDao.open();
        mGroup.id = getIntent().getLongExtra(DatabaseHandler.CHECK_LIST_GROUP_KEY, -1);

        // Buttons listeners
        findViewById(R.id.back_button).setOnClickListener(mBackClickListener);
        findViewById(R.id.restore_group_button).setOnClickListener(mRestoreClickListener);
        mGroupTitleView = (EditText) findViewById(R.id.group_title);
        mGroupTitleView.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(DatabaseHandler.CheckListTitleLength)
        });
        // Layout
        mElementsLayout = (LinearLayout) findViewById(R.id.elements_layout);
        new Thread(new Runnable() {
            @Override
            public void run() {
                populateActivityWithGroup(mGroup.id);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }

    /**
     * Populate the activity with the group and its elements.
     * @param groupId The group id.
     */
    private void populateActivityWithGroup(long groupId) {
        mGroup = mDao.getGroup(groupId);
        final List<CheckListDAO.Element> elements = mDao.getElementsInGroup(groupId);
        mGroupTitleView.post(new Runnable() {
            @Override
            public void run() {
                mGroupTitleView.setText(mGroup.title);
                mGroupTitleView.addTextChangedListener(mTitleChangedListener);
                if (mGroup.title.isEmpty() && elements.size() == 0) {
                    mGroupTitleView.requestFocus();
                }
            }
        });
        mElementsLayout.post(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = getLayoutInflater();
                for (int i = 0; i < elements.size(); ++i) {
                    ElementView elementView = new ElementView(elements.get(i), inflater);
                    mElementsLayout.addView(elementView.getView());
                }
                // Also add an empty element to add a new one.
                addEmptyElementView(inflater);
            }
        });
    }

    /**
     * Add an empty element view that allows the user to add an element.
     */
    private ElementView addEmptyElementView(LayoutInflater inflater) {
        CheckListDAO.Element addElement = new CheckListDAO.Element();
        addElement.group = mGroup.id;
        ElementView elementView = new ElementView(addElement, inflater);
        mElementsLayout.addView(elementView.getView());
        return elementView;
    }

    private TextWatcher mTitleChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // New change detected, thus remove the previous callback and set a new one.
            mHandler.removeCallbacks(setTitleRunnable);
            mHandler.postDelayed(setTitleRunnable, 1000);
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    /**
     * Delete the check list if empty without title.
     */
    private View.OnClickListener mBackClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mGroupTitleView.getText().toString().isEmpty()
                    && mElementViewList.size() <= 1) {
                mDao.deleteGroup(mGroup.id);
            }
            finish();
        }
    };

    /**
     * Listener when the restore button is checked.
     */
    private View.OnClickListener mRestoreClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Utils.showConfirmDeleteDialog(CheckListElementsActivity.this,
                    R.string.restore_group_confirmation, new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < mElementViewList.size(); ++i) {
                                mElementViewList.get(i).mCheckBox.setChecked(false);
                            }
                            mDao.setAllElementsUncheckedForGroup(mGroup.id);
                        }
                    });
        }
    };

    /**
     * Set the title of the group.
     * It updates changes in database using another thread.
     */
    private Runnable setTitleRunnable = new Runnable() {
        @Override
        public void run() {
            String newTitle = mGroupTitleView.getText().toString();
            if (newTitle.compareTo(mGroup.title) != 0) {
                mGroup.title = newTitle;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mDao.editGroup(mGroup);
                    }
                }).start();
            }
        }
    };

    /**
     * Inner class used to easily manage elements views and data.
     */
    private class ElementView {

        private CheckListDAO.Element mElementData;
        private View mElementView;
        private EditText mNameEdit;
        private CheckBox mCheckBox;
        private View mDeleteButton;

        public ElementView(CheckListDAO.Element element, LayoutInflater inflater) {
            mElementData = element;
            mElementView = inflater.inflate(R.layout.check_list_element_view, null);
            mNameEdit = (EditText) mElementView.findViewById(R.id.element_name);
            mNameEdit.setFilters(new InputFilter[] {
                    new InputFilter.LengthFilter(DatabaseHandler.CheckListElementNameLength)
            });
            mCheckBox = (CheckBox) mElementView.findViewById(R.id.element_checkbox);
            mDeleteButton = mElementView.findViewById(R.id.delete_element_button);
            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.showConfirmDeleteDialog(CheckListElementsActivity.this,
                            R.string.delete_confirmation, mDeleteElementRunnable);
                }
            });
            // First update with default values.
            update(element);
            // Then add listeners on change.
            mNameEdit.addTextChangedListener(mOnTextChanged);
            mCheckBox.setOnCheckedChangeListener(mOnCheckBoxChanged);
            // Add the element view to the list of all elements.
            mElementViewList.add(this);
        }

        /**
         * Update the view with element's data.
         */
        private void update(CheckListDAO.Element element) {
            mNameEdit.setText(element.name);
            mCheckBox.setChecked(element.state == CheckListDAO.ElementState.Checked);
            mCheckBox.setVisibility(element.id >= 0 ? View.VISIBLE : View.INVISIBLE);
            mDeleteButton.setVisibility(element.id >= 0 ? View.VISIBLE : View.INVISIBLE);
        }

        /**
         * Get the created view.
         */
        public View getView() {
            return mElementView;
        }

        /**
         * Request the focus of this element.
         * It request the focus on the edit text for element's name.
         */
        public void requestFocus() {
            mNameEdit.requestFocus();
        }

        public boolean isChecked() {
            return mCheckBox.isChecked();
        }

        /**
         * Text watcher on text change for element's name.
         */
        private TextWatcher mOnTextChanged = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mHandler.removeCallbacks(mSetNameRunnable);
                final String text = charSequence.toString();
                if (text.contains(LineSeparator)) {
                    // Remove the line separator without triggering onTextChanged
                    String newName = text.replace(LineSeparator, "");
                    mNameEdit.removeTextChangedListener(this);
                    mNameEdit.setText(newName);
                    mNameEdit.addTextChangedListener(this);
                    // Start set name runnable immediately.
                    mSetNameRunnable.run();
                    // Request focus for the next element
                    int index = mElementViewList.indexOf(ElementView.this);
                    if (index >= 0 && index + 1 < mElementViewList.size()) {
                        mElementViewList.get(index + 1).requestFocus();
                    }
                }
                else {
                    mHandler.postDelayed(mSetNameRunnable, 1000);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        /**
         * Set the name of the element.
         * If the element is a new one, adds an empty element view and adds the previous to database.
         */
        private Runnable mSetNameRunnable = new Runnable() {
            @Override
            public void run() {
                mElementData.name = mNameEdit.getText().toString();
                if (mElementData.id < 0) {
                    // Add a new element view and add this element to database.
                    addEmptyElementView(getLayoutInflater());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mElementData.id = mDao.addElement(mElementData);
                            // Put visible check box and remove button once added to database.
                            mElementView.post(new Runnable() {
                                @Override
                                public void run() {
                                    mCheckBox.setVisibility(View.VISIBLE);
                                    mDeleteButton.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }).start();
                }
                else {
                    // Edit the element
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mDao.editElement(mElementData);
                        }
                    }).start();
                }
            }
        };

        /**
         * Listener on checked change for element's state.
         */
        private CompoundButton.OnCheckedChangeListener mOnCheckBoxChanged = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mElementData.state = (b ?
                        CheckListDAO.ElementState.Checked : CheckListDAO.ElementState.NotChecked);
                mDao.editElement(mElementData);
            }
        };

        /**
         * Runnable for the deletion of an element.
         */
        private Runnable mDeleteElementRunnable = new Runnable() {
            @Override
            public void run() {
                mDao.deleteElement(mElementData.id);
                mElementsLayout.removeView(mElementView);
                mElementViewList.remove(ElementView.this);
            }
        };
    }
}
