package com.android.detection.detection;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.objects.DetectedObject;
import com.android.detection.camerascan.BaseCameraScanActivity;
import com.android.detection.camerascan.analyze.Analyzer;
import com.android.detection.detection.analyze.ObjectDetectionAnalyzer;

import java.util.List;

/**
 * 对象检测 - 相机扫描基类
 * <p>
 * 通过继承 {@link ObjectCameraScanActivity} 可快速实现对象检测
 */
public abstract class ObjectCameraScanActivity extends BaseCameraScanActivity<List<DetectedObject>> {
    @Nullable
    @Override
    public Analyzer<List<DetectedObject>> createAnalyzer() {
        return new ObjectDetectionAnalyzer();
    }

}
