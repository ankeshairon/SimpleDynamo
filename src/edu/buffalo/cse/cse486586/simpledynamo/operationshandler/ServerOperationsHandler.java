package edu.buffalo.cse.cse486586.simpledynamo.operationshandler;

import android.database.Cursor;
import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.constants.Constants;
import edu.buffalo.cse.cse486586.simpledynamo.constants.Operation;
import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;
import edu.buffalo.cse.cse486586.simpledynamo.utils.ContentValuesBuilder;

import java.util.Arrays;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Operation.*;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.*;
import static edu.buffalo.cse.cse486586.simpledynamo.utils.HashingUtils.genHash;

public class ServerOperationsHandler implements Runnable {

    private String request;

    public ServerOperationsHandler(String request) {
        this.request = request;
    }

    @Override
    public void run() {
        Log.w(TAG, "Started processing - " + request);
        //requestId---REQUEST
//        OR "ACK"---requestId
        final String[] splitTokens = request.split(DELIMITER);
        String[] requestTokens;

        //requestId---REQUEST
        if (splitTokens[1].equals(String.valueOf(Operation.QUERY))) {
            //acknowledgement only for query requests
            final Integer destnPort = chordManager.getPortAtAbsolutePosition(Integer.parseInt(splitTokens[2]));
            Log.w(TAG, "Sending ack for " + splitTokens[0] + " to node " + destnPort);
            final String ackRequest = requestFactory.createAckRequest(splitTokens[0]);
            communicator.forwardRequestTo(ackRequest, Arrays.asList(destnPort), null, false);
            requestTokens = Arrays.copyOfRange(splitTokens, 1, splitTokens.length);
        } else {
            requestTokens = splitTokens;
        }


        switch (requestTokens[0]) {
            case INSERT: //receieved insert request // operationId--original requester index--key---value
                Log.w(TAG, "Insert request received");
                handleInsertRequest(requestTokens);
                break;
            case DELETE:
                Log.w(TAG, "Delete request received");
                handleDeleteRequest(requestTokens); //receieved delete request // operationId--original requester index---key
                break;
            case QUERY: //operation---requester index ---request id---key[---queryForPort]
                Log.w(TAG, "Query request received");
                handleQueryRequest(requestTokens);
                break;
            case QUERY_RESPONSE: //operation---requester index ---request id---key---value[x N]
                Log.w(TAG, "Query response received");
                handleQueryResponse(requestTokens);
                break;
            case ACK: //operation-requestId
                mailboxService.deliverNewMessageToMailbox(Integer.parseInt(splitTokens[1]), new String[]{Constants.DELIVERED});
                break;
            default:
                Log.e(TAG, "Unknown operation type " + requestTokens[0] + " in " + Arrays.asList(requestTokens));
                throw new RuntimeException("Unknown operation type " + requestTokens[0]);
        }
    }

    //          0               1                               2       3
    // operationId--originalRequesterIndex--key---value
    private void handleInsertRequest(String[] requestTokens) {
        final String key = requestTokens[2];
        Log.w(TAG, "Insert request received for - " + key + "->" + requestTokens[3]);

        if (requestTokens[1].equals(String.valueOf(myIndex))) {
            logErrorForIncorrectlyForwardedRequest(key, "Received insert request forwarded by me! Error!  ");
        }

        if (!chordManager.isInMyExtendedScope(key)) {
            logErrorForIncorrectlyForwardedRequest(key, "Received insert request for an out of scope key! Error!  ");
        } else {
            Log.w(TAG, "Inserting in my DB - " + key + "->" + requestTokens[3]);
            contentResolver.insert(EXTERNAL_URI, ContentValuesBuilder.with(key, requestTokens[3]));
        }
    }

    // operationId--originalRequesterIndex---key
    private void handleDeleteRequest(String[] requestTokens) {
        final String key = requestTokens[2];
        Log.w(TAG, "Delete request received for key - " + key);

        if (requestTokens[1].equals(String.valueOf(myIndex))) {
            logErrorForIncorrectlyForwardedRequest(key, "Received delete request forwarded by me! Error!  ");
        }

        if (key.equals(ALL_IN_CHORD) || !ResourceManager.chordManager.isInMyExtendedScope(key)) {
            logErrorForIncorrectlyForwardedRequest(key, "Error! Received delete request for " + key);
        } else {
            Log.w(TAG, "Deleting record from myself for key - " + key);
            contentResolver.delete(EXTERNAL_URI, key, null);
        }
    }

