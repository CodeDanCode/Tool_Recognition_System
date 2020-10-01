package com.example.firebaseobjectrecognition;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerLocalModel;
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerOptions;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    private static final int MY_CAMERA_REQUEST_CODE = 101;
    private static final String TAG = "Probability";

    private Button btnCapture;
    private ImageView imageView;
    private Bitmap bitmap;
    private TextView feature;
    private TextView probability;
    private int rotation;


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCapture = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);
        feature = findViewById(R.id.feature);
        probability = findViewById(R.id.probability);

        if(!hasCamera()){
            btnCapture.setEnabled(false);
        }

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

    }

    private boolean hasCamera(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            }
            return true;
        }else{
            return false;
        }
    }

    public void takePicture(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,2);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle extras = data.getExtras();
        bitmap = (Bitmap) extras.get("data");

        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Image Saved to:\n" +
                        bitmap, Toast.LENGTH_LONG).show();

                imageView.setImageBitmap(bitmap);
                InputImage image = InputImage.fromBitmap(bitmap,0);

                AutoMLImageLabelerLocalModel localModel =
                        new AutoMLImageLabelerLocalModel.Builder()
                                .setAssetFilePath("manifest.json")
                                // or .setAbsoluteFilePath(absolute file path to manifest file)
                                .build();

                AutoMLImageLabelerOptions autoMLImageLabelerOptions =
                        new AutoMLImageLabelerOptions.Builder(localModel)
                                .setConfidenceThreshold(0.0f)  // Evaluate your model in the Firebase console
                                // to determine an appropriate value.
                                .build();
                ImageLabeler labeler = ImageLabeling.getClient(autoMLImageLabelerOptions);

                labeler.process(image)
                        .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                            @Override
                            public void onSuccess(List<ImageLabel> labels) {

//                        for (ImageLabel label : labels) {
//                            String text = label.getText();
//                            float confidence = label.getConfidence();
//                            int index = label.getIndex();
//                            Log.d(TAG,text);
//                            Log.d(TAG,String.valueOf(confidence));
//                            Log.d(TAG,String.valueOf(index));
//                            text1.setText(text);

                                String text = labels.get(0).getText();
                                float confidence = labels.get(0).getConfidence();
                                Log.d(TAG,text);
                                Log.d(TAG,String.valueOf(confidence));

                                if(confidence >= 0.75){
                                    feature.setText(text);
                                    probability.setText(String.valueOf(confidence));
                                }
                            }
                        })

                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });

            } else if (requestCode == RESULT_CANCELED) {
                Toast.makeText(this, "Image cancelled. ",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to capture image",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // Get the device's sensor orientation.
        CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);

        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        }
        return rotationCompensation;
    }
}

