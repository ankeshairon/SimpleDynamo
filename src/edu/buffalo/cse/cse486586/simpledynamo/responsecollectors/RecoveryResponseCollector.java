package edu.buffalo.cse.cse486586.simpledynamo.responsecollectors;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.model.CustomLock;
import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;
import edu.buffalo.cse.cse486586.simpledynamo.utils.ContentValuesBuilder;

import java.util.ArrayList;
import java.util.List;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;

public class RecoveryResponseCollector extends ResponseCollector {
    private final CustomLock haveOldRecordsBeenRetrived;
    private volatile List<ContentValues> existingRecords;

    public RecoveryResponseCollector(String keyToQuery, int relativePosition, CustomLock haveOldRecordsBeenRetrived) {
        super(keyToQuery, relativePosition, false);
        this.haveOldRecordsBeenRetrived = haveOldRecordsBeenRetrived;
    }

    @Override
    public void run() {
        final String[] messageContents = sendRequestAndGetMessageContents();

        if (areThereNoResultsToProcess(messageContents)) {
            return;
        }

        List<ContentValues> receivedRecordsToBeAdded = ContentValuesBuilder.getDataFrom(messageContents);

        synchronized (haveOldRecordsBeenRetrived) {
            if (!haveOldRecordsBeenRetrived.hasConditionMet()) {
                haveOldRecordsBeenRetrived.waitOnThis(TAG, "Interrupted exception thrown while waiting for local @ query to be made");
            }
        }

        if (keyToQuery.equals(KEYS_IN_REQUESTER_STRICT_SCOPE) || keyToQuery.equals(KEYS_IN_RESPONDER_STRICT_SCOPE)) {
            //asking for my data from successors - their entries take preference
            List<ContentValues> existingRecordsToBeDeleted = new ArrayList<>(existingRecords);
            existingRecordsToBeDeleted.removeAll(receivedRecordsToBeAdded);

            receivedRecordsToBeAdded.removeAll(existingRecords);


            ArrayList<ContentProviderOperation> allOperations = new ArrayList<>();
            ContentProviderOperation operation;

            Log.w(TAG, "Recovery - Preparing records to be deleted");
            for (ContentValues contentValues : existingRecordsToBeDeleted) {
                operation = ContentProviderOperation
                        .newDelete(RECOVERY_URI)
                        .withSelection(KEY_FIELD + " = ?", new String[]{contentValues.getAsString(KEY_FIELD)})
                        .build();
                allOperations.add(operation);
            }
            Log.w(TAG, "Recovery - Preparing records to be inserted");
            for (ContentValues contentValues : receivedRecordsToBeAdded) {
                operation = ContentProviderOperation.newInsert(RECOVERY_URI).withValues(contentValues).build();
                allOperations.add(operation);
            }
            Log.w(TAG, "Starting batch operations");
            try {
                ResourceManager.contentResolver.applyBatch(HOME_AUTHORITY, allOperations);
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            final String errorMsg = "Can't filter data for key - " + keyToQuery;
            Log.e(TAG, errorMsg);
            throw new RuntimeException(errorMsg);
        }


//        ResourceManager.contentResolver.bulkInsert(INTERNAL_URI, receivedRecordsToBeAdded);
    }

    public void setExistingRecords(List<ContentValues> existingRecords) {
        this.existingRecords = existingRecords;
    }


}
