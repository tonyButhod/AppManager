package buthod.tony.appManager.checkList;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.InflaterInputStream;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.database.CheckListDAO;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.utils.Utils;

/**
 * Activity showing check list groups.
 */
public class CheckListGroupsActivity extends RootActivity {

    private static int DaysNumberBeforeGroupDeletion = 7;

    private Drawable mEmptyStar, mStarFilled;
    private CheckListDAO mDao = null;
    private Resources mRes;
    private LinearLayout mNormalGroupsLayout, mToDeleteGroupsLayout, mStaredGroupsLayout;

    private List<GroupView> mGroupViewList = new ArrayList<>();
    private LongSparseArray<Integer> mUncheckedElementPerGroup = new LongSparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_list_groups_activity);
        mDao = new CheckListDAO(this);
        mDao.open();
        mRes = getResources();
        mEmptyStar = getResources().getDrawable(R.drawable.star_empty);
        mStarFilled = getResources().getDrawable(R.drawable.star_filled);

        // Buttons listeners
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        findViewById(R.id.add_group_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckListDAO.Group newGroup = new CheckListDAO.Group();
                newGroup.title = "";
                newGroup.state = CheckListDAO.GroupState.Normal;
                newGroup.date = Calendar.getInstance().getTime();
                newGroup.id = mDao.addGroup(newGroup);
                Intent intent = new Intent(view.getContext(), CheckListElementsActivity.class);
                intent.putExtra(DatabaseHandler.CHECK_LIST_GROUP_KEY, newGroup.id);
                startActivity(intent);
            }
        });
        // Layout
        mNormalGroupsLayout = (LinearLayout) findViewById(R.id.normal_groups_layout);
        mToDeleteGroupsLayout = (LinearLayout) findViewById(R.id.to_delete_groups_layout);
        mStaredGroupsLayout = (LinearLayout) findViewById(R.id.stared_groups_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                populateActivityWithGroups();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }

    //region POPULATE

    /**
     * Populate the activity with all groups.
     */
    private void populateActivityWithGroups() {
        final List<CheckListDAO.Group> groups = mDao.getGroups();
        // Process groups
        deleteUnusedGroups(groups);
        // Update groups state
        updateGroupsState(groups);
        // Add views
        mNormalGroupsLayout.post(new Runnable() {
            @Override
            public void run() {
                mNormalGroupsLayout.removeAllViews();
                mToDeleteGroupsLayout.removeAllViews();
                mStaredGroupsLayout.removeAllViews();
                LayoutInflater inflater = getLayoutInflater();
                for (int i = 0; i < groups.size(); ++i) {
                    GroupView groupView = new GroupView(groups.get(i), inflater);
                    groupView.setUncheckedElementsNumber(
                            mUncheckedElementPerGroup.get(groups.get(i).id, 0));
                }
            }
        });
    }

    /**
     * Delete groups that are considered unused (depending on their state and date).
     */
    private void deleteUnusedGroups(List<CheckListDAO.Group> groups) {
        int i = 0;
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long maxTime = DaysNumberBeforeGroupDeletion * 86400000; // Number of milliseconds in 1 day
        while (i < groups.size()) {
            CheckListDAO.Group group = groups.get(i);
            if (group.state == CheckListDAO.GroupState.ToBeDeleted
                    && (currentTime - group.date.getTime()) > maxTime) {
                mDao.deleteGroup(group.id);
                groups.remove(i);
            }
            else {
                i++;
            }
        }
    }

    /**
     * Update groups state depending on their state and the number of unchecked elements.
     */
    private void updateGroupsState(List<CheckListDAO.Group> groups) {
        // Get the number of unchecked element per group
        mUncheckedElementPerGroup = mDao.getUncheckedElementsPerGroup();

        for (int i = 0; i < groups.size(); ++i) {
            CheckListDAO.Group group = groups.get(i);
            if (group.state == CheckListDAO.GroupState.DoNoDelete) {
                // Nothing to do
            }
            else if (group.state == CheckListDAO.GroupState.Normal) {
                // If all elements are checked, change its state to ToBeDeleted
                if (mUncheckedElementPerGroup.get(group.id, 0) == 0) {
                    group.state = CheckListDAO.GroupState.ToBeDeleted;
                    group.date = new Date();
                    mDao.editGroup(group);
                }
            }
        }
    }

    //endregion

    /**
     * Inner class used to easily manage groups views and data.
     */
    private class GroupView {

        private CheckListDAO.Group mGroupData;
        private View mGroupView;
        private TextView mTitleView;
        private ImageButton mDeleteButton, mRestoreButton, mStarButton;

        public GroupView(CheckListDAO.Group group, LayoutInflater inflater) {
            mGroupData = group;
            mGroupView = inflater.inflate(R.layout.check_list_group_view, null);
            mTitleView = (TextView) mGroupView.findViewById(R.id.group_title);
            mTitleView.setOnClickListener(mOnGroupClickListener);
            mDeleteButton = (ImageButton) mGroupView.findViewById(R.id.delete_group_button);
            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.showConfirmDeleteDialog(CheckListGroupsActivity.this,
                            R.string.delete_confirmation, mDeleteGroupRunnable);
                }
            });
            mRestoreButton = (ImageButton) mGroupView.findViewById(R.id.restore_group_button);
            mRestoreButton.setOnClickListener(mRestoreGroupListener);
            mStarButton = (ImageButton) mGroupView.findViewById(R.id.star_button);
            mStarButton.setOnClickListener(mStarGroupListener);
            // Update the view content
            update();
            // Add the view to the list of groups and layout.
            mGroupViewList.add(this);
            if (mGroupData.state == CheckListDAO.GroupState.Normal) {
                mNormalGroupsLayout.addView(mGroupView);
            }
            else if (mGroupData.state == CheckListDAO.GroupState.ToBeDeleted) {
                mToDeleteGroupsLayout.addView(mGroupView);
            }
            else if (mGroupData.state == CheckListDAO.GroupState.DoNoDelete) {
                mStaredGroupsLayout.addView(mGroupView);
            }
        }

        /**
         * Update the view content with group data.
         */
        private void update() {
            mTitleView.setText(mGroupData.title);
            boolean isToBeDeleted = mGroupData.state == CheckListDAO.GroupState.ToBeDeleted;
            boolean isStared = mGroupData.state == CheckListDAO.GroupState.DoNoDelete;
            mTitleView.setTextColor(mRes.getColor(isToBeDeleted ? R.color.dark_grey : R.color.colorPrimary));
            mTitleView.setTypeface(null, isToBeDeleted ? Typeface.ITALIC : Typeface.BOLD);
            mStarButton.setBackground(isStared ? mStarFilled : mEmptyStar);
            mStarButton.setVisibility(isToBeDeleted ? View.INVISIBLE : View.VISIBLE);
            mDeleteButton.setVisibility(isToBeDeleted ? View.GONE : View.VISIBLE);
            mRestoreButton.setVisibility(isToBeDeleted ? View.VISIBLE : View.GONE);
        }

        /**
         * Set the number of unchecked elements in the groups.
         */
        public void setUncheckedElementsNumber(int number) {
            ((TextView) mGroupView.findViewById(R.id.unchecked_elements_number)).setText(
                    String.valueOf(number));
        }

        /**
         * Listener called once the group is clicked.
         */
        private View.OnClickListener mOnGroupClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGroupData.state != CheckListDAO.GroupState.ToBeDeleted) {
                    Intent intent = new Intent(view.getContext(), CheckListElementsActivity.class);
                    intent.putExtra(DatabaseHandler.CHECK_LIST_GROUP_KEY, mGroupData.id);
                    startActivity(intent);
                }
            }
        };

        /**
         * Runnable to execute to delete a group.
         */
        private Runnable mDeleteGroupRunnable = new Runnable() {
            @Override
            public void run() {
                mDao.deleteGroup(mGroupData.id);
                ((LinearLayout)mGroupView.getParent()).removeView(mGroupView);
            }
        };

        /**
         * Listener that restore a group that is to be deleted.
         * It puts its state back to normal and resets all elements inside as unchecked.
         */
        private View.OnClickListener mRestoreGroupListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGroupData.state = CheckListDAO.GroupState.Normal;
                changeGroupViewLayout(mNormalGroupsLayout);
                mGroupData.date = new Date();
                update();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mDao.editGroup(mGroupData);
                        final int elementsNumber = mDao.setAllElementsUncheckedForGroup(mGroupData.id);
                        mUncheckedElementPerGroup.put(mGroupData.id, elementsNumber);
                        mGroupView.post(new Runnable() {
                            @Override
                            public void run() {
                                setUncheckedElementsNumber(elementsNumber);
                            }
                        });
                    }
                }).start();
            }
        };

        /**
         * Listener called once the star is toggle.
         * It changes the group's state to DoNotDelete or back to normal.
         */
        private View.OnClickListener mStarGroupListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGroupData.state == CheckListDAO.GroupState.DoNoDelete) {
                    mGroupData.state = CheckListDAO.GroupState.Normal;
                    changeGroupViewLayout(mNormalGroupsLayout);
                }
                else {
                    mGroupData.state = CheckListDAO.GroupState.DoNoDelete;
                    changeGroupViewLayout(mStaredGroupsLayout);
                }
                mGroupData.date = new Date();
                update();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mDao.editGroup(mGroupData);
                    }
                }).start();
            }
        };

        /**
         * Change the parent layout of the group view.
         */
        public void changeGroupViewLayout(LinearLayout layout) {
            ((LinearLayout)mGroupView.getParent()).removeView(mGroupView);
            layout.addView(mGroupView, 0);
        }

        /**
         * Get the created view.
         */
        public View getView() {
            return mGroupView;
        }

        /**
         * Get group data.
         */
        public CheckListDAO.Group getGroupData() {
            return mGroupData;
        }
    }
}
