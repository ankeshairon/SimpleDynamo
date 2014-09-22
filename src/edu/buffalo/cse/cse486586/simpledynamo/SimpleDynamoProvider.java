package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.communicator.ServerThread;
import edu.buffalo.cse.cse486586.simpledynamo.constants.Operation;
import edu.buffalo.cse.cse486586.simpledynamo.dataaccumulator.CrashRecoveryDataAccumulator;
import edu.buffalo.cse.cse486586.simpledynamo.databasehelper.CustomSQLiteOpenHelper;
import edu.buffalo.cse.cse486586.simpledynamo.model.CustomLock;
import edu.buffalo.cse.cse486586.simpledynamo.operationshandler.ClientOperationsHandler;
import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.chordManager;

public class SimpleDynamoProvider extends ContentProvider {


    private static final UriMatcher URI_MATCHER = new UriMatcher(1);

    private CustomSQLiteOpenHelper sqLiteOpenHelper;
    private SQLiteDatabase db;

    private ClientOperationsHandler clientOperationsHandler;
    private ServerThread serverThread;

    private CustomLock hasFinishedRecovery;

    @Override
    public boolean onCreate() {
        URI_MATCHER.addURI(HOME_AUTHORITY, TABLE_NAME, 1);
        TAG += getMyPort();
        sqLiteOpenHelper = new CustomSQLiteOpenHelper(getContext(), DB_NAME, null, 1);

        CustomLock hasServerStarted = new CustomLock(false);
        serverThread = new ServerThread(hasServerStarted);
        serverThread.start();

        clientOperationsHandler = new ClientOperationsHandler();
        new ResourceManager(getContext().getContentResolver(), getMyPort());
        hasFinishedRecovery = new CustomLock(false);
        new Thread(new CrashRecoveryDataAccumulator(hasServerStarted, hasFinishedRecovery)).start();
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        getDbIfNull();

        final String key = values.getAsString(KEY_FIELD);
        final Object value = values.get(VALUE_FIELD);

        if (INTERNAL_URI.equals(uri) || TESTER_URI.equals(uri)) {
            makeTesterWaitForRecoveryToFinish(uri);
            clientOperationsHandler.sendRequest(Operation.INSERT, key, value);
            if (!chordManager.isInMyExtendedScope(key)) {
                return null;
            }
        } else if (!(EXTERNAL_URI.equals(uri) || RECOVERY_URI.equals(uri))) {
            Log.e(TAG, "Unsupported URI request received for insert - " + uri.toString());
            throw new UnsupportedOperationException("Unsupported URI request received for insert - " + uri.toString());
        }

        db.execSQL("REPLACE INTO content (key, value) VALUES ('" + key + "','" + value + "')");

        Log.d("insert", values.toString());
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        getDbIfNull();

        if (EXTERNAL_URI.equals(uri)) {
            if (ALL_LOCAL.equals(selection)) {
                selection = null;
            }
        } else if (INTERNAL_URI.equals(uri) || TESTER_URI.equals(uri)) {
            makeTesterWaitForRecoveryToFinish(uri);
            switch (selection) {
                case ALL_IN_CHORD:
                    clientOperationsHandler.sendRequest(Operation.DELETE, ALL_IN_CHORD, null);
                    selection = null;
                    break;
                case ALL_LOCAL:
                    selection = null;
                    break;
                default:
                    clientOperationsHandler.sendRequest(Operation.DELETE, selection, null);
                    if (!chordManager.isInMyExtendedScope(selection)) {
                        return 0;
                    }
                    break;
            }
        } else if (!RECOVERY_URI.equals(uri)) {
            Log.e(TAG, "Unsupported URI request received for delete - " + uri.toString());
            throw new UnsupportedOperationException("Unsupported URI request received for delete - " + uri.toString());
        }

        if (selection != null) {
            Log.v("delete", selection);
        } else {
            Log.v("delete", "null");
        }
        String sql = "DELETE FROM " + TABLE_NAME;
        if (selection != null) {
            sql += " where key = '" + selection + "'";
        }
        db.execSQL(sql);
        return 1;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        getDbIfNull();

        if (EXTERNAL_URI.equals(uri)) {
            if (ALL_LOCAL.equals(selection)) {
                selection = null;
            } else {
                selection = KEY_FIELD + "='" + selection + "'";
            }
        } else if (INTERNAL_URI.equals(uri) || TESTER_URI.equals(uri)) {
            makeTesterWaitForRecoveryToFinish(uri);
            switch (selection) {
                case ALL_IN_CHORD:
                    return clientOperationsHandler.sendQueryRequest(ALL_IN_CHORD);
                case ALL_LOCAL:
                    selection = null;
                    break;
                default:
                    if (!chordManager.isInMyExtendedScope(selection)) {
                        return clientOperationsHandler.sendQueryRequest(selection);
                    } else {
                        selection = KEY_FIELD + "='" + selection + "'";
                    }
                    break;
            }
        } else if (!RECOVERY_URI.equals(uri)) {
            Log.e(TAG, "Unsupported URI request received for query - " + uri.toString());
            throw new UnsupportedOperationException("Unsupported URI request received for query - " + uri.toString());
        }

        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        qBuilder.setTables(TABLE_NAME);

        Cursor resultCursor = qBuilder.query(db, projection, selection, null, null, null, null);
        if (resultCursor != null) {
            resultCursor.setNotificationUri(getContext().getContentResolver(), TESTER_URI);
        }
        return resultCursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private void getDbIfNull() {
        if (db == null) {
            db = sqLiteOpenHelper.getWritableDatabase();
        }
    }

    private int getMyPort() {
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        return Integer.parseInt(portStr) * 2;
    }


    public void makeTesterWaitForRecoveryToFinish(Uri uri) {
//        if (TESTER_URI.equals(uri)) {
//            synchronized (hasFinishedRecovery) {
//                while (!hasFinishedRecovery.hasConditionMet()) {
//                    hasFinishedRecovery.waitOnThis(TAG, "Waiting for node to finish recovering");
//                }
//                hasFinishedRecovery.notifyAll();
//            }
//        }
    }

}
