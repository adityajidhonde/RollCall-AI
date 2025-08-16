package com.example.attendance;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.attendance.databinding.ActivityAttendanceBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttendanceActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceActivity";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.CAMERA
            };
        }
    }

    private ActivityAttendanceBinding binding;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private AppDatabase database;
    private FaceDetector faceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = AppDatabase.getDatabase(this);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();
        faceDetector = FaceDetection.getClient(options);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }

        binding.takeAttendanceButton.setOnClickListener(view -> takePhotoAndCompare());

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhotoAndCompare() {
        if (imageCapture == null) {
            return;
        }

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Attendance");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                        runOnUiThread(() -> Toast.makeText(getBaseContext(), "Photo capture failed: " + exc.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri capturedUri = output.getSavedUri();
                        runOnUiThread(() -> Toast.makeText(getBaseContext(), "Photo captured, now comparing...", Toast.LENGTH_SHORT).show());

                        // Run face comparison on a background thread
                        ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
                        dbExecutor.execute(() -> {
                            compareFacesFromCapturedImage(capturedUri);
                        });
                    }
                }
        );
    }

    private void compareFacesFromCapturedImage(Uri capturedImageUri) {
        try {
            Bitmap capturedBitmap = uriToBitmap(capturedImageUri);
            if (capturedBitmap == null) {
                runOnUiThread(() -> Toast.makeText(this, "Could not get bitmap from captured image URI.", Toast.LENGTH_SHORT).show());
                return;
            }

            InputImage capturedInputImage = InputImage.fromBitmap(capturedBitmap, 0);
            faceDetector.process(capturedInputImage)
                    .addOnSuccessListener(capturedFaces -> {
                        if (capturedFaces.isEmpty()) {
                            runOnUiThread(() -> {
                                binding.resultTextView.setText("No face detected in the new photo.");
                                Toast.makeText(this, "No face detected in the new photo.", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        Face capturedFace = capturedFaces.get(0);
                        compareWithStoredStudents(capturedFace);
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            binding.resultTextView.setText("Face detection failed.");
                            Toast.makeText(this, "Face detection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    });
        } catch (IOException e) {
            runOnUiThread(() -> {
                binding.resultTextView.setText("Error getting bitmap from URI.");
                Toast.makeText(this, "Error getting bitmap from URI.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error getting bitmap from URI", e);
            });
        }
    }

    private void compareWithStoredStudents(Face capturedFace) {
        ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
        dbExecutor.execute(() -> {
            List<Student> storedStudents = database.studentDao().getAllStudents();
            List<Task<List<Face>>> faceDetectionTasks = new ArrayList<>();

            for (Student student : storedStudents) {
                try {
                    Bitmap storedBitmap = uriToBitmap(Uri.parse(student.getPhotoUri()));
                    if (storedBitmap == null) continue;

                    InputImage storedInputImage = InputImage.fromBitmap(storedBitmap, 0);
                    Task<List<Face>> task = faceDetector.process(storedInputImage);
                    faceDetectionTasks.add(task);
                } catch (IOException e) {
                    Log.e(TAG, "Error processing stored image for student " + student.getName(), e);
                }
            }

            Tasks.whenAllSuccess(faceDetectionTasks)
                    .addOnSuccessListener(results -> {
                        boolean matchFound = false;
                        for (int i = 0; i < results.size(); i++) {
                            List<Face> storedFaces = (List<Face>) results.get(i);
                            if (!storedFaces.isEmpty()) {
                                if (isFaceMatch(capturedFace, storedFaces.get(0))) {
                                    final Student matchedStudent = storedStudents.get(i);
                                    runOnUiThread(() -> {
                                        binding.resultTextView.setText("Attendance marked for: " + matchedStudent.getName());
                                        Toast.makeText(this, "Match found for: " + matchedStudent.getName(), Toast.LENGTH_LONG).show();
                                    });
                                    matchFound = true;
                                    break; // Stop after the first match
                                }
                            }
                        }

                        if (!matchFound) {
                            runOnUiThread(() -> {
                                binding.resultTextView.setText("No matching student found.");
                                Toast.makeText(this, "No matching student found in the database.", Toast.LENGTH_LONG).show();
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            binding.resultTextView.setText("Comparison failed.");
                            Toast.makeText(this, "Comparison failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    });
        });
    }

    // New, simple face comparison logic based on bounding box similarity
    private boolean isFaceMatch(Face face1, Face face2) {
        Rect rect1 = face1.getBoundingBox();
        Rect rect2 = face2.getBoundingBox();

        // Check if the faces are of similar size (within 20% tolerance)
        boolean isSimilarSize = Math.abs(rect1.width() - rect2.width()) < (0.2 * Math.max(rect1.width(), rect2.width())) &&
                Math.abs(rect1.height() - rect2.height()) < (0.2 * Math.max(rect1.height(), rect2.height()));

        // Check if the faces are in a similar location (within 20% tolerance of image size)
        float imageWidth = (float) getResources().getDisplayMetrics().widthPixels;
        float imageHeight = (float) getResources().getDisplayMetrics().heightPixels;

        boolean isSimilarPosition = Math.abs(rect1.centerX() - rect2.centerX()) < (0.2 * imageWidth) &&
                Math.abs(rect1.centerY() - rect2.centerY()) < (0.2 * imageHeight);

        return isSimilarSize && isSimilarPosition;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.attendanceViewFinder.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();

                // Changed to use the back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private Bitmap uriToBitmap(Uri selectedFileUri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(selectedFileUri, "r");
        if (parcelFileDescriptor == null) return null;
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}