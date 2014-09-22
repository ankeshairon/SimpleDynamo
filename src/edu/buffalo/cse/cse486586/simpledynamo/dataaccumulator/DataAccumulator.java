package edu.buffalo.cse.cse486586.simpledynamo.dataaccumulator;

import edu.buffalo.cse.cse486586.simpledynamo.responsecollectors.ResponseCollector;

public class DataAccumulator {

    protected Thread[] startThreads(ResponseCollector[] responseCollectors) {
        Thread[] threads = new Thread[responseCollectors.length];
        for (int i = 0; i < responseCollectors.length; i++) {
            threads[i] = new Thread(responseCollectors[i]);
            threads[i].start();
        }
        return threads;
    }

    protected void joinOnThreads(Thread[] threads) {
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
