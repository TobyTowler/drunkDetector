package com.example.drunkdetector;

import ai.onnxruntime.*;
import android.content.Context;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public double predict(float[] inputData) throws Exception {

        float[][] inputArray2D = new float[1][inputData.length];
        inputArray2D[0] = inputData;

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputArray2D);
        // onnx expects data to be in the form of an Onnx Tensor object


        OrtSession.Result result = session.run(Collections.singletonMap(
                session.getInputNames().iterator().next(), inputTensor));
        // the input is supposed to be a map between input names and input values
        // so I used this rather clunky method to get that

        String[] outputArray = (String[]) result.get(0).getValue(); // should return the probabilities of "drunk"
        String outputName = "output_probability";
        double[] probabilities;

        Object outputValue = result.get(outputName).get().getValue();
        System.out.println(outputValue.toString());

            // If the output is a List (like UnmodifiableRandomAccessList)
        List<?> listOutput = (List<?>) outputValue;

        if (listOutput.isEmpty()) {
            throw new IllegalStateException("Empty output list");
        }

        OnnxMap outputMap = (OnnxMap) listOutput.get(0);

        System.out.println(outputMap.getValue().toString());

        //double[][] output = (double[][]) value.getValue();
        return ((Float) outputMap.getValue().get("Drunk")).doubleValue(); // returns prob of drunk

    }
}
