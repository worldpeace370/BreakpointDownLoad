package com.lebron.breakpointdownload.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import com.lebron.breakpointdownload.R;
import com.lebron.breakpointdownload.adapter.DownloadListAdpter;
import com.lebron.breakpointdownload.bean.FileInfo;
import com.lebron.breakpointdownload.service.DownloadService;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MultiDownLoad extends AppCompatActivity {
    private static final String TAG = "MultiDownLoad";
    @InjectView(R.id.listview)
    ListView mListView;
    private List<FileInfo> mFileList;
    private DownloadListAdpter mAdpter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_down_load);

        ButterKnife.inject(this);
        initData();
        initRegister();
        initListView();
    }

    private void initData() {
        mFileList = new ArrayList<>();
        FileInfo fileInfo0 = new FileInfo(0, "http://dldir1.qq.com/weixin/android/weixin6316android780.apk",
                "weixin.apk", 0, 0, false);
        FileInfo fileInfo1 = new FileInfo(1, "http://111.202.99.12/sqdd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk",
                "qq.apk", 0, 0, false);
        FileInfo fileInfo2 = new FileInfo(2, "http://www.imooc.com/mobile/imooc.apk",
                "imooc.apk", 0, 0, false);
        FileInfo fileInfo3 = new FileInfo(3, "http://www.imooc.com/download/Activator.exe",
                "Activator.exe", 0, 0, false);
        FileInfo fileInfo4 = new FileInfo(0, "http://114.215.117.169/fileupload/abc.mp4",
                "abc.mp4", 0, 0, false);
        FileInfo fileInfo5 = new FileInfo(1, "http://114.215.117.169/fileupload/bcd.mp4",
                "bcd.mp4", 0, 0, false);
        FileInfo fileInfo6 = new FileInfo(2, "http://114.215.117.169/fileupload/cde.mp4",
                "cde.mp4", 0, 0, false);
        FileInfo fileInfo7 = new FileInfo(3, "http://114.215.117.169/fileupload/def.mp4",
                "def.mp4", 0, 0, false);
        mFileList.add(fileInfo0);
        mFileList.add(fileInfo1);
        mFileList.add(fileInfo2);
        mFileList.add(fileInfo3);
        mFileList.add(fileInfo4);
        mFileList.add(fileInfo5);
        mFileList.add(fileInfo6);
        mFileList.add(fileInfo7);
    }

    private void initRegister() {
        //注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        filter.addAction(DownloadService.ACTION_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private void initListView(){
        mAdpter = new DownloadListAdpter(mFileList, this);
        mListView.setAdapter(mAdpter);
    }

    /**
     * 更新UI的广播接收器
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadService.ACTION_UPDATE.equals(action)){
                Bundle bundle = intent.getExtras();
                int progress = bundle.getInt("finishing");
                int id = bundle.getInt("id");
                mAdpter.updateProgressBar(id, progress);
                Log.i(TAG, "onReceive: 还在执行！");
            }else if (DownloadService.ACTION_FINISHED.equals(action)) {
                int progress = intent.getIntExtra("finished", 0);
                int id = intent.getIntExtra("id", 0);
            }else if (DownloadService.ACTION_UPDATE_FILEINFO_STATE.equals(action)){
                //该fileInfo的长度已经被得到了，下次点击开始的时候传入的mFileInfo的isGettedLength就是true了
                Bundle bundle = intent.getExtras();
                long length = bundle.getLong("length");
                boolean isGettedLength = bundle.getBoolean("isGettedLength");
                int id = bundle.getInt("id");
                FileInfo fileInfo = mFileList.get(id);
                fileInfo.setLength(length);
                fileInfo.setGettedLength(isGettedLength);
            }
        }
    };
}
