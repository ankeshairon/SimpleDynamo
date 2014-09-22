package edu.buffalo.cse.cse486586.simpledynamo.dataaccumulator;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.model.CustomLock;
import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;
import edu.buffalo.cse.cse486586.simpledynamo.responsecollectors.RecoveryResponseCollector;
import edu.buffalo.cse.cse486586.simpledynamo.utils.ContentValuesBuilder;

import java.util.List;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;

public class CrashRecoveryDataAccumulator extends DataAccumulator implements Runnable {

    private final CustomLock hasServerStartedLock;
//    private final CustomLock hasFinishedRecovery;

    public CrashRecoveryDataAccumulator(CustomLock hasServerStartedLock, CustomLock hasFinishedRecovery) {
        this.hasServerStartedLock = hasServerStartedLock;
//        this.hasFinishedRecovery = hasFinishedRecovery;
    }

    @Override
    public void run() {
        Log.w(TAG, "Entering crash recover data accumulator...");
        synchronized (hasServerStartedLock) {
            if (!hasServerStartedLock.hasConditionMet()) {
                Log.w(TAG, "Waiting for server thread to start up..");
                hasServerStartedLock.waitOnThis(TAG, "Interrupted exception thrown while waiting for server to start");
            }
        }

        final CustomLock haveOldRecordsBeenRetrieved = new CustomLock(false);

        final RecoveryResponseCollector[] responseCollectors = new RecoveryResponseCollector[]{
                new RecoveryResponseCollector(KEYS_IN_RESPONDER_STRICT_SCOPE, -2, haveOldRecordsBeenRetrieved),
                new RecoveryResponseCollector(KEYS_IN_RESPONDER_STRICT_SCOPE, -1, haveOldRecordsBeenRetrieved),
                new RecoveryResponseCollector(KEYS_IN_REQUESTER_STRICT_SCOPE, 1, haveOldRecordsBeenRetrieved),
                new RecoveryResponseCollector(KEYS_IN_REQUESTER_STRICT_SCOPE, 2, haveOldRecordsBeenRetrieved)
        };

        Log.w(TAG, "Sending out data recovery requests");
        Thread[] threads = startThreads(responseCollectors);

        Log.w(TAG, "Deleting all local records");
        //delete all records
//        ResourceManager.contentResolver.delete(RECOVERY_URI, null, null);

        final Cursor allLocalRecordsCursor = ResourceManager.contentResolver.query(RECOVERY_URI, null, null, null, null);
        final List<ContentValues> allLocalRecords = ContentValuesBuilder.getDataFrom(allLocalRecordsCursor);
//        final int keyColumnIndex = allLocalRecordsCursor.getColumnIndex(KEY_FIELD);

        for (RecoveryResponseCollector responseCollector : responseCollectors) {
            responseCollector.setExistingRecords(allLocalRecords);
//            responseCollector.setKeyIndex(keyColumnIndex);
        }

        synchronized (haveOldRecordsBeenRetrieved) {
            Log.w(TAG, "Finished retrieving old records");
            haveOldRecordsBeenRetrieved.setHasConditionMet(true);
            haveOldRecordsBeenRetrieved.notifyAll();
        }
        joinOnThreads(threads);

        Log.w(TAG, "Finished recovery");
//        synchronized (hasFinishedRecovery) {
//            hasFinishedRecovery.setHasConditionMet(true);
//            hasFinishedRecovery.notifyAll();
//        }
    }
}
