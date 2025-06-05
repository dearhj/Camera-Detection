package com.android.detection.detection.analyze;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.List;

/**
 * 对象检测分析器
 */
public class ObjectDetectionAnalyzer extends CommonAnalyzer<List<DetectedObject>> {

    private final ObjectDetector mDetector;

    public ObjectDetectionAnalyzer() {
        this(null);
    }

    public ObjectDetectionAnalyzer(ObjectDetectorOptionsBase options) {
        if (options != null) {
            mDetector = ObjectDetection.getClient(options);
        } else {
            // Live detection and tracking
            mDetector = ObjectDetection.getClient(new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()  // Optional
                    .build());

            // Multiple object detection in static images
//            mDetector = ObjectDetection.getClient(new ObjectDetectorOptions.Builder()
//                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
//                    .enableMultipleObjects()
//                    .enableClassification()  // Optional
//                    .build());
        }
    }

    @NonNull
    @Override
    protected Task<List<DetectedObject>> detectInImage(@NonNull InputImage inputImage) {
        return mDetector.process(inputImage);
    }
}
