package edu.buffalo.cse.cse486586.simpledynamo.communicator;

import edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager;

import java.util.Arrays;
import java.util.List;

public class Communicator {

    private final ChordManager chordManager;

    public Communicator() {
        this.chordManager = ResourceManager.chordManager;
    }

    public void forwardRequestToAllPorts(String request, boolean askForAck) {
        List<Integer> destinationPorts = chordManager.getAllPorts();
        destinationPorts.remove(chordManager.myPort);
        forwardRequestTo(request, destinationPorts, null, askForAck);
    }

    public void forwardRequestToPortsWhoseExtendedScopeContains(String request, String key, boolean askForAck) {
        List<Integer> destinationPorts = chordManager.getPortsWhoseExtendedScopeContains(key);

        final int myPortIndexInList = destinationPorts.indexOf(chordManager.myPort);
        if (myPortIndexInList != -1) {
            destinationPorts.remove(myPortIndexInList);
        }
        forwardRequestTo(request, destinationPorts, null, askForAck);
    }

    public void forwardRequestTo(String request, Integer destinationPort, List<Integer> fallbackPorts, boolean askForAck) {
        forwardRequestTo(request, Arrays.asList(destinationPort), fallbackPorts, askForAck);
    }

    public void forwardRequestTo(String request, List<Integer> destinationPorts, List<Integer> fallbackPorts, Boolean askForAck) {
        new ClientTask(request, destinationPorts, fallbackPorts, askForAck).start();
//        new ClientTask(destinationPort).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }
}
