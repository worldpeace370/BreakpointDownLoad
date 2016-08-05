package com.lebron.breakpointdownload.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.lebron.breakpointdownload.bean.DownloadInfo;
import com.lebron.breakpointdownload.bean.FileInfo;
import com.lebron.breakpointdownload.service.DownloadService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**单任务单线程下载的封装类
 * Created by wuxiangkun on 2016/8/4 19:35.
 * Contacts wuxiangkun@live.com
 */
public class DownloadTask {
    private static final String TAG = "DownloadTask";
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private DownloadInfoManagerImpl mDownloadInfoManagerImpl;

    private boolean isPause = false;

    public DownloadTask(Context context, FileInfo fileInfo) {
        mContext = context;
        mFileInfo = fileInfo;
        mDownloadInfoManagerImpl = new DownloadInfoManagerImpl(context);
    }

    /**
     * 单任务单线程下载，先从数据库中获得文件的下载信息，如果没有再创建，说明不是断点下载情况
     * 断点下载会进行下载信息(DownloadInfo)的数据库存储
     */
    public void downloadInOneThread() {
        List<DownloadInfo> downloadInfoList = mDownloadInfoManagerImpl.getDownloadInfoList(mFileInfo.getUrl());
        DownloadInfo downloadInfo;
        //如果数据库中找不到该文件的下载信息，则创建该文件的下载信息
        if (downloadInfoList.size() == 0){
            downloadInfo = new DownloadInfo(mFileInfo.getId(), mFileInfo.getUrl(),
                    0, mFileInfo.getLength(), 0, 0);
            //插入数据库
            mDownloadInfoManagerImpl.insertDownloadInfo(downloadInfo);
        }else {
            downloadInfo = downloadInfoList.get(0);
        }
        //将downloadInfo放入线程开始下载
        new DownloadThread(downloadInfo).start();
    }

    public void setPause(boolean isPause){
        this.isPause = isPause;
    }

    class DownloadThread extends Thread{
        private  DownloadInfo mDownloadInfo;

        public DownloadThread(DownloadInfo downloadInfo) {
            mDownloadInfo = downloadInfo;
        }

        @Override
        public void run() {
            long mFinished = 0;
            HttpURLConnection connection = null;
            RandomAccessFile raf = null;
            InputStream is = null;
            try {
                URL url = new URL(mDownloadInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("GET");
                //设置下载起始位置
                long start = mDownloadInfo.getStart() + mDownloadInfo.getFinished();
                connection.setRequestProperty("Range", "bytes=" + start + "-" + mDownloadInfo.getEnd());
                Log.i("Range", "start = " + mDownloadInfo.getStart() + ", finish = " + mDownloadInfo.getFinished() + ", end = " + mDownloadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                //用于发送广播，将下载的进度更新到UI中，当然也能用观察者模式
                Intent finishingIntent = new Intent(DownloadService.ACTION_UPDATE);
                Bundle finishingBundle = new Bundle();
                mFinished += mDownloadInfo.getFinished();
                //HTTP_PARTIAL HTTP/206 “Partial Content”响应是在客户端表明自己只需要目标URL上的部分资源的时候返回的.
                // 这种情况经常发生在客户端继续请求一个未完成的下载的时候
                if (connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
                    //读取数据
                    is = connection.getInputStream();
                    byte[] buffer = new byte[1024 * 8];
                    int len;
                    //每1s更新一次广播，不能太快了
                    long time = System.currentTimeMillis();
                    int progress = 0;
                    while ((len = is.read(buffer)) != -1) {
                        //如果暂停了，更新mDownloadInfo到数据库
                        if (isPause){
                            mDownloadInfoManagerImpl.updateDownloadInfo(mDownloadInfo.getUrl(), mDownloadInfo.getId(),
                                    mFinished, progress);
                            return;
                        }

                        //写入文件
                        raf.write(buffer, 0, len);
                        //把下载进度发送广播给Activity
                        mFinished += len;
                        if (System.currentTimeMillis() - time > 1000) {//减少UI负载
                            time = System.currentTimeMillis();
                            progress = (int)(mFinished * 100 / mFileInfo.getLength());
                            finishingBundle.putInt("finishing", progress);
                            finishingBundle.putInt("id", mFileInfo.getId());
                            finishingIntent.putExtras(finishingBundle);
                            mContext.sendBroadcast(finishingIntent);
                        }
                    }
                    Intent finishedIntent = new Intent(DownloadService.ACTION_FINISHED);
                    finishedIntent.putExtra("finished",100);
                    finishingIntent.putExtra("id", mFileInfo.getId());
                    mContext.sendBroadcast(finishedIntent);
                    //删除线程信息
                    mDownloadInfoManagerImpl.deleteDownloadInfo(mDownloadInfo.getUrl(), mDownloadInfo.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (is != null){
                    try {
                        is.close();
                        raf.close();
                        connection.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
