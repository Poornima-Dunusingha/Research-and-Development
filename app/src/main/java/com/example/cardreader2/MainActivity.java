package com.example.cardreader2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Button btnCapture;
    private EditText cardNum;
    private EditText expDate;
    private TextRecognizer textRecognizer;
    private CameraSource cameraSource;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCapture = findViewById(R.id.btnCapture);
        cardNum = findViewById(R.id.cardNum);
        expDate = findViewById(R.id.expDate);

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        btnCapture.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                // Camera permission is already granted, launch camera intent
                launchCameraIntent();
            } else {
                // Request camera permission from the user
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, launch camera intent
                launchCameraIntent();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void launchCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                // Process the captured image
                processImage(imageBitmap);
            }
        }
    }

    private void processImage(Bitmap bitmap) {
        if (!textRecognizer.isOperational()) {
            Toast.makeText(this, "Text recognizer could not be set up.", Toast.LENGTH_SHORT).show();
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

        String[] cardNumber1 = {null};
        String[] expiryDate2 = {null};

        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.valueAt(i);
            Log.d("Card details", textBlock.toString());
            String[] lines = textBlock.getValue().split("\\n");

            // Look for patterns matching credit card number and expiry date
            for (String line : lines) {
                if (isPossibleCardNumber(line)) {
                    cardNumber1[0] = line.replaceAll("\\s", ""); // Remove whitespaces
                } else if (isPossibleExpiryDate(line)) {
                    expiryDate2[0] = line;
                }
            }
        }

        final String finalCardNumber = cardNumber1[0];
        final String finalExpiryDate = expiryDate2[0];

        runOnUiThread(() -> {
            if (finalCardNumber != null) {
                cardNum.setText(finalCardNumber);
                Log.d("Card Number", finalCardNumber.toString());
            } else {
                cardNum.setText("Card number not detected");
                Log.d("Card Number false", "Error" );
            }
            if (finalExpiryDate != null) {
                expDate.setText(finalExpiryDate);
            } else {
                expDate.setText("Expiry date not detected");
            }
        });
    }

    private boolean isPossibleCardNumber(String text) {
        // Remove whitespaces and check if the remaining string contains 16 digits
        String cleanedText = text.replaceAll("\\s", "");
        return cleanedText.matches("^\\d{16}$"); // Check for 16 digits
    }


    private boolean isPossibleExpiryDate(String text) {
        // Check if the text matches the MM/YY format (e.g., 11/25)
        return text.matches("^(0[1-9]|1[0-2])/(\\d{2})$"); // Check for MM/YY format
    }


}