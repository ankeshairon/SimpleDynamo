package edu.buffalo.cse.cse486586.simpledynamo.responsecollectors;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;

public class QueryResponseCollector extends ResponseCollector {
    private MatrixCursor queryResultCursor;

    public QueryResponseCollector(String keyToQuery, int relativePosition, Boolean tryReplicasOnFail) {
        super(keyToQuery, relativePosition, tryReplicasOnFail);
        queryResultCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
    }

    @Override
    public void run() {
        final String[] messageContents = sendRequestAndGetMessageContents();

        if (areThereNoResultsToProcess(messageContents)) {
            return;
        }

        String[] row;
        for (int i = 0, queryResultsLength = messageContents.length; i < queryResultsLength; i += 2) {
            Log.w(TAG, "Data received : " + messageContents[i] + " ----> " + messageContents[i + 1]);
            row = new String[]{messageContents[i], messageContents[i + 1]};
            queryResultCursor.addRow(row);
        }
    }

    public Cursor getQueryResultCursor() {
        return queryResultCursor;
    }
}
