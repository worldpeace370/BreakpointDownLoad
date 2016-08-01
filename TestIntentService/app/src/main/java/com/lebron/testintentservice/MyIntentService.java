package com.lebron.testintentservice;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.lebron.testintentservice.action.FOO";
    private static final String ACTION_BAZ = "com.lebron.testintentservice.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "url";
    private static final String EXTRA_PARAM2 = "com.lebron.testintentservice.extra.PARAM2";
    private static final String TAG = "MyIntentService";
    private String fileName = "尿失禁.mp4";
    private String filePath = "";
    private final static int NOTIFICATION_ID = 0;
    public MyIntentService() {
        super("MyIntentService");
        filePath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + File.separator + fileName;
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent: -->线程id = " + Thread.currentThread().getId());
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        Log.i(TAG, "handleActionFoo: fielpath = " + filePath );
        //本例中只有param1有值
        NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("正在下载...");
        builder.setContentText("已下载0%");
        builder.setTicker("开始下载！").setContentInfo("啦啦");
        builder.setAutoCancel(false);
        builder.setOngoing(false);//可滑动取消掉通知栏
        // 提示最初状态的通知
        manager.notify(NOTIFICATION_ID, builder.build());
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            File file = new File(filePath);
            if (!file.exists()){
                file.createNewFile();
            }
            bos = new BufferedOutputStream(new FileOutputStream(file));
            URL url = new URL(param1);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(false);
            httpConn.connect();
            if (httpConn.getResponseCode() == 200) {
                // 获取下载文件的总长度
                int totalLength = httpConn.getContentLength();
                bis = new BufferedInputStream(httpConn.getInputStream());
                byte buffer[] = new byte[1024*8];
                int len;
                int currentLength = 0;
                while ((len = bis.read(buffer)) != -1){
                    // 计算已经下载的字节总数
                    currentLength += len;
                    bos.write(buffer, 0, len);
                    bos.flush();
                    // 计算当前下载的进度
                    int progress = (int) ((currentLength / (float) totalLength) * 100);
                    // 参数1：最大的进度100
                    // 参数2：当前进度
                    // 参数3：是否显示模糊进度条。如果要显示精确的下载进度，则设置为false
                    builder.setProgress(100, progress, false);
                    builder.setContentText("已下载 " + progress + " %");
                    // 提示下载进度的通知
                    manager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // 设置延迟意图PendingIntent，当点击进度条后，进入视频播放
        Intent myIntent = new Intent();
        myIntent.setAction(Intent.ACTION_VIEW);
        myIntent.setDataAndType(Uri.fromFile(new File(filePath)), "video/*");
        // text/xml,text/html,text/javascript,text/css
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, myIntent,
                PendingIntent.FLAG_ONE_SHOT);
        builder.setContentIntent(pIntent);
        // 提示已经下载完毕的通知
        builder.setContentTitle("下载完毕！点击播放");
        builder.setContentText("已经下载100%");
        manager.notify(NOTIFICATION_ID, builder.build());

        // 发送自定义广播，给予Activity已经下载完毕，可以点击观看视频的提示
        Intent intent_broad = new Intent();
        intent_broad.setAction("com.steven.intentserviceloadvideo.myloadservice");
        Bundle bundle = new Bundle();
        bundle.putString("info", "视频下载完毕，是否播放？");
        bundle.putString("filePath", filePath);
        intent_broad.putExtras(bundle);
        sendBroadcast(intent_broad);
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**下载完成后会自动销毁服务
     * 在所有的请求(Intent)都被执行完以后会自动停止服务，所以，你不需要自己去调用stopSelf()方法来停止该服务
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: 服务被销毁了");
    }
}
