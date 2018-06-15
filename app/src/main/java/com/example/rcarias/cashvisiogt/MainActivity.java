package com.example.rcarias.cashvisiogt;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.security.Policy;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextToSpeech tts;
    private Camera mCamera;
    private CameraPreview mPreview;
    FirebaseVisionImage image;
    Bitmap              bitmap = null;
    Button              button_capture;
    FirebaseVisionTextDetector detector;
    ImageView           img;
    Activity            act;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        act = this;
        button_capture = (Button)findViewById(R.id.button_capture);
        img = (ImageView)findViewById(R.id.img);
        mCamera = getCameraInstance();
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//some more settings
        mCamera.setParameters(params);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        mCamera.setDisplayOrientation(90);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        FirebaseApp.initializeApp(this.getApplicationContext());

        button_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null,null,mpicture);
            }
        });

        tts = new TextToSpeech(act, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                     Locale locSpanish = new Locale("spa", "MEX");
                    tts.setLanguage(locSpanish);
                }
            }
        });

    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mpicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {

            bitmap  = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            bitmap = RotateBitmap(bitmap,90);
            img.setImageBitmap(bitmap);
            image = FirebaseVisionImage.fromBitmap(bitmap);
            detector = FirebaseVision.getInstance().getVisionTextDetector();
            Task<FirebaseVisionText> result =
                    detector.detectInImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                String ttsS="";
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    // Task completed successfully
                                    // ...
                                    for (FirebaseVisionText.Block block: firebaseVisionText.getBlocks()) {
                                        //Rect boundingBox = block.getBoundingBox();
                                        //Point[] cornerPoints = block.getCornerPoints();
                                        String text = block.getText();
                                        System.out.println(text);
                                        for (FirebaseVisionText.Line line: block.getLines()) {

                                            // ...
                                            for (FirebaseVisionText.Element element: line.getElements()) {
                                                // ...
                                                FirebaseVisionText.Element e = element;
                                                ttsS += e.getText();
                                                System.out.println(e.getText());

                                            }
                                        }
                                    }
                                    tts.speak(ttsS, TextToSpeech.QUEUE_FLUSH, null);
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                                    });


        }

    };

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}


