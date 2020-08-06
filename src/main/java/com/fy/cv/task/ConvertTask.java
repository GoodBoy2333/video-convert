package com.fy.cv.task;

import com.fy.cv.utils.SFTPChannel;
import com.fy.cv.utils.convertUtil;
import com.fy.cv.config.ConvertConfig;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 方焱
 */
public class ConvertTask implements Runnable, SftpProgressMonitor, Comparable<ConvertTask> {
    /**
     * 表主键ID
     */
    private Integer id;

    /**
     * 业务主键ID(32位小写UUID)
     */
    private String bizCode;

    /**
     * 资源ID(关联res_base_resource.biz_code)
     */
    private String resCode;

    /**
     * 是否已删除(0:未删除1:已删除)
     */
    private Short isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者用户名(关联sys_user.user_name)
     */
    private String createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 最后更新者用户名(关联sys_user.user_name)
     */
    private String lastUpdateBy;

    /**
     * 转码任务ID
     */
    private String taskJobId;

    /**
     * 源文件URL路径
     */
    private String sourceUrl;

    /**
     * 输出目标文件路径
     */
    private String targetPath;

    /**
     * 转码状态(取值字典表)
     */
    private String convertStatus;

    /**
     * 转码分辨率
     */
    private String resolution;

    /**
     * 转码码率(单位：M)
     */
    private String bps;

    /**
     * 错误信息
     */
    private String errMsg;

    /**
     * 转码开始时间
     */
    private Date beginTime;

    /**
     * 转码结束时间
     */
    private Date endTime;

    /**
     * 转码优先级(数值越大,优先级越高)
     */
    private Integer taskSort;

    /**
     * 内容长度(单位：秒)
     */
    private Integer contentLength;
    /**
     * 转码相关配置路径
     */
    private String ffmpegPath = ConvertConfig.ffmpegPath;
    private String ffmpegThreadCount = ConvertConfig.ffmpegThreadCount;
    private String mencoderPath = ConvertConfig.mencoderPath;

    /**
     * 转码相关查询参数
     */
    private Number speed = new Integer(0);
    private String fileName = "暂未获取";
    private volatile boolean isCompleted = false;

    /**
     * 文件传输相关查询参数
     */
    private long transferSpeed = 0;
    /**
     * 是否传输成功
     */
    private volatile boolean transferCompleted = false;
    private long count = 0;
    private long max = 0;

    @Override
    public void run() {
        try {
            String filerealname = sourceUrl.substring(sourceUrl.lastIndexOf("/") + 1, sourceUrl.lastIndexOf(".")).toLowerCase();
            setFileName(filerealname);
            isCompleted = process();
            if (isCompleted) {
                transferFile();
            }
        } catch (Exception e) {
            setErrMsg(e.getMessage());
            return;
        }
    }

    public void transferFile() {
        try {
            ChannelSftp channel = null;
            // 1、建立连接
            try {
                channel = SFTPChannel.getChannel(60 * 1000);
            } catch (JSchException jSchException) {
                setErrMsg(jSchException.getMessage());
                jSchException.printStackTrace();
            }
            // 2、建立目录
            if (!StringUtils.isBlank(getTargetPath()) && getTargetPath().indexOf("/") != -1 && channel != null) {
                String path = getTargetPath().substring(0, getTargetPath().lastIndexOf("/"));
                try {
                    channel.ls(path);
                } catch (SftpException e) {
                    channel.mkdir(path);
                    setErrMsg(e.getMessage());
                }
            }
            // 3、传输文件
            try {
                channel.put(getTargetPath(), getTargetPath(), this, ChannelSftp.OVERWRITE);
            } catch (SftpException sftpException) {
                setErrMsg(sftpException.getMessage());
            } finally {
                SFTPChannel.closeChannel();
            }
        } catch (Exception e) {
            setErrMsg(e.getMessage());
        }
    }

