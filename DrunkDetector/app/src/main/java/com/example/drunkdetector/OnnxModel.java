package com.example.drunkdetector;

import ai.onnxruntime.*;
import android.content.Context;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
public class OnnxModel {

    private final OrtEnvironment env;
    private final OrtSession session;

    public OnnxModel(Context context) throws Exception {
        env = OrtEnvironment.getEnvironment();
        byte[] modelBytes;

        try (InputStream is = context.getAssets().open("drunk_model.onnx")) {
            modelBytes = new byte[is.available()];
            is.read(modelBytes);
        } // get the model file with an input stream, and read as many bytes as we can (hopefully all of them)

        session = env.createSession(modelBytes, new OrtSession.SessionOptions());
        // create a "session" of the loaded ML model
    }

    public double[] predict(double[] inputData) throws Exception {

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
        // onnx expects data to be in the form of an Onnx Tensor object

        OrtSession.Result result = session.run(Collections.singletonMap(
                session.getInputNames().iterator().next(), inputTensor));
        // the input is supposed to be a map between input names and input values
        // so I used this rather clunky method to get that

        double[][] outputArray = (double[][]) result.get(0).getValue(); // should return the probabilities of "drunk"
        return outputArray[0]; // returns probability of "drunk"

    }
}
