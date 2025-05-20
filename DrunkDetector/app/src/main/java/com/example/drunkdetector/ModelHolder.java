package com.example.drunkdetector;

import android.content.Context;


// singleton to hold the ML model, so that it may be accessed from static functions
public class ModelHolder {
    private static OnnxModel model;

    public static void initialize(Context context) throws Exception {
        if (model == null) {
            model = new OnnxModel(context);
        }
    }

    public static OnnxModel getModel() {
        return model;
    }
}
