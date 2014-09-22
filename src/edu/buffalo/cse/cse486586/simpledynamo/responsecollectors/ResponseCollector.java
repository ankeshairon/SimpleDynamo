package edu.buffalo.cse.cse486586.simpledynamo.responsecollectors;

import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.constants.Constants;
import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.TAG;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.myIndex;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Operation.QUERY;

public abstract class ResponseCollector implements Runnable {

    protected final Boolean tryReplicasOnFail;
    protected final String keyToQuery;
    protected final int relativePosition;

    public ResponseCollector(String keyToQuery, int relativePosition, Boolean tryReplicasOnFail) {
        this.keyToQuery = keyToQuery;
        this.relativePosition = relativePosition;
        this.tryReplicasOnFail = tryReplicasOnFail;
    }

    protected Integer sendQueryRequest(Boolean tryReplicasOnFail) {
        final Integer messageId = ResourceManager.mailboxService.registerForExpectedMessage();
        final String request = ResourceManager.requestFactory.createRequest(QUERY, myIndex, messageId, keyToQuery);
        final Integer destinationPort = ResourceManager.chordManager.getPortAtRelativePosition(relativePosition);

        List<Integer> fallbackPorts = null;
        if (tryReplicasOnFail) {
            fallbackPorts = new ArrayList<>();
            fallbackPorts.add(ResourceManager.chordManager.getPortAtRelativePosition(relativePosition + 1));
            fallbackPorts.add(ResourceManager.chordManager.getPortAtRelativePosition(relativePosition + 2));
        }

        ResourceManager.communicator.forwardRequestTo(request, destinationPort, fallbackPorts, true);
        return messageId;
    }


    protected boolean areThereNoResultsToProcess(String[] messageContents) {
        if (messageContents.length == 1 && messageContents[0].equals(Constants.NULL)) {
            return true;
        }

        if (messageContents.length % 2 != 0) {
            throw new RuntimeException("Malformed response string received");
        }
        return false;
    }

    protected String[] sendRequestAndGetMessageContents() {
        final Integer messageId = sendQueryRequest(tryReplicasOnFail);
        final String[] messageContents = ResourceManager.mailboxService.readMessage(messageId);
        Log.w(TAG, "Response received - " + Arrays.asList(messageContents));
        return messageContents;
    }

}
