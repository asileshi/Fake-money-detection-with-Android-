package com.example.first;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PICTURE_REQUEST = 2;
    private static final String TAG = "MainActivity";
    private ImageView imageView;
    private TextView resultTextView;
    private Button checkButton;
    private Bitmap selectedImage;
    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.resultTextView);
        checkButton = findViewById(R.id.checkButton);
        checkButton.setVisibility(View.GONE);

        try {
            tflite = new Interpreter(loadModelFile());
            Log.d(TAG, "Model loaded successfully");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error loading model", e);
        }

        Button selectImageButton = findViewById(R.id.selectImageButton);
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        Button takePictureButton = findViewById(R.id.takePictureButton);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImage != null) {
                    if (tflite != null) {
                        String result = runInference(selectedImage);
                        if ("Real".equals(result)) {
                            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        } else {
                            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }
                        resultTextView.setText(result);
                    } else {
                        Log.e(TAG, "TFLite interpreter is not initialized");
                    }
                }
            }
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(getAssets().openFd("final.tflite").getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = getAssets().openFd("final.tflite").getStartOffset();
        long declaredLength = getAssets().openFd("final.tflite").getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void takePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, TAKE_PICTURE_REQUEST);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, TAKE_PICTURE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedImage);
                imageView.setVisibility(View.VISIBLE);
                checkButton.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error loading image", e);
            }
        } else if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            selectedImage = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(selectedImage);
            imageView.setVisibility(View.VISIBLE);
            checkButton.setVisibility(View.VISIBLE);
        }
    }

    private float[][][][] preprocessImage(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        float[][][][] input = new float[1][224][224][3];
        for (int i = 0; i < 224; i++) {
            for (int j = 0; j < 224; j++) {
                int pixel = resizedBitmap.getPixel(i, j);
                input[0][i][j][0] = ((pixel >> 16) & 0xFF) / 255.0f; // Red channel
                input[0][i][j][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // Green channel
                input[0][i][j][2] = (pixel & 0xFF) / 255.0f;         // Blue channel
            }
        }
        return input;
    }

    private String runInference(Bitmap bitmap) {
        float[][][][] input = preprocessImage(bitmap);
        float[][] output = new float[1][1];
        tflite.run(input, output);
        float prediction = output[0][0];
        if (prediction > 0.5) {
            return "Real";
        } else {
            return "Fake";
        }
    }
}
