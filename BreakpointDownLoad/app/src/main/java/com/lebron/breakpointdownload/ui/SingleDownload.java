package com.lebron.breakpointdownload.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lebron.breakpointdownload.R;
import com.lebron.breakpointdownload.bean.DownloadInfo;
import com.lebron.breakpointdownload.bean.FileInfo;
import com.lebron.breakpointdownload.service.DownloadService;
import com.lebron.breakpointdownload.utils.DownloadInfoManagerImpl;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * 单任务单线程下载，支持断点下载。
 * 即对于一个文件下载来讲，只是开启一个线程去下载。
 * 高级用法可以针对一个任务(下载一个文件)开启多线程，将文件拆成几份，每份一个线程
 * 下载后再合并。这个在MultiDownLoad中有展现
 */
public class SingleDownload extends AppCompatActivity {
    private static final String TAG = "SingleDownload";
    @InjectView(R.id.name)
    TextView mFileName;
    @InjectView(R.id.progressBar)
    ProgressBar mProgressBar;
    @InjectView(R.id.pro_text)
    TextView mProText;
    @InjectView(R.id.start)
    Button mButtonStart;
    @InjectView(R.id.pause)
    Button mButtonPause;
    @InjectView(R.id.path)
    TextView mPath;
    //待下载的文件的文件相关信息,java bean类
    FileInfo mFileInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_download);
        ButterKnife.inject(this);
        initProgressBarAndFileInfo();
        registerMyReciver();
    }
    //注册广播接收器
    private void registerMyReciver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_START);
        filter.addAction(DownloadService.ACTION_UPDATE);
        filter.addAction(DownloadService.ACTION_FINISHED);
        filter.addAction(DownloadService.ACTION_UPDATE_FILEINFO_STATE);
        registerReceiver(mReceiver, filter);
    }


    /**
     * 初始化ProgressBar，如果之前应用正在下载，然后退出了
     * 下次应用打开的时候，ProgressBar接着之间的进度进行显示
     * 同时根据数据库信息创建FileInfo对象
     */
    private void initProgressBarAndFileInfo(){
        long fileLength = 0;
        boolean isGettedLength = false;
        //创建文件信息对象
        String urlString = "http://dldir1.qq.com/weixin/android/weixin6316android780.apk";
        mFileInfo = new FileInfo(0, urlString, "WeChat.apk", 0, 0, false);

        DownloadInfoManagerImpl impl = new DownloadInfoManagerImpl(this);
        //读取数据库的线程信息,应用退出下次启动时照样能接着上次的下载,progress显示应用退出时的下载进度
        List<DownloadInfo> downloadInfoList = impl.getDownloadInfoList(mFileInfo.getUrl());

        //设置进度条信息
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setMax(100);
        if (downloadInfoList.size() == 0) {
            mProgressBar.setProgress(0);
        }else {
            DownloadInfo downloadInfo = downloadInfoList.get(0);
            mProgressBar.setProgress(downloadInfo.getProgress());
            mProText.setText(new StringBuffer().append(downloadInfo.getProgress()).append("%"));
            fileLength = downloadInfo.getEnd();
            isGettedLength = true;
        }
        //下载百分比可见
        mProText.setVisibility(View.VISIBLE);
        mFileName.setText(mFileInfo.getFileName());

        mFileInfo.setLength(fileLength);
        mFileInfo.setGettedLength(isGettedLength);
    }

    @OnClick({R.id.start, R.id.pause})
    public void onClick(View view){
        Intent intent = new Intent(SingleDownload.this, DownloadService.class);
        switch (view.getId()){
            case R.id.start:
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra("mFileInfo", mFileInfo);
                mButtonStart.setEnabled(false);
                mButtonPause.setEnabled(true);
                break;
            case R.id.pause:
                intent.setAction(DownloadService.ACTION_PAUSE);
                intent.putExtra("mFileInfo", mFileInfo);
                mButtonStart.setEnabled(true);
                mButtonPause.setEnabled(false);
                break;
        }
        startService(intent);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadService.ACTION_UPDATE.equals(action)){
                Bundle bundle = intent.getExtras();
                int progress = bundle.getInt("finishing");
                mProgressBar.setProgress(progress);
                mProText.setText(new StringBuilder().append(progress).append("%"));
                Log.i(TAG, "onReceive: 还在执行！");
            }else if (DownloadService.ACTION_UPDATE_FILEINFO_STATE.equals(action)){
                //该mFileInfo的长度已经被得到了，下次点击开始的时候传入的mFileInfo的isGettedLength就是true了
                Bundle bundle = intent.getExtras();
                long length = bundle.getLong("length");
                boolean isGettedLength = bundle.getBoolean("isGettedLength");
                mFileInfo.setLength(length);
                mFileInfo.setGettedLength(isGettedLength);
            }else if (DownloadService.ACTION_FINISHED.equals(action)){
                int progress = intent.getIntExtra("finished", 0);
                mProgressBar.setProgress(progress);
                mProText.setText(new StringBuilder().append(progress).append("%"));
                mButtonPause.setEnabled(false);
                Toast.makeText(SingleDownload.this, "下载成功！", Toast.LENGTH_SHORT).show();
                mPath.setVisibility(View.VISIBLE);
                mPath.setText(new StringBuilder().append("下载路径在：").append(DownloadService.DOWNLOAD_PATH).append(mFileInfo.getFileName()));
            }
        }
    };

    //停止服务，取消注册广播
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: 执行了！");
        stopService(new Intent(SingleDownload.this, DownloadService.class));
        unregisterReceiver(mReceiver);
    }
}
