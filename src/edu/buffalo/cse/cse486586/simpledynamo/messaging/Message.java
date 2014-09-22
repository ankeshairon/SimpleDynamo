package edu.buffalo.cse.cse486586.simpledynamo.messaging;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.AWAITED;

public class Message {
    public volatile String[] contents;

    public Message() {
        this.contents = new String[]{AWAITED};
    }
}
