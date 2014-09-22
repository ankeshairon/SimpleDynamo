package edu.buffalo.cse.cse486586.simpledynamo.communicator;

import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.failurehandler.DestinationFailureHandler;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.DELIVERED;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.TAG;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.mailboxService;
import static edu.buffalo.cse.cse486586.simpledynamo.utils.MsgSendingUtils.attachMessageIdAndSendRequest;

public class ClientTask extends Thread {
    //public class ClientTask extends AsyncTask<String, Void, Void> {

    private final List<Integer> destinationPorts;
    private final Boolean shouldAskForAck;
    private List<Integer> fallbackPorts;
    private String request;

    public ClientTask(String request, List<Integer> destinationPorts, List<Integer> fallbackPorts, Boolean shouldAskForAck) {
//    public ClientTask(int destinationPorts) {
        this.shouldAskForAck = shouldAskForAck;
        this.destinationPorts = destinationPorts;
        this.fallbackPorts = fallbackPorts;
        this.request = request;
    }

    @Override
//    public Void doInBackground(String...msgs) {
    public void run() {
        Integer messageId;

        for (Integer remotePort : destinationPorts) {
            try {
                messageId = attachMessageIdAndSendRequest(remotePort, request, shouldAskForAck);
                if (!requestHasBeenDelivered(messageId)) {
                    tryToSendToFallbackPorts();
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException. Sending interrupted!");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException. Request not sent : " + request);
//            Log.e(TAG, "ClientTask socket IOException. Request not sent : " + msgs[0]);
                e.printStackTrace();
            }
        }
//        return null;
    }

    private void tryToSendToFallbackPorts() throws IOException {
        Integer messageId;

        if (fallbackPorts == null || fallbackPorts.isEmpty()) {
            DestinationFailureHandler failureHandler = new DestinationFailureHandler(request);

            if (failureHandler.hasFallbackPortsNKeyBeenUpdatedForFallback(destinationPorts.get(0))) {
                fallbackPorts = failureHandler.getUpdatedFallbackPorts();
                request = failureHandler.getUpdatedRequest();
                tryToSendToFallbackPorts();
            }
        } else {
            for (Integer fallbackPort : fallbackPorts) {
                messageId = attachMessageIdAndSendRequest(fallbackPort, request, shouldAskForAck);
                if (requestHasBeenDelivered(messageId)) {
                    return;
                }
            }
        }
    }

    private boolean requestHasBeenDelivered(Integer messageId) {
        if (!shouldAskForAck) {
            return true;
        }

        final String[] message = mailboxService.readMessage(messageId, 5000);
        return DELIVERED.equals(message[0]);
    }
}
