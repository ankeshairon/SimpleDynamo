package edu.buffalo.cse.cse486586.simpledynamo.model;

import android.util.Log;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.TAG;

public class CustomLock {
    private volatile Boolean conditionMet;

    public CustomLock(Boolean hasConditionMet) {
        this.conditionMet = hasConditionMet;
    }

    public Boolean hasConditionMet() {
        return conditionMet;
    }

    public void setHasConditionMet(Boolean hasConditionMet) {
        this.conditionMet = hasConditionMet;
    }

    public void waitOnThis(String tag, String errorMessage) {
        try {
            Log.w(TAG, "Waiting for condition to meet");
            wait(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(tag, errorMessage);
        }
    }
}
