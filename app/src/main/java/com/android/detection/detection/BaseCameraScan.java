package com.android.detection.detection;

import static android.content.Context.SENSOR_SERVICE;
import static com.android.detection.detection.CameraConfig.CAMERA_HEIGHT;
import static com.android.detection.detection.CameraConfig.CAMERA_WIDTH;
import static com.android.detection.detection.CameraConfig.TAKE_PICTURE_HEIGHT;
import static com.android.detection.detection.CameraConfig.TAKE_PICTURE_WIDTH;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ResolutionInfo;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.android.detection.detection.analyze.Analyzer;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 相机扫描基类；{@link BaseCameraScan} 为 {@link CameraScan} 的默认实现
 */
public class BaseCameraScan<T> extends CameraScan<T> {
    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    /**
     * 预览视图
     */
    private final PreviewView mPreviewView;

    private ExecutorService mExecutorService;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    /**
     * 相机
     */
    private Camera mCamera;
    /**
     * 分析器
     */
    private Analyzer<T> mAnalyzer;
    /**
     * 是否分析
     */
    private volatile boolean isAnalyze = true;
    /**
     * 是否自动停止分析
     */
    private volatile boolean isAutoStopAnalyze = true;
    /**
     * 是否已经分析出结果
     */
    private volatile boolean isAnalyzeResult;
    /**
     * 分析结果
     */
    private MutableLiveData<AnalyzeResult<T>> mResultLiveData;

    private MutableLiveData<Integer> mRotationLiveData;
    /**
     * 扫描结果回调
     */
    private OnScanResultCallback<T> mOnScanResultCallback;
    /**
     * 分析监听器
     */
    private Analyzer.OnAnalyzeListener<T> mOnAnalyzeListener;

    private ImageCapture imageCapture;
    private CameraSelector cameraSelector;
    private Preview preview;
    private ImageAnalysis imageAnalysis;

    private ProcessCameraProvider processCameraProvider;

    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private SensorEventListener sensorEventListener;
    private int currentGravityValue = 0;
    private int gravityValue = 0;

    public BaseCameraScan(@NonNull ComponentActivity activity, @NonNull PreviewView previewView) {
        this(activity, activity, previewView);
    }

    public BaseCameraScan(@NonNull Fragment fragment, @NonNull PreviewView previewView) {
        this(fragment.requireContext(), fragment.getViewLifecycleOwner(), previewView);
    }

    public BaseCameraScan(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner, @NonNull PreviewView previewView) {
        this.mContext = context;
        this.mLifecycleOwner = lifecycleOwner;
        this.mPreviewView = previewView;
        initData();
    }

