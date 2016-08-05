package com.lebron.breakpointdownload.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.lebron.breakpointdownload.bean.FileInfo;
import com.lebron.breakpointdownload.utils.DownloadTask;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**单线程断点下载启动的服务，接受SingleDownload Activity传来的Intent信息进行下载或者暂停下载
 * Created by wuxiangkun on 2016/8/4 14:24.
 * Contacts wuxiangkun@live.com
 */
public class DownloadService extends Service{
    private static final String TAG = "DownloadService";
    private static final int MSG_INIT = 0;
    //开始下载
    public static final String ACTION_START = "ACTION_START";
    //暂停下载
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    //结束下载
    public static final String ACTION_FINISHED = "ACTION_FINISHED";
    //更新UI
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    //更新FileInfo对象的是否得到文件长度状态
    public static final String ACTION_UPDATE_FILEINFO_STATE = "ACTION_UPDATE_FILEINFO_STATE";
    //下载路径
    public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/downloads/";
    private MyHandler mMyHandler;
    private Map<String, DownloadTask> mDownloadTaskMap;
    //防止内存泄漏的Handler模板
    private static class MyHandler extends Handler{
        WeakReference<DownloadService> weakReference;
        public MyHandler(DownloadService context){
            weakReference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DownloadService context = weakReference.get();
            if (context != null){//如果activity仍然在弱引用中,执行...
                switch (msg.what){
                    case MSG_INIT:
                        FileInfo fileInfo = (FileInfo) msg.obj;
                        //启动下载任务
                        context.startDownload(fileInfo);
                }
            }else { //否则不执行
                Log.i(TAG, "handleMessage: 上下文为null");
            }
        }
    }

    private void startDownload(FileInfo fileInfo){
        DownloadTask downloadTask = mDownloadTaskMap.get(fileInfo.getUrl());
        if (downloadTask == null){
            downloadTask = new DownloadTask(DownloadService.this, fileInfo);
            mDownloadTaskMap.put(fileInfo.getUrl(), downloadTask);
        }
        downloadTask.setPause(false);
        downloadTask.downloadInOneThread();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDownloadTaskMap = new HashMap<>();
        mMyHandler = new MyHandler(DownloadService.this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获得SingleDownload 启动服务时传过来的Intent信息
        if (ACTION_START.equals(intent.getAction())){
            //虽然每次都会getParcelableExtra，但实际上得到的对象hashCode()不一样。对象内容是相同的，但是引用不一样
            FileInfo fileInfo = intent.getParcelableExtra("mFileInfo");
            //获取待下载的文件长度，如果已经获取就不再第二次获取。完事之后通知开始下载文件
            new GetFileLengthThread(fileInfo).start();
        }else if(ACTION_PAUSE.equals(intent.getAction())){
            FileInfo fileInfo = intent.getParcelableExtra("mFileInfo");
            DownloadTask downloadTask = mDownloadTaskMap.get(fileInfo.getUrl());
            if (downloadTask != null){
                downloadTask.setPause(true);//实际上是退出了线程，当重新开始下载时又开启了新线程
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**获取文件长度耗时，所以需要优化
     * 根据传入的文件信息(此时可能只有url)，得到文件长度的线程
     * 将文件长度再存入FileInfo中。得到文件长度之后，通知文件下载存储线程开始执行
     */
    class GetFileLengthThread extends Thread{
        private FileInfo mFileInfo;

        public GetFileLengthThread(FileInfo fileInfo) {
            mFileInfo = fileInfo;
        }

        @Override
        public void run() {
            int length = -1;
            //如果时第一次获取文件长度就获取，否则不在进行获取文件长度操作
            if (!mFileInfo.isGettedLength()){
                HttpURLConnection connection = null;
                RandomAccessFile accessFile = null;
                //连接网络文件
                try {
                    URL url = new URL(mFileInfo.getUrl());
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(3000);
                    connection.setRequestMethod("GET");

                    Log.i("getResponseCode==", connection.getResponseCode() + "");
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        //获取文件长度
                        length = connection.getContentLength();
                        Log.i("length==", length + "");
                    }
                    if (length < 0) {
                        return;
                    }
                    //创建目录
                    File dir = new File(DOWNLOAD_PATH);
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    //在本地创建随机读写文件，供给DownloadTask进行文件写操作
                    File file = new File(dir, mFileInfo.getFileName());
                    accessFile = new RandomAccessFile(file, "rwd");
                    //设置本地文件长度
                    accessFile.setLength(length);
                    //设置mFileInfo文件长度
                    mFileInfo.setLength(length);

                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (connection != null && accessFile != null){
                        try {
                            accessFile.close();
                            connection.disconnect();
                            //mFileInfo已经获得过文件长度,发送广播，通知传入的mFileInfo更改属性
                            Intent intent = new Intent();
                            intent.setAction(ACTION_UPDATE_FILEINFO_STATE);
                            Bundle bundle = new Bundle();
                            bundle.putInt("id", mFileInfo.getId());
                            bundle.putBoolean("isGettedLength", true);
                            bundle.putLong("length", length);
                            intent.putExtras(bundle);
                            DownloadService.this.sendBroadcast(intent);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            //文件长度获取完毕，handler发送通知开始下载文件
            Message message = Message.obtain();
            message.what = MSG_INIT;
            message.obj = mFileInfo;
            mMyHandler.sendMessage(message);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**当Activity按下返回键退出时，执行onDestroy(),进而执行DownloadService的onDestroy()方法
     * ，可是由服务开启的DownloadTask任务中含有子线程下载程序可能正在运行，所以把子线程也得退出，
     * 退出时会保存下载进度在数据库中，下次应用启动时能接着之前的进度继续下载
     * 从mDownloadTaskMap取出所有的下载任务，然后全部遍历设置暂停
     */
    private void pauseAllThread(){
        Iterator iterator = mDownloadTaskMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry entry = (Map.Entry) iterator.next();
            DownloadTask downloadTask = (DownloadTask) entry.getValue();
            downloadTask.setPause(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: 执行了！！");
        mMyHandler.removeMessages(MSG_INIT);
        mMyHandler = null;
        pauseAllThread();
        mDownloadTaskMap = null;
        stopSelf();
    }
}
