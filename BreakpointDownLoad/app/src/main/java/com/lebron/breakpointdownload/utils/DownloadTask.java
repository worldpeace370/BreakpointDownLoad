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
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**单任务单线程下载的封装类
 * Created by wuxiangkun on 2016/8/4 19:35.
 * Contacts wuxiangkun@live.com
 */
public class DownloadTask {
    private static final String TAG = "DownloadTask";
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private DownloadInfoManagerImpl mDownloadInfoManagerImpl;
    //线程池
    public ExecutorService sExecutorService = Executors.newCachedThreadPool();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(7, 10, 200, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5));
    private volatile boolean mIsPause = false;
    //将每个任务默认拆分成3个线程进行下载
    private int mThreadCount = 2;
    //表示mThreadCount个线程下载的总完成量
    private long  mFinished = 0;
    private List<DownloadThreadForMuch> mThreadForMuchList = new ArrayList<>();

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
        new DownloadThreadForOne(downloadInfo).start();
    }
    /**
     * 单任务多线程下载，先从数据库中获得文件的下载信息，如果没有再创建，说明不是断点下载情况
     * 断点下载会进行下载信息(DownloadInfo)的数据库存储
     */
    public void downloadInMuchThread(){
        List<DownloadInfo> downloadInfoList = mDownloadInfoManagerImpl.getDownloadInfoList(mFileInfo.getUrl());
        if (downloadInfoList.size() == 0){
            //获得每个线程下载的长度
            long length = mFileInfo.getLength() / mThreadCount;
            for (int i = 0; i < mThreadCount; i++) {
                DownloadInfo downloadInfo = new DownloadInfo(i, mFileInfo.getUrl(), length * i,
                        (i + 1) * length -1, 0, 0);
                //如果是文件的最后一段，则设置end为文件的长度值，否则文件会少一字节
                if (i + 1 == mThreadCount){
                    downloadInfo.setEnd(mFileInfo.getLength());
                }
                downloadInfoList.add(downloadInfo);
                //插入数据库
                mDownloadInfoManagerImpl.insertDownloadInfo(downloadInfo);
            }
        }
        //启动多个线程进行下载
        for (DownloadInfo e : downloadInfoList) {
            Log.i(TAG, "DownloadInfo: " + e);
            DownloadThreadForMuch downloadThread = new DownloadThreadForMuch(e);
            //添加线程到集合中
            mThreadForMuchList.add(downloadThread);
            //加入到线程池开始执行
            executor.execute(downloadThread);
        }
    }

    /**
     * 单任务单线程这种方法好使
     * @param isPause true or false
     */
    public void setPause(boolean isPause){
        this.mIsPause = isPause;
    }

    /**
     * 单任务多线程情况下使用
     * @param isPause true or false mean Pause or Not Pause
     */
    public void sendMessageToThreadForPause(boolean isPause){
        ObserverManager.getObserverManager().sendMessage(isPause);
    }

    /**
     * 针对单任务多线程的方式，这个涉及到进度的更新和进度的保存，有点烧脑啊
     * 进度更新是整体下载进度，要把多个线程下载的进度累加然后广播出去，用全局多线程共享变量mFinished进行总体下载字节的记录，然后求得总体进度
     * 暂停后保存进度，这个时候需要保存单任务中每个线程的进度，此时的进度用thisProgress来保存
     */
    class DownloadThreadForMuch extends Thread implements Observer{
        private DownloadInfo mDownloadInfo;
        public boolean isFinished = false;
        public volatile boolean isPaused = false;
        public DownloadThreadForMuch(DownloadInfo downloadInfo) {
            mDownloadInfo = downloadInfo;
            ObserverManager.getObserverManager().addObserver(this);
        }

        @Override
        public void run() {
            //表示该线程中下载的完成量
            long thisFinished = 0;
            int thisProgress = 0;
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
                Log.i(TAG, "start = " + mDownloadInfo.getStart() + ", finished = " + mDownloadInfo.getFinished() + ", end = " + mDownloadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                //用于发送广播，将下载的进度更新到UI中，当然也能用观察者模式
                Intent finishingIntent = new Intent(DownloadService.ACTION_UPDATE);
                Bundle finishingBundle = new Bundle();
                synchronized (DownloadTask.this){
                    mFinished += mDownloadInfo.getFinished();
                }
                thisFinished += mDownloadInfo.getFinished();
                //HTTP_PARTIAL HTTP/206 “Partial Content”响应是在客户端表明自己只需要目标URL上的部分资源的时候返回的.
                // 这种情况经常发生在客户端继续请求一个未完成的下载的时候
                if (connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
                    //读取数据
                    is = connection.getInputStream();
                    byte[] buffer = new byte[1024 * 8];
                    int len;
                    //每1s更新一次广播，不能太快了
                    long time = System.currentTimeMillis();
                    //总进度
                    int progress = 0;
                    while ((len = is.read(buffer)) != -1) {
                        //如果暂停了，更新mDownloadInfo到数据库
                        if (isPaused){
                            mDownloadInfoManagerImpl.updateDownloadInfo(mDownloadInfo.getUrl(), mDownloadInfo.getId(),
                                    thisFinished, thisProgress);
                            Log.i(TAG, "when pause in much thread: thisFinished = " + thisFinished + ",thisProgress = "
                                    + thisProgress + "--" + Thread.currentThread().getName());
                            checkAllThreadPaused();
                            return;
                        }
                        //写入文件
                        raf.write(buffer, 0, len);
                        //多线程操作mFinished，需要同步
                        synchronized (DownloadTask.this){
                            //把下载进度发送广播给Activity
                            mFinished += len;
                        }
                        thisFinished += len;
                        if (System.currentTimeMillis() - time > 1000) {//减少UI负载
                            time = System.currentTimeMillis();
                            progress = (int)(mFinished * 100 / mFileInfo.getLength());
                            thisProgress = (int)(thisFinished * 100 / (mDownloadInfo.getEnd() - mDownloadInfo.getStart()));
                            finishingBundle.putInt("finishing", progress);
                            Log.i(TAG, "much thread: progress = " + progress);
                            Log.i(TAG, "much thread: thisProgress = "+ thisProgress  + " in the thread " + Thread.currentThread().getName());
                            Log.i(TAG, "much thread: thisFinished = "+ thisFinished  + " in the thread " + Thread.currentThread().getName());
                            finishingBundle.putInt("id", mFileInfo.getId());
                            finishingIntent.putExtras(finishingBundle);
                            mContext.sendBroadcast(finishingIntent);
                        }
                    }
                    isFinished = true;
                    checkAllThreadFinished();
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

        @Override
        public void update(Observable observable, Object data) {
            if (data instanceof Boolean){
                isPaused = (boolean) data;
                Log.i(TAG, "update: " + this + ", isPaused = " + isPaused);
            }
        }
    }
    /**
     * 判断所有线程是否都执行完毕
     *,然后再发送下载完成广播
     */
    private synchronized void checkAllThreadFinished(){
        boolean allFinished = true;
        for (DownloadThreadForMuch e: mThreadForMuchList) {
            if (!e.isFinished){
                allFinished = false;
                break;
            }
        }
        if (allFinished){
            Intent finishedIntent = new Intent(DownloadService.ACTION_FINISHED);
            Bundle bundle = new Bundle();
            bundle.putInt("finished",100);
            bundle.putInt("id", mFileInfo.getId());
            Log.i(TAG, "checkAllThreadFinished: " + "All finished");
            finishedIntent.putExtras(bundle);
            mContext.sendBroadcast(finishedIntent);
            mFinished = 0;
            //删除线程信息
            mDownloadInfoManagerImpl.deleteDownloadInfoByUrl(mFileInfo.getUrl());
            mThreadForMuchList.clear();
            ObserverManager.getObserverManager().deleteObservers();
        }
    }

    /**
     * 判断所有线程是否都暂停了
     *,如果都暂停了，将mFinished置0
     */
    private synchronized void checkAllThreadPaused(){
        boolean allPaused = true;
        for (DownloadThreadForMuch e: mThreadForMuchList) {
            if (!e.isPaused){
                allPaused = false;
                break;
            }
        }
        if (allPaused){
            mFinished = 0;
            mThreadForMuchList.clear();
            ObserverManager.getObserverManager().deleteObservers();
            Log.i(TAG, "checkAllThreadPaused: All paused");
        }
    }

    /**
     * 针对单任务单线程的方式，这个简单
     */
    class DownloadThreadForOne extends Thread{
        private DownloadInfo mDownloadInfo;
        public DownloadThreadForOne(DownloadInfo downloadInfo) {
            mDownloadInfo = downloadInfo;
        }

        @Override
        public void run() {
            //表示该线程中下载的完成量
            long thisFinished = 0;
            int thisProgress = 0;
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
                Log.i(TAG, "start = " + mDownloadInfo.getStart() + ", finish = " + mDownloadInfo.getFinished() + ", end = " + mDownloadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                //用于发送广播，将下载的进度更新到UI中，当然也能用观察者模式
                Intent finishingIntent = new Intent(DownloadService.ACTION_UPDATE);
                Bundle finishingBundle = new Bundle();

                thisFinished += mDownloadInfo.getFinished();
                //HTTP_PARTIAL HTTP/206 “Partial Content”响应是在客户端表明自己只需要目标URL上的部分资源的时候返回的.
                // 这种情况经常发生在客户端继续请求一个未完成的下载的时候
                if (connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
                    //读取数据
                    is = connection.getInputStream();
                    byte[] buffer = new byte[1024 * 8];
                    int len;
                    //每1s更新一次广播，不能太快了
                    long time = System.currentTimeMillis();

                    while ((len = is.read(buffer)) != -1) {
                        //如果暂停了，更新mDownloadInfo到数据库
                        if (mIsPause){
                            mDownloadInfoManagerImpl.updateDownloadInfo(mDownloadInfo.getUrl(), mDownloadInfo.getId(),
                                    thisFinished, thisProgress);
                            Log.i(TAG, "when pause in one thread: thisFinished = " + thisFinished + ",thisProgress = " + thisProgress);
                            return;
                        }

                        //写入文件
                        raf.write(buffer, 0, len);
                        thisFinished += len;
                        if (System.currentTimeMillis() - time > 1000) {//减少UI负载
                            time = System.currentTimeMillis();
                            thisProgress = (int)(thisFinished * 100 / mFileInfo.getLength());
                            finishingBundle.putInt("finishing", thisProgress);
                            Log.i(TAG, "one thread: thisProgress = " + thisProgress);
                            finishingBundle.putInt("id", mFileInfo.getId());
                            finishingIntent.putExtras(finishingBundle);
                            mContext.sendBroadcast(finishingIntent);
                        }
                    }
                    Intent finishedIntent = new Intent(DownloadService.ACTION_FINISHED);
                    Bundle bundle = new Bundle();
                    bundle.putInt("finished",100);
                    bundle.putInt("id", mFileInfo.getId());
                    //                    finishedIntent.putExtra("finished",100); 这么写id会被覆盖，如果传送多个int类型值，用bundle
                    //                    finishingIntent.putExtra("id", mFileInfo.getId());
                    finishedIntent.putExtras(bundle);
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
