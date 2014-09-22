package edu.buffalo.cse.cse486586.simpledynamo.dataaccumulator;

import android.database.Cursor;
import android.database.MergeCursor;
import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;
import edu.buffalo.cse.cse486586.simpledynamo.responsecollectors.QueryResponseCollector;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;

public class QueryDataAccumulator extends DataAccumulator {

    public Cursor getCollectedData() {
        QueryResponseCollector responseCollectors[] = new QueryResponseCollector[]{
                new QueryResponseCollector(KEYS_IN_RESPONDER_STRICT_SCOPE, 1, false),
                new QueryResponseCollector(KEYS_IN_RESPONDER_STRICT_SCOPE, 2, false)
        };

        final Thread[] threads = startThreads(responseCollectors);

        final Cursor localResultsCursor = ResourceManager.contentResolver.query(INTERNAL_URI, null, ALL_LOCAL, null, null);
        final Cursor[] queryResultCursors = new Cursor[responseCollectors.length + 1];

        joinOnThreads(threads);

        int i = 0;
        for (; i < responseCollectors.length; i++) {
            queryResultCursors[i] = responseCollectors[i].getQueryResultCursor();
        }
        queryResultCursors[i] = localResultsCursor;

        return new MergeCursor(queryResultCursors);
    }
}