    /**
     * 初始化
     */
    private long currentTime = 0L;

    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        mExecutorService = Executors.newSingleThreadExecutor();
        mResultLiveData = new MutableLiveData<>();
        mResultLiveData.observe(mLifecycleOwner, result -> {
            long time = System.currentTimeMillis();
            if ((time - currentTime) > 100) {
                currentTime = time;
                handleAnalyzeResult(result);
            } else if (mOnScanResultCallback != null) {
                mOnScanResultCallback.onScanResultFailure();
            }
        });
        mRotationLiveData = new MutableLiveData<>();
        mRotationLiveData.observe(mLifecycleOwner, rotation -> {
            if (currentGravityValue != rotation) {
                currentGravityValue = rotation;
                System.out.println("这里方向变化了。   $rotation " + rotation);
                try {
                    imageCapture = new ImageCapture.Builder()
                            .setTargetResolution(new Size(TAKE_PICTURE_WIDTH, TAKE_PICTURE_HEIGHT))
                            .setTargetRotation(rotation)
                            .build();
                    // 重新绑定到生命周期
                    if (mCamera != null) processCameraProvider.unbindAll();
                    mCamera = processCameraProvider.bindToLifecycle(
                            mLifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis,
                            imageCapture
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mOnAnalyzeListener = new Analyzer.OnAnalyzeListener<>() {
            @Override
            public void onSuccess(@NonNull AnalyzeResult<T> result) {
                mResultLiveData.postValue(result);
            }

            @Override
            public void onFailure(Exception e) {
                mResultLiveData.postValue(null);
            }

        };

        sensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    if (x > 3) gravityValue = 3;
                    else if (x < -3) gravityValue = 1;
                    else if (y > 3) gravityValue = 0;
                    else if (y < -3) gravityValue = 2;
                    else if (x >= -1.0 && x <= 1.0 && y >= -1.0 && y <= 1.0) gravityValue = 0;
//                    mRotationLiveData.postValue(gravityValue);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }

    @Override
    public void startCamera() {
        mPreviewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        mCameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        mCameraProviderFuture.addListener(() -> {
            try {
                processCameraProvider = mCameraProviderFuture.get();
                // 相机选择器
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                // 预览
                preview = new Preview.Builder()
                        .setTargetResolution(new Size(CAMERA_WIDTH, CAMERA_HEIGHT))
                        .build();
                // 设置SurfaceProvider
                preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

                // 设置图像分析分辨率
                imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(CAMERA_WIDTH, CAMERA_HEIGHT))
                        .build();

                imageAnalysis.setAnalyzer(mExecutorService, image -> {
                    if (isAnalyze && !isAnalyzeResult && mAnalyzer != null) {
                        mAnalyzer.analyze(image, mOnAnalyzeListener);
                    }
                    image.close();
                });
                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(Surface.ROTATION_0)
                        .setTargetResolution(new Size(TAKE_PICTURE_WIDTH, TAKE_PICTURE_HEIGHT))
                        .build();
                if (mCamera != null) processCameraProvider.unbindAll();

                // 绑定到生命周期
                mCamera = processCameraProvider.bindToLifecycle(mLifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture);

                ResolutionInfo previewResolutionInfo = preview.getResolutionInfo();
                if (previewResolutionInfo != null) {
                    System.out.println("这里的预览分辨率是？Preview resolution: " + previewResolutionInfo.getResolution());
                }
                ResolutionInfo imageResolutionInfo = imageAnalysis.getResolutionInfo();
                if (imageResolutionInfo != null) {
                    System.out.println("这里的分析分辨率是》ImageAnalysis resolution: " + imageResolutionInfo.getResolution());
                }
                ResolutionInfo jpgResolutionInfo = imageCapture.getResolutionInfo();
                if (jpgResolutionInfo != null) {
                    System.out.println("这里的拍照分辨率是》imageCapture resolution: " + jpgResolutionInfo.getResolution());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(mContext));
        sensorManager.registerListener(
                sensorEventListener,
                gravitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    @Override
    public void takePhoto() {
        if (imageCapture != null) {
            String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");


            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                    mContext.getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
            ).build();

            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(mContext),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                            System.out.println("这里路径是： " + msg);
                            Toast.makeText(mContext, "拍照成功", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            System.out.println("这里什么原因？   " + exception.getMessage());
                            Toast.makeText(mContext, "拍照失败", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * 处理分析结果
     *
     * @param result 分析结果
     */
    private synchronized void handleAnalyzeResult(AnalyzeResult<T> result) {
        if (isAnalyzeResult || !isAnalyze) {
            return;
        }
        isAnalyzeResult = true;
        if (isAutoStopAnalyze) {
            isAnalyze = false;
        }
        if (mOnScanResultCallback != null) {
            mOnScanResultCallback.onScanResultCallback(result);
        }
        isAnalyzeResult = false;
    }

    @Override
    public void stopCamera() {
        sensorManager.unregisterListener(sensorEventListener);
        if (mCameraProviderFuture != null && processCameraProvider != null) {
            try {
                processCameraProvider.unbindAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public CameraScan<T> setAnalyzeImage(boolean analyze) {
        isAnalyze = analyze;
        return this;
    }

    @Override
    public CameraScan<T> setAutoStopAnalyze(boolean autoStopAnalyze) {
        isAutoStopAnalyze = autoStopAnalyze;
        return this;
    }

    @Override
    public CameraScan<T> setAnalyzer(Analyzer<T> analyzer) {
        mAnalyzer = analyzer;
        return this;
    }

    @Override
    public void enableTorch(boolean torch) {
        try {
            int flashMode = ImageCapture.FLASH_MODE_OFF;
            if (torch) flashMode = ImageCapture.FLASH_MODE_ON;
            imageCapture = new ImageCapture.Builder()
                    .setTargetResolution(new Size(TAKE_PICTURE_WIDTH, TAKE_PICTURE_HEIGHT))
                    .setFlashMode(flashMode)
                    .build();
            // 重新绑定到生命周期
            if (mCamera != null) processCameraProvider.unbindAll();
            mCamera = processCameraProvider.bindToLifecycle(
                    mLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CameraScan<T> setOnScanResultCallback(OnScanResultCallback<T> callback) {
        this.mOnScanResultCallback = callback;
        return this;
    }

    @Override
    public void release() {
        isAnalyze = false;
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
        stopCamera();
    }

}
