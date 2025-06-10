package com.android.detection.camerascan;

/**
 * 相机控制：主要包括调节焦距和闪光灯控制
 */
@SuppressWarnings("unused")
public interface ICameraControl {

    /**
     * 设置闪光灯（手电筒）是否开启
     *
     * @param torch 是否开启闪光灯（手电筒）
     */
    void enableTorch(boolean torch);

    /**
     * 闪光灯（手电筒）是否开启
     *
     * @return 闪光灯（手电筒）是否开启
     */
    boolean isTorchEnabled();

    /**
     * 是否支持闪光灯
     *
     * @return 是否支持闪光灯
     */
    boolean hasFlashUnit();
}
