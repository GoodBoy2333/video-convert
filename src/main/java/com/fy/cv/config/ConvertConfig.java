package com.fy.cv.config;

/**
 * @author 方焱
 */
public class ConvertConfig {
    /**
     * mencoder安装路径
     */
    public static String mencoderPath = "mencoder";

    /**
     * ffmpegPath安装路径
     */
    public static String ffmpegPath = "ffmpeg";

    /**
     * ffprobePath安装路径
     */
    public static String ffprobePath = "ffprobe";

    /**
     * ffmpeg转码线程数
     */
    public static String ffmpegThreadCount = "12";

    /**
     * 线程池大小
     */
    public static Integer corePoolSize = 3;

    /**
     * 队列大小
     */
    public static Integer queueSize = 100;

    /**
     * 转码琐
     */
    public static final String convertLock = "convertLock";
}
