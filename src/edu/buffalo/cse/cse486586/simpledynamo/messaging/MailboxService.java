package edu.buffalo.cse.cse486586.simpledynamo.messaging;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.AWAITED;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.TAG;

public class MailboxService {
    volatile private Map<Integer, Message> messagesReceived;
    final private Object keyCounterLock;
    volatile private Integer currentKey;

    public MailboxService() {
        messagesReceived = new HashMap<>();
        currentKey = Integer.MIN_VALUE;
        keyCounterLock = new Object();
    }

    public Integer registerForExpectedMessage() {
        Integer myKey;
        synchronized (keyCounterLock) {
            messagesReceived.put(currentKey, new Message());
            myKey = currentKey;
            ++currentKey;
            keyCounterLock.notifyAll();
        }
        return myKey;
    }

    public void deliverNewMessageToMailbox(Integer requestId, String[] contents) {
        final Message message = messagesReceived.get(requestId);

        synchronized (message) {
            message.contents = contents;
            message.notifyAll();
        }
    }

    public String[] readMessage(Integer key) {
//        Message message = messagesReceived.get(key);
        final Message message = messagesReceived.get(key);
        String[] messageContents;

//        while (message == null || message.contents == null || message.contents.length == 0) {
//            try {
//                Log.w(TAG, "Waiting for messsage to be registered. Message ID - " + key);
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            message = messagesReceived.get(key);
//        }

        Log.w(TAG, "Message registered. Message ID - " + key + " Current contents " + Arrays.asList(message.contents));

        synchronized (message) {
//            while (message == null || message.contents == null || message.contents.length == 0 || message.contents[0].equals(AWAITED)) {
            while (message.contents[0].equals(AWAITED)) {
                try {
                    Log.w(TAG, "Waiting for a messsage");
                    message.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            message.notifyAll();
            messageContents = message.contents;
        }
//        messagesReceived.remove(key);
        return messageContents;
    }

    public String[] readMessage(Integer key, long timeOut) {
        final Message message = messagesReceived.get(key);
        String[] messageContents;

        synchronized (message) {
            try {
                Log.w(TAG, "Going to wait for a messsage for " + timeOut);
                message.wait(timeOut);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            message.notifyAll();
            messageContents = message.contents;
        }
//        messagesReceived.remove(key);
        return messageContents;
    }
}
