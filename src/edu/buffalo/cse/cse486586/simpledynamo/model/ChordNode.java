package edu.buffalo.cse.cse486586.simpledynamo.model;

import static edu.buffalo.cse.cse486586.simpledynamo.utils.HashingUtils.genHash;

public class ChordNode implements Comparable<ChordNode> {
    public Integer port;
    public String id;
    public ChordNode predecessor;
    public ChordNode successor;

    public ChordNode(Integer port, String id) {
        this.port = port;
        this.id = id;
    }

    @Override
    public int compareTo(ChordNode another) {
        return id.compareTo(another.id);
    }

    public Boolean hasInStrictScope(String key) {
        String hashKey = genHash(key);
        return isKeyBetween(hashKey, predecessor.id);
    }

    public Boolean hasInExtendedScope(String key) {
        String hashKey = genHash(key);
        return isKeyBetween(hashKey, predecessor.predecessor.predecessor.id);
    }

    private Boolean isKeyBetween(String hashKey, String predecessorId) {
        return
                //normal case
                ((isAGreaterThanB(id, hashKey) && isAGreaterThanB(hashKey, predecessorId)) ||
                        //minus infinity
                        (isALesserThanB(hashKey, id) && isALesserThanB(hashKey, predecessorId) && isALesserThanB(id, predecessorId)) ||
                        //plus infinity
                        (isAGreaterThanB(hashKey, predecessorId) && isAGreaterThanB(hashKey, id) && isALesserThanB(id, predecessorId)));
    }

    private boolean isAGreaterThanB(String a, String b) {
        return a.compareTo(b) > 0;
    }

    private boolean isALesserThanB(String a, String b) {
        return a.compareTo(b) < 0;
    }



}
