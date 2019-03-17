package buthod.tony.appManager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.LongSparseArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;

import buthod.tony.appManager.utils.Utils;

/**
 * Created by Tony on 29/07/2017.
 */

public class CheckListDAO extends DAOBase {

    public static final String
            GROUP_TABLE_NAME = DatabaseHandler.CHECK_LIST_GROUP_TABLE_NAME,
            GROUP_KEY = DatabaseHandler.CHECK_LIST_GROUP_KEY,
            GROUP_TITLE = DatabaseHandler.CHECK_LIST_GROUP_TITLE,
            GROUP_STATE = DatabaseHandler.CHECK_LIST_GROUP_STATE,
            GROUP_DATE = DatabaseHandler.CHECK_LIST_GROUP_DATE,
            ELEMENT_TABLE_NAME = DatabaseHandler.CHECK_LIST_ELEMENT_TABLE_NAME,
            ELEMENT_KEY = DatabaseHandler.CHECK_LIST_ELEMENT_KEY,
            ELEMENT_NAME = DatabaseHandler.CHECK_LIST_ELEMENT_NAME,
            ELEMENT_GROUP = DatabaseHandler.CHECK_LIST_ELEMENT_GROUP,
            ELEMENT_STATE = DatabaseHandler.CHECK_LIST_ELEMENT_STATE;

    public static class Group {
        public long id;
        public String title;
        public int state;
        public Date date;

        public Group() {
            id = -1;
            title = "";
            state = GroupState.Normal;
            date = null;
        }

        public Group(long id, String title, int state, Date date) {
            this.id = id;
            this.title = title;
            this.state = state;
            this.date = date;
        }
    }

    public static class GroupState {
        public static int
            Normal = 0,
            ToBeDeleted = 1,
            DoNoDelete = 2;
    }

    public static class Element {
        public long id;
        public String name;
        public long group;
        public int state;

        public Element() {
            id = -1;
            name = "";
            group = -1;
            state = ElementState.NotChecked;
        }

        public Element(long id, String name, long group, int state) {
            this.id = id;
            this.name = name;
            this.group = group;
            this.state = state;
        }
    }

    public static class ElementState {
        public static int
            NotChecked = 0,
            Checked = 1;
    }

    private SimpleDateFormat mDateFormatter = null;

    public CheckListDAO(Context context) {
        super(context);
        mDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
    }

    //region CHECK LIST GROUP

    public long addGroup(Group group) {
        ContentValues value = new ContentValues();
        value.put(GROUP_TITLE, group.title);
        value.put(GROUP_STATE, group.state);
        value.put(GROUP_DATE, mDateFormatter.format(group.date));
        return mDb.insert(GROUP_TABLE_NAME, null, value);
    }

    public void editGroup(Group group) {
        ContentValues value = new ContentValues();
        value.put(GROUP_KEY, group.id);
        value.put(GROUP_TITLE, group.title);
        value.put(GROUP_STATE, group.state);
        value.put(GROUP_DATE, mDateFormatter.format(group.date));
        mDb.update(GROUP_TABLE_NAME, value, GROUP_KEY + " = ?",
                new String[]{ String.valueOf(group.id) });
    }

    public void deleteGroup(long groupId) {
        mDb.delete(GROUP_TABLE_NAME, GROUP_KEY + " = ?",
                new String[] { String.valueOf(groupId) });
        mDb.delete(ELEMENT_TABLE_NAME, ELEMENT_GROUP + " = ?",
                new String[] { String.valueOf(groupId) });
    }

