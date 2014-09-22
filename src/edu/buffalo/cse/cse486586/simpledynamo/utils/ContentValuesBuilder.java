package edu.buffalo.cse.cse486586.simpledynamo.utils;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.KEY_FIELD;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.VALUE_FIELD;

public class ContentValuesBuilder {

    public static ContentValues with(String key, String value) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_FIELD, key);
        contentValues.put(VALUE_FIELD, value);
        return contentValues;
    }

    public static List<ContentValues> getDataFrom(Cursor cursor) {
        List<ContentValues> result = new ArrayList<>();

        if (cursor != null && cursor.getCount() != 0) {
            int keyIndex = cursor.getColumnIndex(KEY_FIELD);
            int valueIndex = cursor.getColumnIndex(VALUE_FIELD);

            cursor.moveToFirst();
            do {
                result.add(with(cursor.getString(keyIndex), cursor.getString(valueIndex)));
            } while (cursor.moveToNext());
        }
        return result;
    }


    public static List<ContentValues> getDataFrom(String[] flattenedData) {
        List<ContentValues> result = new ArrayList<>();
        for (int i = 0; i < flattenedData.length; i = i + 2) {
            result.add(ContentValuesBuilder.with(flattenedData[i], flattenedData[i + 1]));
        }
        return result;
    }

}
