package com.german.learner;

import android.app.Application;
import com.german.learner.utils.StateManager;

public class GermanApplication extends Application {

    private static GermanApplication instance;
    private StateManager stateManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        stateManager = new StateManager(this);
    }

    public static GermanApplication getInstance() {
        return instance;
    }

    public StateManager getStateManager() {
        return stateManager;
    }
}