    public List<Group> getGroups() {
        Cursor c = mDb.rawQuery(
                "Select * From " + GROUP_TABLE_NAME + " Order by " + GROUP_DATE + " DESC;",
                new String[]{});
        List<Group> groups = new ArrayList<>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Group group = new Group(
                    c.getLong(c.getColumnIndex(GROUP_KEY)),
                    c.getString(c.getColumnIndex(GROUP_TITLE)),
                    c.getInt(c.getColumnIndex(GROUP_STATE)),
                    Utils.parseDate(mDateFormatter, c.getString(c.getColumnIndex(GROUP_DATE)))
            );
            groups.add(group);
        }
        c.close();
        return groups;
    }

    public Group getGroup(long groupId) {
        Cursor c = mDb.rawQuery(
                "Select * From " + GROUP_TABLE_NAME +
                " Where " + GROUP_KEY + "=?;", new String[]{ String.valueOf(groupId) });
        c.moveToFirst();
        Group group = null;
        if (!c.isAfterLast()) {
            group = new Group(
                    c.getLong(c.getColumnIndex(GROUP_KEY)),
                    c.getString(c.getColumnIndex(GROUP_TITLE)),
                    c.getInt(c.getColumnIndex(GROUP_STATE)),
                    Utils.parseDate(mDateFormatter, c.getString(c.getColumnIndex(GROUP_DATE))));
        }
        c.close();
        return group;
    }

    //endregion

    //region CHECK LIST ELEMENT

    public long addElement(Element element) {
        ContentValues value = new ContentValues();
        value.put(ELEMENT_NAME, element.name);
        value.put(ELEMENT_GROUP, element.group);
        value.put(ELEMENT_STATE, element.state);
        return mDb.insert(ELEMENT_TABLE_NAME, null, value);
    }

    public void editElement(Element element) {
        ContentValues value = new ContentValues();
        value.put(ELEMENT_KEY, element.id);
        value.put(ELEMENT_NAME, element.name);
        value.put(ELEMENT_GROUP, element.group);
        value.put(ELEMENT_STATE, element.state);
        mDb.update(ELEMENT_TABLE_NAME, value, ELEMENT_KEY + " = ?",
                new String[]{ String.valueOf(element.id) });
    }

    public void deleteElement(long elementId) {
        mDb.delete(ELEMENT_TABLE_NAME, ELEMENT_KEY + " = ?",
                new String[] { String.valueOf(elementId) });
    }

    public List<Element> getElementsInGroup(long groupId) {
        Cursor c = mDb.rawQuery(
                String.format(Locale.getDefault(), "Select * From %s Where %s = ?;",
                        ELEMENT_TABLE_NAME, ELEMENT_GROUP),
                new String[] { String.valueOf(groupId) });
        List<Element> elementsInGroup = new ArrayList<>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Element element = new Element(
                    c.getLong(c.getColumnIndex(ELEMENT_KEY)),
                    c.getString(c.getColumnIndex(ELEMENT_NAME)),
                    c.getLong(c.getColumnIndex(ELEMENT_GROUP)),
                    c.getInt(c.getColumnIndex(ELEMENT_STATE))
            );
            elementsInGroup.add(element);
        }
        c.close();
        return elementsInGroup;
    }

    //endregion

    //region ADVANCED

    /**
     * Get the number of unchecked elements for each group.
     * Groups with no unchecked elements are not present in the array.
     * @return Array with group id -> number of unchecked elements.
     */
    public LongSparseArray<Integer> getUncheckedElementsPerGroup() {
        LongSparseArray<Integer> uncheckedElements = new LongSparseArray<>();
        Cursor c = mDb.rawQuery(
                String.format(Locale.getDefault(),
                        "Select %3$s.%1$s, COUNT(%4$s.%2$s) From %3$s, %4$s" +
                        " Where %4$s.%5$s = %3$s.%1$s And %4$s.%6$s = ?" +
                        " Group By %3$s.%1$s;",
                        GROUP_KEY, ELEMENT_KEY, GROUP_TABLE_NAME, ELEMENT_TABLE_NAME,
                        ELEMENT_GROUP, ELEMENT_STATE),
                new String[] { String.valueOf(ElementState.NotChecked) }
        );
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            uncheckedElements.append(c.getLong(0), c.getInt(1));
        }
        c.close();
        return uncheckedElements;
    }

    /**
     * Set all elements of a group with unchecked state.
     * @param groupId The group id to update elements.
     * @return The number of elements updated.
     */
    public int setAllElementsUncheckedForGroup(long groupId) {
        ContentValues values = new ContentValues();
        values.put(ELEMENT_STATE, ElementState.NotChecked);
        return mDb.update(ELEMENT_TABLE_NAME, values, ELEMENT_GROUP + "=?",
                new String[] { String.valueOf(groupId) });
    }

    //endregion

    //region LOAD_SAVE_EXTERNAL_STORAGE

    // No need to save a check list to external storage

    //endregion
}
