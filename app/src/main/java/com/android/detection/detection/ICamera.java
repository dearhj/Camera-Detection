package com.android.detection.detection;

import androidx.annotation.Nullable;
import androidx.camera.core.Camera;

/**
 * 相机定义
 */
public interface ICamera {

    /**
     * 启动相机预览
     */
    void startCamera();

    /**
     * 停止相机预览
     */
    void stopCamera();

    void takePhoto();

    /**
     * 获取 {@link Camera}
     *
     * @return {@link Camera}
     */
    @Nullable
    Camera getCamera();

    /**
     * 释放
     */
    void release();

}
