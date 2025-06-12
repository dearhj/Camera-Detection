package com.android.detection.detection;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;

import com.android.detection.detection.analyze.Analyzer;

/**
 * 相机扫描基类定义；内置的默认实现见：{@link BaseCameraScan}
 */
@SuppressWarnings("unused")
public abstract class CameraScan<T> implements ICamera, ICameraControl {

    /**
     * A camera on the device facing the same direction as the device's screen.
     */
    public static int LENS_FACING_FRONT = CameraSelector.LENS_FACING_FRONT;
    /**
     * A camera on the device facing the opposite direction as the device's screen.
     */
    public static int LENS_FACING_BACK = CameraSelector.LENS_FACING_BACK;

    /**
     * 纵横比：4:3
     */
    public static final float ASPECT_RATIO_4_3 = 4.0F / 3.0F;
    /**
     * 纵横比：16:9
     */
    public static final float ASPECT_RATIO_16_9 = 16.0F / 9.0F;
    /**
     * 设置是否分析图像，默认为：true；通过此方法可以动态控制是否分析图像；在连续扫描识别时，可能会用到。
     * <p>
     * 如：当分析图像成功一次之后，如需继续连扫，可以在结果回调函数中等处理了自己的业务后，继续调用此方法并设置为true，就可以继续扫描分析图像了。
     *
     * @param analyze 是否分析图像
     * @return {@link CameraScan}
     */
    public abstract CameraScan<T> setAnalyzeImage(boolean analyze);

    /**
     * 设置是否自动停止分析图像；默认为：true；
     * <p>
     * 大多数情况下，单次扫描的场景应用较多；很容易忘记主动调用 {@link CameraScan#setAnalyzeImage(boolean)} 来停止分析。
     * <p>
     * 如果设置为：true；即：启用了自动停止分析图像：当分析图像成功一次之后；那么设置的分析图像会自动停止；如果此时
     * 需要继续分析图像，可以在结果回调里面调用 {@link CameraScan#setAnalyzeImage(boolean)} 来控制是否继续分析图像。
     * <p>
     * 如果设置为：false；即：禁用了自动停止分析图像：当分析图像成功一次之后；不会有任何变化；会继续分析图像。
     *
     * @param autoStopAnalyze 是否自动停止分析
     * @return {@link CameraScan}
     */
    public abstract CameraScan<T> setAutoStopAnalyze(boolean autoStopAnalyze);

    /**
     * 设置分析器，如果内置的一些分析器不满足您的需求，你也可以自定义{@link Analyzer}，
     * 自定义时，切记需在{@link #startCamera()}之前调用才有效。
     *
     * @param analyzer 分析器
     * @return {@link CameraScan}
     */
    public abstract CameraScan<T> setAnalyzer(Analyzer<T> analyzer);
    /**
     * 设置扫描结果回调
     *
     * @param callback 扫描结果回调
     * @return {@link CameraScan}
     */
    public abstract CameraScan<T> setOnScanResultCallback(OnScanResultCallback<T> callback);
    /**
     * 扫描结果回调
     *
     * @param <T> 扫描结果数据类型
     */
    public interface OnScanResultCallback<T> {
        /**
         * 扫描结果回调
         *
         * @param result 扫描结果
         */
        void onScanResultCallback(AnalyzeResult<T> result);

        /**
         * 扫描结果识别失败时触发此回调方法
         */
        default void onScanResultFailure() {

        }
    }

}
