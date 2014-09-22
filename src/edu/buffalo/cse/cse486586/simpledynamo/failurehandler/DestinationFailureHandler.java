package edu.buffalo.cse.cse486586.simpledynamo.failurehandler;

import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.constants.Operation;

import java.util.ArrayList;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.*;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.chordManager;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.requestFactory;

public class DestinationFailureHandler {

    private final ArrayList<Integer> fallbackPorts;
    private String request;

    public DestinationFailureHandler(String originalRequest) {
        this.request = originalRequest;
        fallbackPorts = new ArrayList<>();
    }

    //    0               1                             2            3
    //operation---requester index ---request id---key
    public boolean hasFallbackPortsNKeyBeenUpdatedForFallback(Integer originalDestinationPort) {
        final String[] requestTokens = request.split(DELIMITER);

        if (!String.valueOf(Operation.QUERY).equals(requestTokens[0])) {
            return false;
        }

        switch (requestTokens[3]) {
            case KEYS_IN_REQUESTER_STRICT_SCOPE:
                //this is only being used for recovery & a duplicate request is already being sent to another successor
                return false;
            case KEYS_IN_RESPONDER_STRICT_SCOPE:
            case ALL_LOCAL:
                //send request to the destination successor asking for predecessor's strict data
                Integer fallbackPort = chordManager.getPortOfSuccessorOfPort(originalDestinationPort);
                if (chordManager.getAbsoluteIndexOfPort(fallbackPort).toString().equals(requestTokens[1])) {
                    fallbackPort = chordManager.getPortOfSuccessorOfPort(fallbackPort);
                }
                fallbackPorts.add(fallbackPort);
                requestTokens[3] = requestFactory.createGenericRequest(KEYS_IN_SCOPE_OF_PORT, originalDestinationPort);
                break;
            default:
                Log.e(TAG, "Unable to calculate fallback port for " + requestTokens[3]);
                throw new UnsupportedOperationException("Unable to calculate fallback port for " + requestTokens[3]);
        }
        request = requestFactory.createRequestOfTokens(requestTokens);
        return true;
    }

    public ArrayList<Integer> getUpdatedFallbackPorts() {
        return fallbackPorts;
    }

    public String getUpdatedRequest() {
        return request;
    }
}
