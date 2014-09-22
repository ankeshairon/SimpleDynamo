package edu.buffalo.cse.cse486586.simpledynamo.communicator;

import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.constants.Constants;
import edu.buffalo.cse.cse486586.simpledynamo.model.ChordNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.PORTS_LIST;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.myIndex;
import static edu.buffalo.cse.cse486586.simpledynamo.utils.HashingUtils.genHash;

public class ChordManager {
    private List<ChordNode> chordNodes;
    public int myPort;

    public ChordManager(int myPort) {
        this.myPort = myPort;
        chordNodes = new ArrayList<>();

        ChordNode chordNode;
        ChordNode myNode = null;
        for (int i = 0; i < PORTS_LIST.length; i++) {
            chordNode = new ChordNode(PORTS_LIST[i], genHash(PORTS_LIST[i] / 2));
            chordNodes.add(chordNode);

            if (PORTS_LIST[i] == myPort) {
                myNode = chordNode;
            }
        }
        Collections.sort(chordNodes);
        myIndex = chordNodes.indexOf(myNode);
        linkChordNodes();
    }

    public Boolean isInMyScopeStrictly(String key) {
        return chordNodes.get(myIndex).hasInStrictScope(key);
    }

    public Boolean isInMyExtendedScope(String key) {
        return chordNodes.get(myIndex).hasInExtendedScope(key);
    }

    public List<Integer> getPortsWhoseExtendedScopeContains(String key) {
        List<Integer> destinationPorts = new ArrayList<>();
        int i = 0;

        for (; i < chordNodes.size(); i++) {
            if (chordNodes.get(i).hasInStrictScope(key)) {
                break;
            }
        }
        destinationPorts.add(chordNodes.get(i).port);
        destinationPorts.add(chordNodes.get(i).successor.port);
        destinationPorts.add(chordNodes.get(i).successor.successor.port);
        return destinationPorts;
    }

    public Integer getPortWhoseStrictScopeContains(String key) {
        for (ChordNode chordNode : chordNodes) {
            if (chordNode.hasInStrictScope(key)) {
                return chordNode.port;
            }
        }
        throw new UnsupportedOperationException("Unable to determine node for " + key);
    }

    public int getRelativeIndexOfNodeWhoseStrictScopeContains(String key) {
        for (int i = 0; i < chordNodes.size(); i++) {
            if (chordNodes.get(i).hasInStrictScope(key)) {
                return i - myIndex;
            }
        }
        throw new UnsupportedOperationException("Unable to determine node for " + key);
    }

    public String getIdAtRelativePosition(Integer relativePosition) {
        Integer indexOfInterest = myIndex + relativePosition;
        indexOfInterest = getAbsoluteIndexForRelativeIndex(indexOfInterest);
        return chordNodes.get(indexOfInterest).id;
    }

    public Integer getPortAtRelativePosition(Integer relativePosition) {
        Integer indexOfInterest = getAbsoluteIndexForRelativeIndex(myIndex + relativePosition);
        return chordNodes.get(indexOfInterest).port;
    }

    public Integer getAbsoluteIndexForRelativeIndex(Integer indexOfInterest) {
        if (indexOfInterest < 0) {
            indexOfInterest = chordNodes.size() + indexOfInterest;
        } else if (indexOfInterest >= chordNodes.size()) {
            indexOfInterest = indexOfInterest - chordNodes.size();
        }
        return indexOfInterest;
    }

    public boolean isInStrictScopeOfIndex(Integer absolutePosition, String recordKey) {
        return chordNodes.get(absolutePosition).hasInStrictScope(recordKey);
    }

    public Integer getPortAtAbsolutePosition(Integer absolutePosition) {
        return chordNodes.get(absolutePosition).port;
    }

    public List<Integer> getAllPorts() {
        List<Integer> allPorts = new ArrayList<>();
        for (ChordNode chordNode : chordNodes) {
            allPorts.add(chordNode.port);
        }
        return allPorts;
    }

    public Integer getRelativeIndexOfNodeWithPort(int port) {
        int i = 0;
        for (; i < chordNodes.size(); i++) {
            if (chordNodes.get(i).port == port) {
                break;
            }
        }
        return i - myIndex;
    }

    private void linkChordNodes() {
        ChordNode chordNode;
        for (int i = 1; i < chordNodes.size() - 1; i++) {
            chordNode = chordNodes.get(i);
            chordNode.predecessor = chordNodes.get(i - 1);
            chordNode.successor = chordNodes.get(i + 1);
        }

        chordNode = chordNodes.get(0);
        chordNode.predecessor = chordNodes.get(chordNodes.size() - 1);
        chordNode.successor = chordNodes.get(1);

        chordNode = chordNodes.get(chordNodes.size() - 1);
        chordNode.predecessor = chordNodes.get(chordNodes.size() - 2);
        chordNode.successor = chordNodes.get(0);
    }

    @Override
    public String toString() {
        StringBuilder chordMap = new StringBuilder();
        for (int i = 0; i < chordNodes.size(); i++) {
            chordMap.append("\n").append(i).append(" ---> ").append(chordNodes.get(i).id);
        }
        return chordMap.toString();
    }

    public Integer getPortOfSuccessorOfPort(int port) {
        for (ChordNode chordNode : chordNodes) {
            if (chordNode.port == port) {
                return chordNode.successor.port;
            }
        }
        Log.e(Constants.TAG, "Could not find this port in chord - " + port);
        throw new RuntimeException("Could not find this port in chord - " + port);
    }

    public Integer getAbsoluteIndexOfPort(Integer port) {
        for (int i = 0; i < chordNodes.size(); i++) {
            if (chordNodes.get(i).port.equals(port)) {
                return i;
            }
        }
        Log.e(Constants.TAG, "Could not find this port in chord - " + port);
        throw new RuntimeException("Could not find this port in chord - " + port);
    }
}
