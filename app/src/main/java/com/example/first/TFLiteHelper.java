package com.example.first;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteHelper {
    private static final String TAG = "TFLiteHelper";
    private Interpreter interpreter;

    public TFLiteHelper(Context context, String modelPath) throws IOException {
        try {
            interpreter = new Interpreter(loadModelFile(context, modelPath));
            Log.d(TAG, "TFLite model initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TFLite model", e);
            throw new IOException("Error initializing TFLite model", e);
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] runInference(float[][][][] inputData) {
        float[][] outputData = new float[1][5]; // Adjust the size based on your model's output
        interpreter.run(inputData, outputData);
        return outputData[0];
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}
