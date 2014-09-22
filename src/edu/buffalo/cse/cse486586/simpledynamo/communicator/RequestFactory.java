package edu.buffalo.cse.cse486586.simpledynamo.communicator;

import edu.buffalo.cse.cse486586.simpledynamo.constants.Operation;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.DELIMITER;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.chordManager;

public class RequestFactory {

    public String createRequest(String operation, Integer requesterIndex, Object... valuesToSend) {
        StringBuilder request = new StringBuilder();
        request.append(operation)
                .append(DELIMITER)
                .append(requesterIndex);

        for (Object valueToSend : valuesToSend) {
            request.append(DELIMITER).append(valueToSend);
        }

        return request.toString();
    }

    public String attachMyPortForAckTo(String request) {
        return createGenericRequest(chordManager.myPort, request);
    }

    public String attachRequestIdForAckTo(Integer messageId, String request) {
        return createGenericRequest(messageId, request);
    }

    public String createAckRequest(String messageId) {
        return createGenericRequest(Operation.ACK, messageId);
    }

    public String createGenericRequest(Object... tokens) {
        StringBuilder request = new StringBuilder();
        for (Object token : tokens) {
            request.append(DELIMITER).append(token);
        }
        return request.substring(DELIMITER.length(), request.length());
    }

    public String createRequestOfTokens(String[] requestTokens) {
        StringBuilder request = new StringBuilder();
        for (String requestToken : requestTokens) {
            request.append(DELIMITER).append(requestToken);
        }
        return request.substring(DELIMITER.length(), request.length());
    }
}
