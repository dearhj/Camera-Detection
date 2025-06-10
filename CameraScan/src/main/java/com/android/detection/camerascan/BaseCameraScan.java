package com.android.detection.camerascan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.ResolutionInfo;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.activity.ComponentActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.android.detection.camerascan.analyze.Analyzer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 相机扫描基类；{@link BaseCameraScan} 为 {@link CameraScan} 的默认实现
 * <p>
 * 快速实现扫描识别主要有以下几种方式：
 * <p>
 * 1、通过继承 {@link BaseCameraScanActivity}或其子类，可快速实现扫描识别。
 * （适用于大多数场景，自定义布局时需覆写getLayoutId方法）
 * <p>
 * 2、在你项目的Activity或者Fragment中实例化一个{@link BaseCameraScan}。（适用于想在扫描界面写交互逻辑，又因为项目
 * 架构或其它原因，无法直接或间接继承{@link BaseCameraScanActivity}时使用）
 * <p>
 * 3、继承{@link CameraScan}自己实现一个，可参照默认实现类{@link BaseCameraScan}，其他步骤同方式2。（高级用法，谨慎使用）
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
    /**
     * 扫描结果回调
     */
    private OnScanResultCallback<T> mOnScanResultCallback;
    /**
     * 分析监听器
     */
    private Analyzer.OnAnalyzeListener<T> mOnAnalyzeListener;

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
    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        mExecutorService = Executors.newSingleThreadExecutor();
        mResultLiveData = new MutableLiveData<>();
        mResultLiveData.observe(mLifecycleOwner, result -> {
            if (result != null) {
                handleAnalyzeResult(result);
            } else if (mOnScanResultCallback != null) {
                mOnScanResultCallback.onScanResultFailure();
            }
        });

        mOnAnalyzeListener = new Analyzer.OnAnalyzeListener<T>() {
            @Override
            public void onSuccess(@NonNull AnalyzeResult<T> result) {
                mResultLiveData.postValue(result);
            }

            @Override
            public void onFailure(@Nullable Exception e) {
                mResultLiveData.postValue(null);
            }

        };
    }

    @Override
    public void startCamera() {
        mPreviewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        mCameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        mCameraProviderFuture.addListener(() -> {
            try {
                // 相机选择器
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                // 预览
                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(3072, 4096))
                        .build();
                // 设置SurfaceProvider
                preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

                // 设置图像分析分辨率
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(3072, 4096))
                        .build();

                imageAnalysis.setAnalyzer(mExecutorService, image -> {
                    if (isAnalyze && !isAnalyzeResult && mAnalyzer != null) {
                        mAnalyzer.analyze(image, mOnAnalyzeListener);
                    }
                    image.close();
                });
                if (mCamera != null) {
                    mCameraProviderFuture.get().unbindAll();
                }

                // 绑定到生命周期
                mCamera = mCameraProviderFuture.get().bindToLifecycle(mLifecycleOwner, cameraSelector, preview, imageAnalysis);

                ResolutionInfo previewResolutionInfo = preview.getResolutionInfo();
                System.out.println("执行这里了吗？？？？？   " + (previewResolutionInfo != null));
                if (previewResolutionInfo != null) {
                    System.out.println("这里的预览分辨率是？Preview resolution: " + previewResolutionInfo.getResolution());
                }
                ResolutionInfo imageResolutionInfo = imageAnalysis.getResolutionInfo();
                if (imageResolutionInfo != null) {
                    System.out.println("这里的回调分辨率是》ImageAnalysis resolution: " + imageResolutionInfo.getResolution());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(mContext));
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
        if (mCameraProviderFuture != null) {
            try {
                mCameraProviderFuture.get().unbindAll();
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
    }

    @Override
    public boolean isTorchEnabled() {
        return false;
    }

    @Override
    public boolean hasFlashUnit() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public CameraScan<T> setOnScanResultCallback(OnScanResultCallback<T> callback) {
        this.mOnScanResultCallback = callback;
        return this;
    }

    @Nullable
    @Override
    public Camera getCamera() {
        return mCamera;
    }

    @Override
    public void release() {
        isAnalyze = false;
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
        stopCamera();
    }

    @Override
    public CameraScan<T> bindFlashlightView(@Nullable View flashlightView) {
        return this;
    }

}
