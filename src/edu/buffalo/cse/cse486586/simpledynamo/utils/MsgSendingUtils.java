package edu.buffalo.cse.cse486586.simpledynamo.utils;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.TAG;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.mailboxService;
import static edu.buffalo.cse.cse486586.simpledynamo.resources.ResourceManager.requestFactory;

public class MsgSendingUtils {
    public static Integer attachMessageIdAndSendRequest(Integer remotePort, String request, boolean shouldAskForAck) throws IOException {
        String updatedRequest = request;
        Integer messageId = null;

        if (shouldAskForAck) {
            messageId = mailboxService.registerForExpectedMessage();
            updatedRequest = requestFactory.attachRequestIdForAckTo(messageId, updatedRequest);
        }
        sendRequest(updatedRequest, remotePort);
        return messageId;
    }

    public static void sendRequest(String updatedRequest, Integer remotePort) throws IOException {
        PrintWriter writer;
        Socket socket;

        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
        writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println(updatedRequest);
//            writer.println(msgs[0]);
        Log.w(TAG, "Request sent" + updatedRequest + " to " + remotePort);
        if (!socket.isClosed()) {
            socket.close();
        }
        writer.close();
    }
}
