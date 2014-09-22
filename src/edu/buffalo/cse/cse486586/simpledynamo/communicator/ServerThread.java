package edu.buffalo.cse.cse486586.simpledynamo.communicator;

import android.util.Log;
import edu.buffalo.cse.cse486586.simpledynamo.constants.Constants;
import edu.buffalo.cse.cse486586.simpledynamo.model.CustomLock;
import edu.buffalo.cse.cse486586.simpledynamo.operationshandler.ServerOperationsHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.TAG;

public class ServerThread extends Thread {

    private final CustomLock hasServerStartedLock;
    private ServerSocket serverSocket;


    public ServerThread(CustomLock hasServerStarted) {
        hasServerStartedLock = hasServerStarted;
        createServerSocket();
    }

    @Override
    public void run() {
        BufferedReader bufferedReader = null;
        Socket clientSocket = null;
        String line;

        synchronized (hasServerStartedLock) {
            hasServerStartedLock.setHasConditionMet(true);
            hasServerStartedLock.notifyAll();
        }

        try {
            while (true) {
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Error accepting incoming connection");
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Null pointer exception in Serversocket.accept()");
                    throw e;
                }
                try {
                    Log.w(TAG, "incoming received");
                    bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    if ((line = bufferedReader.readLine()) != null) {
                        new Thread(new ServerOperationsHandler(line)).start();
                    }
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error handling client socket");
                    e.printStackTrace();
                } finally {
                    try {
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            clientSocket.close();
                        }
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception closing client socket");
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            closeServerSocket();
        }
    }


    @Override
    protected void finalize() throws Throwable {
        closeServerSocket();
        super.finalize();
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket");
            }
        }
    }

    public void createServerSocket() {
        try {
            serverSocket = new ServerSocket(Constants.SERVER_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Error creating server socket");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}