    //      0               1                           2           3           4
    //operation---requesterIndex ---request id---key[---queryForPort]
    private void handleQueryRequest(String[] requestTokens) {
        final String queryKey = requestTokens[3];
        Log.w(TAG, "Received query sent by " + requestTokens[1] + " for key -" + queryKey);

        if (requestTokens[1].equals(String.valueOf(myIndex))) {
            Log.e(TAG, Arrays.asList(requestTokens).toString());
            logErrorForIncorrectlyForwardedRequest(requestTokens[3], "Received query request forwarded by me! Error!  ");
        }

        Cursor cursor;
        switch (queryKey) {
            case ALL_IN_CHORD:
                logErrorForIncorrectlyForwardedRequest(requestTokens[2], "Error! Received query request for key - " + queryKey);
                throw new RuntimeException("Error! Received query request for key - " + queryKey);
            case KEYS_IN_SCOPE_OF_PORT:
                Log.w(TAG, "Querying own DB for keys of port - " + requestTokens[4]);
                //let it fall through
            case ALL_LOCAL:
            case KEYS_IN_REQUESTER_STRICT_SCOPE:
            case KEYS_IN_RESPONDER_STRICT_SCOPE:
                Log.w(TAG, "Querying own DB for key - " + queryKey);
                cursor = contentResolver.query(EXTERNAL_URI, null, ALL_LOCAL, null, null);
                filterDataAndCreateNSendResponse(cursor, requestTokens, false);
                break;
            default:
                if (chordManager.isInMyExtendedScope(queryKey)) {
                    Log.w(TAG, "Querying own DB " + queryKey);
                    cursor = contentResolver.query(EXTERNAL_URI, null, queryKey, null, null);
                    filterDataAndCreateNSendResponse(cursor, requestTokens, true);
                } else {
                    logErrorForIncorrectlyForwardedRequest(requestTokens[2], "Error! Received query request for key - " + queryKey);
                    throw new RuntimeException("Error! Received query request for key - " + queryKey);
                }
        }
    }

    //      0                             1                           2           3                     4
    //requestOperation---requesterIndex ---request id---queryKey[---queryForPort]
    private void filterDataAndCreateNSendResponse(Cursor cursor, String[] requestTokens, Boolean isSingleKeyQuery) {
        final Integer requesterIndex = Integer.parseInt(requestTokens[1]);
        final String queryKey = requestTokens[3];
        Integer queryForIndex = null;

        if (requestTokens.length == 5) {
            queryForIndex = chordManager.getAbsoluteIndexOfPort(Integer.parseInt(requestTokens[4]));
        }

        StringBuilder response = new StringBuilder(requestFactory.createRequest(QUERY_RESPONSE, requesterIndex, requestTokens[2]));

        if (cursor == null || cursor.getCount() == 0) {
            response.append(DELIMITER).append("null");
        } else {
            int keyIndex = cursor.getColumnIndex(KEY_FIELD);
            int valueIndex = cursor.getColumnIndex(VALUE_FIELD);
            String tupleKey;

            cursor.moveToFirst();
            do {
                tupleKey = cursor.getString(keyIndex);
                if (tupleSatisfiesQueryKeyCriteria(queryKey, requesterIndex, tupleKey, queryForIndex)) {
                    response.append(DELIMITER).append(tupleKey)
                            .append(DELIMITER).append(cursor.getString(valueIndex));
                    if (isSingleKeyQuery) {
                        break;
                    }
                }
            } while (cursor.moveToNext());
        }
        communicator.forwardRequestTo(response.toString(), chordManager.getPortAtAbsolutePosition(requesterIndex), null, false);
    }

    private boolean tupleSatisfiesQueryKeyCriteria(String queryKey, Integer requesterIndex, String tupleKey, Integer queryForIndex) {
        switch (queryKey) {
            case ALL_LOCAL:
                return true;
            case KEYS_IN_REQUESTER_STRICT_SCOPE:
                return chordManager.isInStrictScopeOfIndex(requesterIndex, tupleKey);
            case KEYS_IN_RESPONDER_STRICT_SCOPE:
                return chordManager.isInStrictScopeOfIndex(myIndex, tupleKey);
            case KEYS_IN_SCOPE_OF_PORT:
                return chordManager.isInStrictScopeOfIndex(queryForIndex, tupleKey);
            default:
                return queryKey.equals(tupleKey);
        }
    }

    //operation---requester index ---request id---key---value---version[x N]
    private void handleQueryResponse(String[] requestTokens) {
        if (requestTokens[1].equals(String.valueOf(myIndex))) {
            //set value in expected data corresp to request id
            Log.w(TAG, "Received response for query request sent by me");
            mailboxService.deliverNewMessageToMailbox(Integer.parseInt(requestTokens[2]), Arrays.copyOfRange(requestTokens, 3, requestTokens.length));
        } else {
            Log.e(TAG, "Error! Received query response requested by guy at index - " + requestTokens[1] + " but my index is " + myIndex);
        }
    }

    private void logErrorForIncorrectlyForwardedRequest(String key, String errorMsg) {
        final String msg = errorMsg +
                "  key-" + key +
                " ,\nhash(Key)-" + genHash(key) +
                " ,\n myIndex-" + myIndex +
                chordManager.toString();
        Log.e(TAG, msg);
        throw new RuntimeException(msg);
    }
}