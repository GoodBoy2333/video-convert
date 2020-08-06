package com.fy.cv.utils;
import com.fy.cv.config.ConvertConfig;
import com.jcraft.jsch.*;

import java.util.Properties;

/**
 * @author 方焱
 */
public class SFTPChannel {

    private static ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Channel> channelThreadLocal = new ThreadLocal<>();

    public static ChannelSftp getChannel(int timeout) throws JSchException {
        String ftpHost = ConvertConfig.SFTP_REQ_HOST;
        Integer ftpPort = ConvertConfig.SFTP_REQ_PORT;
        String ftpUserName = ConvertConfig.SFTP_REQ_USERNAME;
        String ftpPassword = ConvertConfig.SFTP_REQ_PASSWORD;
        // 创建JSch对象
        JSch jsch = new JSch();
        // 根据用户名，主机ip，端口获取一个Session对象
        Session session = jsch.getSession(ftpUserName, ftpHost, ftpPort);
//        System.out.println("Thread:" +Thread.currentThread().getId() + ",Session created.");
        if (ftpPassword != null) {
            // 设置密码
            session.setPassword(ftpPassword);
        }
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        // 为Session对象设置properties
        session.setConfig(config);
        // 设置timeout时间
        session.setTimeout(timeout);
        // 通过Session建立链接
        session.connect();
//        System.out.println("Thread:" + Thread.currentThread().getId() + ",Session connected.");

//        System.out.println("Thread:" + Thread.currentThread().getId() + ",Opening Channel.");
        // 打开SFTP通道
        Channel channel = session.openChannel("sftp");
        channel.connect(); // 建立SFTP通道的连接
//        System.out.println("Thread:" + Thread.currentThread().getId() + ",Connected successfully to ftpHost = " + ftpHost
//                + ",as ftpUserName = " + ftpUserName
//                + ", returning: " + channel);
        channelThreadLocal.set(channel);
        sessionThreadLocal.set(session);
        return (ChannelSftp) channel;
    }

    public static void closeChannel() throws Exception {
        Channel channel = channelThreadLocal.get();
        Session session = sessionThreadLocal.get();
        if (channel != null) {
            channel.disconnect();
            channelThreadLocal.remove();
//            System.out.println("Thread:" + Thread.currentThread().getId() + ",channelClose");
        }
        if (session != null) {
            session.disconnect();
            sessionThreadLocal.remove();
//            System.out.println("Thread:" + Thread.currentThread().getId() + ",sessionClose");
        }
    }
}
