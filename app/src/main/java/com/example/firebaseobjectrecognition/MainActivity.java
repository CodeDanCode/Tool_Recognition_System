package com.example.firebaseobjectrecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerLocalModel;
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerOptions;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Mode;
import org.jetbrains.annotations.NotNull;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "Probability";

    private Button btnCapture;
    private ImageView imageView;
    private TextView feature;
    private TextView probability;
    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCapture = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);
        feature = findViewById(R.id.feature);
        probability = findViewById(R.id.probability);

        cameraView = findViewById(R.id.cameraView);
        cameraView.setMode(Mode.PICTURE);
        cameraView.setLifecycleOwner(this);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cameraView.isOpened()){
                    takePicture();
                }else{
                    cameraView.open();
                }
            }
        });

    }


    public void takePicture(){

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NotNull PictureResult result){
                result.toBitmap(new BitmapCallback() {
                    @Override
                    public void onBitmapReady(@Nullable Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                        InputImage image = InputImage.fromBitmap(bitmap,0);
                        Log.d(TAG,String.valueOf(image));
                        AutoMLImageLabelerLocalModel localModel = new AutoMLImageLabelerLocalModel.Builder()
                                .setAssetFilePath("manifest.json")
                                // or .setAbsoluteFilePath(absolute file path to manifest file)
                                //.setAbsoluteFilePath("fine-grain/manifest.json")
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

                                        String text = labels.get(0).getText();
                                        float confidence = labels.get(0).getConfidence();
                                        Log.d(TAG,text);
                                        Log.d(TAG,String.valueOf(confidence));

                                        if(confidence >= 0.30){
                                            feature.setText(text);
                                            probability.setText(String.valueOf(confidence));
                                        }
                                    }
                                })

                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.d(TAG,"failed to identify object");
                                    }
                                });
                        cameraView.close();
                    }
                });
            }
        });
        cameraView.takePicture();
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraView.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.destroy();
    }


}

