package com.android.detection.detection;

import androidx.annotation.NonNull;

/**
 * 帧元数据
 */
public class FrameMetadata {

    private final int width;
    private final int height;
    private final int rotation;

    /**
     * 帧元数据的宽
     *
     * @return 帧元数据的宽
     */
    public int getWidth() {
        return width;
    }

    /**
     * 帧元数据的高
     *
     * @return 帧元数据的高
     */
    public int getHeight() {
        return height;
    }

    /**
     * 获取旋转角度
     *
     * @return 旋转角度
     */
    public int getRotation() {
        return rotation;
    }

    public FrameMetadata(int width, int height, int rotation) {
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    @NonNull
    @Override
    public String toString() {
        return "FrameMetadata{" +
                "width=" + width +
                ", height=" + height +
                ", rotation=" + rotation +
                '}';
    }
}
