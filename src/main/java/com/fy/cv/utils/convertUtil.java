package com.fy.cv.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fy.cv.config.ConvertConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 方焱
 */
public class convertUtil {

    public static String ffprobePath = ConvertConfig.ffprobePath;

    public static String getTempPath() {
        Path path = Paths.get("/temp");
        try {
            if (Files.notExists(path)) {
                Files.createDirectory(path);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "";
        }
        return path.toAbsolutePath().toString();
    }

    public static String checkFileParentDir(String fileAbsolutePath) {
        Path path = Paths.get(fileAbsolutePath);
        try {
            Path parent = path.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "";
        }
        return path.toAbsolutePath().toString();
    }

    public static JSONObject getDetail(String sourceUrl) {
        JSONObject jsonObject = new JSONObject();
        List<String> commend = new ArrayList<>();
        commend.add(ffprobePath);
        commend.add("-hide_banner");
        commend.add("-print_format");
        commend.add("json");
        commend.add("-show_streams");
        commend.add("-select_streams");
        commend.add("v");
        commend.add(sourceUrl);
        try {
            ProcessBuilder builder = new ProcessBuilder();
            String cmd = commend.toString();
            builder.command(commend);
            Process p = builder.start();
            StringBuffer stringBuffer = doWaitFor(p);
            String result = new String(stringBuffer);
            if (StringUtils.isBlank(result)) {
                return jsonObject;
            }
            jsonObject = JSONObject.parseObject(new String(stringBuffer));
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 获取主要信息参数（持续时间（duration）、宽度（width）、高度（height）
     *
     * @param sourceUrl
     * @return
     */
    public static Map getMainInfo(String sourceUrl) {
        Map result = new HashMap();
        JSONObject detail = getDetail(sourceUrl);
        if (!detail.containsKey("streams")) {
            return result;
        }
        JSONArray streams = detail.getJSONArray("streams");
        if (streams.size() < 0) {
            return result;
        }
        JSONObject jsonObject = streams.getJSONObject(0);
        if (jsonObject.containsKey("duration")) {
            result.put("duration", jsonObject.get("duration"));
        }
        if (jsonObject.containsKey("width")) {
            result.put("width", jsonObject.get("width"));
        }
        if (jsonObject.containsKey("height")) {
            result.put("height", jsonObject.get("height"));
        }
        return result;
    }

    public static Map checkConvertTask(BizConvertTask task) {
        Map result = new HashMap();
        result.put("code", 0);
        if (StringUtils.isBlank(task.getSourceUrl()) ||
                StringUtils.isBlank(task.getTargetPath()) ||
                StringUtils.isBlank(task.getResolution()) ||
                StringUtils.isBlank(task.getBps()) ||
                StringUtils.isBlank(task.getSourceUrl())) {
            result.put("msg", "核心参数丢失");
            return result;
        }
        // 检查分辨率
        String[] split = task.getResolution().split("\\*");
        if (split.length < 2) {
            result.put("msg", "分辨率设置错误。示例：1920*1080");
            return result;
        }
        long taskWidth = Long.valueOf(split[0]);
        long taskHeight = Long.valueOf(split[1]);
        Map mainInfo = getMainInfo(task.getSourceUrl());
        long width = Long.parseLong(String.valueOf(mainInfo.getOrDefault("width", "0")));
        long height = Long.parseLong(String.valueOf(mainInfo.getOrDefault("height", "0")));
        int duration = (int) Double.parseDouble(String.valueOf(mainInfo.getOrDefault("duration", "0")));
        if (width != 0 && taskWidth > width) {
            result.put("msg", "分辨率设置错误,超出原视频宽度。");
            return result;
        }
        if (height != 0 && taskHeight > height) {
            result.put("msg", "分辨率设置错误,超出原视频高度。");
            return result;
        }
        try {
            double v = Double.parseDouble(task.getBps());
            if (v > 10) {
                result.put("msg", "转码码率设置过大。最大值：10");
                return result;
            }
        } catch (Exception e) {
            result.put("msg", "转码码率设置错误。示例：1.5");
            return result;
        }
        // 设置内容长度
        task.setContentLength(duration);
        // 检查父路径是否存在
        checkFileParentDir(task.getTargetPath());
        // 设置任务id
        task.setTaskJobId(StringUtils.getRandomLowerCaseUUID());
        // 设置转码状态
        task.setConvertStatus("TYPE_CONVERT");
        result.put("code", 1);
        return result;
    }

    public static StringBuffer doWaitFor(Process p) {
        InputStream inputStream = p.getInputStream();
        InputStream errorStream = p.getErrorStream();
        StringBuffer res = new StringBuffer();
        // returned to caller when p is finished
        int exitValue = -1;
        try {
            boolean finished = false;
            while (!finished) {
                try {
                    if (inputStream.available() > 0) {
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(inputStream));
                        String str;
                        while ((str = bufferedReader.readLine()) != null) {
                            res.append(str);
                        }
                        bufferedReader.close();
                    } else if (errorStream.available() > 0) {
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(inputStream));
                        String str;
                        while ((str = bufferedReader.readLine()) != null) {
                            res.append(str);
                        }
                        bufferedReader.close();
                    }

                    exitValue = p.exitValue();
                    finished = true;

                } catch (IllegalThreadStateException e) {
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            System.err.println("doWaitFor();: unexpected exception - " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }
        return res;
    }
}
