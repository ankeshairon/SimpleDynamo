package edu.buffalo.cse.cse486586.simpledynamo.operationshandler;

import android.database.Cursor;
import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.dataaccumulator.QueryDataAccumulator;
import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;
import edu.buffalo.cse.cse486586.simpledynamo.responsecollectors.QueryResponseCollector;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Operation.*;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.communicator;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.requestFactory;

public class ClientOperationsHandler {

//    private int requestType;
//    private String value;
//    private String key;


    /**
     * for insert/delete operations
     */
//    public ClientOperationsHandler(ResourceManager resourceManager, int requestType, String key, String value) {
//        super(resourceManager);
//        this.requestType = requestType;
//        this.value = value;
//        this.key = key;
//    }

    /**
     * for insert/delete operations
     */
//    @Override
//    public void run() {
//        sendRequest(requestType, key, value);
//    }

    /**
     * for query operations
     */
    public Cursor sendRequest(String requestType, String key, Object value) {
        switch (requestType) {
            case INSERT:
                sendInsertRequest(key, value);
                return null;
            case DELETE:
                sendDeleteRequest(key);
                return null;
            case QUERY:
                //done to allow decoupling of query from insert/delete to allow space for addn of Async task for insert/delete
                throw new UnsupportedOperationException("Error! Not retrieving query results");
            default:
                throw new UnsupportedOperationException("Unsupported operation type " + requestType);
        }
    }

    // operationId--originalRequesterIndex--key---value
    private void sendInsertRequest(String key, Object value) {
        Log.w(TAG, "Forwarding insert request for key - " + key);
        String request = requestFactory.createRequest(INSERT, myIndex, key, value);
        communicator.forwardRequestToPortsWhoseExtendedScopeContains(request, key, false);
    }

    // operationId--original requester index---key
    private void sendDeleteRequest(String key) {
        Log.w(TAG, "Forwarding delete request for key - " + key);
        String request;

        if (ALL_IN_CHORD.equals(key)) {
            request = requestFactory.createRequest(DELETE, myIndex, ALL_LOCAL);
            communicator.forwardRequestToAllPorts(request, false);
        } else {
            request = requestFactory.createRequest(DELETE, myIndex, key);
            communicator.forwardRequestToPortsWhoseExtendedScopeContains(request, key, false);
        }
    }

    //    only query requests for  * and keys in other avds
    //handle query requests if node fails

    //operation---requester index ---request id---key
    public Cursor sendQueryRequest(String key) {
        if (ALL_IN_CHORD.equals(key)) {
            Log.w(TAG, "Forwarding query request for key @ to all nodes ");
            return new QueryDataAccumulator().getCollectedData();
        } else {
            int relativePosition = ResourceManager.chordManager.getRelativeIndexOfNodeWhoseStrictScopeContains(key);
            Log.w(TAG, "Forwarding query request for key " + key + " to node at relative position " + relativePosition);
            final QueryResponseCollector responseCollector = new QueryResponseCollector(key, relativePosition, true);
            responseCollector.run();
            return responseCollector.getQueryResultCursor();
        }
    }
}
