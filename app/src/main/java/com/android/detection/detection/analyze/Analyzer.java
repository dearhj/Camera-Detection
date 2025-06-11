package com.android.detection.detection.analyze;

import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.android.detection.detection.AnalyzeResult;

/**
 * 分析器：主要用于分析相机预览的帧数据
 *
 * @param <T> 泛型T为分析成功后的具体结果
 */
@SuppressWarnings("unused")
public interface Analyzer<T> {
    /**
     * 分析图像并将分析的结果通过分析监听器返回
     *
     * @param imageProxy 需要分析的图像
     * @param listener   分析监听器，参见：{@link OnAnalyzeListener}
     */
    void analyze(@NonNull ImageProxy imageProxy, @NonNull OnAnalyzeListener<T> listener);
    void analyze(@NonNull Image imageProxy, @NonNull OnAnalyzeListener<T> listener);

    /**
     * Analyze listener
     *
     * @param <T> 泛型T为分析结果对应的对象
     */
    interface OnAnalyzeListener<T> {
        /**
         * 成功
         *
         * @param result 分析结果
         */
        void onSuccess(@NonNull AnalyzeResult<T> result);

        /**
         * 失败
         *
         * @param e 异常
         */
        void onFailure(@Nullable Exception e);
    }
}