    /**
     * 实际转换视频格式的方法
     */
    private boolean process() {
        int type = checkContentType();
        boolean status = false;
        if (type == 0) {
            //如果type为0用ffmpeg直接转换
            status = processVideoFormat(sourceUrl);
        } else if (type == 1) {
            //如果type为1，将其他文件先转换为avi，然后在用ffmpeg转换为指定格式
            String avifilepath = processAVI();
            if (avifilepath == null) {
                // avi文件没有得到
                setErrMsg("转换AVI格式失败");
                return false;
            } else {
                status = processVideoFormat(avifilepath);
                try {
                    // 删除临时文件
                    Files.deleteIfExists(Paths.get(avifilepath));
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        return status;
    }

    /**
     * 检查文件类型
     *
     * @return
     */
    private int checkContentType() {
        String type = sourceUrl.substring(sourceUrl.lastIndexOf(".") + 1, sourceUrl.length()).toLowerCase();
        // ffmpeg能解析的格式：（asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等）
        if (type.equals("avi")) {
            return 0;
        } else if (type.equals("mpg")) {
            return 0;
        } else if (type.equals("wmv")) {
            return 0;
        } else if (type.equals("3gp")) {
            return 0;
        } else if (type.equals("mov")) {
            return 0;
        } else if (type.equals("mp4")) {
            return 0;
        } else if (type.equals("asf")) {
            return 0;
        } else if (type.equals("asx")) {
            return 0;
        } else if (type.equals("flv")) {
            return 0;
        }
        // 对ffmpeg无法解析的文件格式(wmv9，rm，rmvb等),
        // 可以先用别的工具（mencoder）转换为avi(ffmpeg能解析的)格式.
        else if (type.equals("wmv9")) {
            return 1;
        } else if (type.equals("rm")) {
            return 1;
        } else if (type.equals("rmvb")) {
            return 1;
        }
        return 9;
    }

    /**
     * 对ffmpeg无法解析的文件格式(wmv9，rm，rmvb等), 可以先用别的工具（mencoder）转换为avi(ffmpeg能解析的)格式.
     *
     * @return
     */
    private String processAVI() {
        String tempPath = convertUtil.getTempPath();
        if (StringUtils.isBlank(tempPath)) {
            return null;
        }
        String fileAbsolutePath = tempPath + StringUtils.getRandomLowerCaseUUID() + ".avi";
        List<String> commend = new ArrayList<>();
        commend.add(mencoderPath);
        commend.add(sourceUrl);
        commend.add("-oac");
        commend.add("mp3lame");
        commend.add("-lameopts");
        commend.add("preset=64");
        commend.add("-ovc");
        commend.add("xvid");
        commend.add("-xvidencopts");
        commend.add("bitrate=600");
        commend.add("-of");
        commend.add("avi");
        commend.add("-o");
        commend.add(fileAbsolutePath);
        // 命令类型：mencoder 1.rmvb -oac mp3lame -lameopts preset=64 -ovc xvid
        // -xvidencopts bitrate=600 -of avi -o rmvb.avi
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(commend);
            Process p = builder.start();
            doWaitFor(p);
            return fileAbsolutePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 转换为指定格式
     * ffmpeg能解析的格式：（asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等）
     *
     * @param oldfilepath
     * @return
     */
    private boolean processVideoFormat(String oldfilepath) {
        //ffmpeg -hide_banner -i h:/test.mp4 -s 540*320 -b:v 0.5M -y -threads 2 h:/result540_320.mp4
        List<String> commend = new ArrayList<>();
        commend.add(ffmpegPath);
        commend.add("-hide_banner");
        commend.add("-i");
        commend.add(oldfilepath);
        commend.add("-s");
        commend.add(resolution);
        commend.add("-b:v");
        commend.add(bps + "M");
        commend.add("-y");
        commend.add("-threads");
        commend.add(ffmpegThreadCount);
        commend.add(targetPath);
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(commend);
            Process p = builder.start();
            doWaitFor(p);
            p.destroy();
            return true;
        } catch (Exception e) {
            setErrMsg("命令:" + commend + "执行失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public int doWaitFor(Process p) {
        InputStream inputStream = p.getInputStream();
        InputStream errorStream = p.getErrorStream();

        // returned to caller when p is finished
        int exitValue = -1;
        try {
            boolean finished = false;
            while (!finished) {
                try {
                    if (inputStream.available() > 0) {
                        setPercentage(inputStream);
                    } else if (errorStream.available() > 0) {
                        setPercentage(errorStream);
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
        return exitValue;
    }

    public void setPercentage(InputStream inputStream) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream));
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            String subStr = "time=";
            int index = str.indexOf(subStr);
            if (index > 0) {
                int firstIndex = index + subStr.length();
                int lastIndex = firstIndex + 8;
                String time = str.substring(firstIndex, lastIndex);

                long second = 0;
                if (time.length() == 8) { //时分秒格式00:00:00
                    int index1 = time.indexOf(":");
                    int index2 = time.indexOf(":", index1 + 1);
                    second = Integer.parseInt(time.substring(0, index1)) * 3600;//小时
                    second += Integer.parseInt(time.substring(index1 + 1, index2)) * 60;//分钟
                    second += Integer.parseInt(time.substring(index2 + 1));//秒
                }
                if (time.length() == 5) {//分秒格式00:00
                    second = Integer.parseInt(time.substring(time.length() - 2)); //秒  后两位肯定是秒
                    second += Integer.parseInt(time.substring(0, 2)) * 60;    //分钟
                }

                Number result = (float) second / (float) getContentLength() * 100;
                setSpeed(result.intValue());

                System.out.println("-------------------任务id：" + getTaskJobId() + "--------【" + result.intValue() + "】--------");
            } else {
                // 如果读不出进度就先演示一下
                setSpeed(80);
            }
        }
        bufferedReader.close();
    }

    @Override
    public void init(int op, String src, String dest, long max) {
        this.max = max;
        this.count = 0;
        this.setSpeed(new Integer(100));
    }

    @Override
    public boolean count(long count) {
        this.count += count;
        if (transferSpeed >= this.count * 100 / max) {
            return true;
        }
        transferSpeed = this.count * 100 / max;
        System.out.println("Completed " + this.count + "(" + transferSpeed + "%) out of " + max + ".");
        return true;
    }

    @Override
    public void end() {
        transferCompleted = true;
    }

    @Override
    public int compareTo(ConvertTask o) {
        if (this.getTaskSort() < o.getTaskSort()) {
            return 1;
        } else if (this.getTaskSort() > o.getTaskSort()) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return taskJobId;
    }

    public ConvertTask(Integer id, String bizCode, String resCode, Short isDelete, Date createTime, String createBy, Date updateTime, String lastUpdateBy, String taskJobId, String sourceUrl, String targetPath, String convertStatus, String resolution, String bps, String errMsg, Date beginTime, Date endTime, Integer taskSort, Integer contentLength) {
        this.id = id;
        this.bizCode = bizCode;
        this.resCode = resCode;
        this.isDelete = isDelete;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.lastUpdateBy = lastUpdateBy;
        this.taskJobId = taskJobId;
        this.sourceUrl = sourceUrl;
        this.targetPath = targetPath;
        this.convertStatus = convertStatus;
        this.resolution = resolution;
        this.bps = bps;
        this.errMsg = errMsg;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.taskSort = taskSort;
        this.contentLength = contentLength;
    }

    public ConvertTask() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public String getResCode() {
        return resCode;
    }

    public void setResCode(String resCode) {
        this.resCode = resCode;
    }

    public Short getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Short isDelete) {
        this.isDelete = isDelete;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getLastUpdateBy() {
        return lastUpdateBy;
    }

    public void setLastUpdateBy(String lastUpdateBy) {
        this.lastUpdateBy = lastUpdateBy;
    }

    public String getTaskJobId() {
        return taskJobId;
    }

    public void setTaskJobId(String taskJobId) {
        this.taskJobId = taskJobId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getConvertStatus() {
        return convertStatus;
    }

    public void setConvertStatus(String convertStatus) {
        this.convertStatus = convertStatus;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getBps() {
        return bps;
    }

    public void setBps(String bps) {
        this.bps = bps;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getTaskSort() {
        return taskSort;
    }

    public void setTaskSort(Integer taskSort) {
        this.taskSort = taskSort;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    public void setContentLength(Integer contentLength) {
        this.contentLength = contentLength;
    }

    public Number getSpeed() {
        return speed;
    }

    public void setSpeed(Number speed) {
        this.speed = speed;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public long getTransferSpeed() {
        return transferSpeed;
    }

    public void setTransferSpeed(long transferSpeed) {
        this.transferSpeed = transferSpeed;
    }

    public boolean isTransferCompleted() {
        return transferCompleted;
    }

    public void setTransferCompleted(boolean transferCompleted) {
        this.transferCompleted = transferCompleted;
    }
}
