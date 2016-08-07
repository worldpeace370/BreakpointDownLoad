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
import com.lebron.breakpointdownload.bean.DownloadInfo;
import com.lebron.breakpointdownload.bean.FileInfo;
import com.lebron.breakpointdownload.service.DownloadService;
import com.lebron.breakpointdownload.utils.DownloadInfoManagerImpl;

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
        initLastState();
    }

    /**
     * 由于上次应用退出前可能有任务没有下载完全，所以再次打开应用的时候，需要接着上次的下载任务继续下载
     * 在每个任务对应的下载进度条也得显示上次应用退出时的下载进度
     */
    private void initLastState() {
        DownloadInfoManagerImpl impl = new DownloadInfoManagerImpl(this);
        for (FileInfo info :mFileList) {
            //读取数据库的线程信息,应用退出下次启动时照样能接着上次的下载,progress显示应用退出时的下载进度
            List<DownloadInfo> downloadInfoList = impl.getDownloadInfoList(info.getUrl());
            if (downloadInfoList.size() == 0) {

            }else {
                DownloadInfo downloadInfo = downloadInfoList.get(0);
                long fileLength = downloadInfo.getEnd();
                mAdpter.updateProgressBar(downloadInfo.getId(), downloadInfo.getProgress());
                //根据从数据库中查询来的下载信息，重新设置info的两个属性
                //当点击开始的时候，由于这两个属性已经有值了，就会省去重新计算文件长度的操作，加快速度
                info.setGettedLength(true);
                info.setLength(fileLength);
            }
        }
    }

    private void initData() {
        mFileList= new ArrayList<>();
        FileInfo fileInfo0 = new FileInfo(0, "http://dldir1.qq.com/weixin/android/weixin6316android780.apk",
                "weixin.apk", 0, 0, false);
        FileInfo fileInfo1 = new FileInfo(1, "http://111.202.99.12/sqdd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk",
                "qq.apk", 0, 0, false);
        FileInfo fileInfo2 = new FileInfo(2, "http://d3g.qq.com/sngapp/app/update/20160722151212_4807/qzone_6.7.1.288_android_r138011_20160722004617_release_QZGW_D.apk",
                "QQZone.apk", 0, 0, false);
        FileInfo fileInfo3 = new FileInfo(3, "http://10.200.88.1/files/A008000002CB8FEE/codown.youdao.com/note/youdaonote_android_5.3.6_youdaoweb.apk",
                "youdao.apk", 0, 0, false);
        FileInfo fileInfo4 = new FileInfo(5, "http://10.200.88.3/files/7006000002D27EAF/s1.music.126.net/download/android/CloudMusic_official_3.6.0.143673.apk",
                "wangyiMusic.mp4", 0, 0, false);
        FileInfo fileInfo5 = new FileInfo(5, "http://file.ws.126.net/opencourse/netease_open_androidphone.apk",
                "wangyigongkaike.apk", 0, 0, false);
        FileInfo fileInfo6 = new FileInfo(6, "http://10.200.88.1/drivefiles/9084000002D22E02/issuecdn.baidupcs.com/issue/netdisk/apk/BaiduYun_7.13.3.apk",
                "baiduguanjia.apk", 0, 0, false);
        FileInfo fileInfo7 = new FileInfo(7, "http://10.200.88.2/files/9160000002B00ACF/sqdd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk",
                "QQ.apk", 0, 0, false);
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
        filter.addAction(DownloadService.ACTION_UPDATE_FILEINFO_STATE);
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
                int progress =  bundle.getInt("finishing");
                int id = bundle.getInt("id");
                mAdpter.updateProgressBar(id, progress);
            }else if (DownloadService.ACTION_FINISHED.equals(action)) {
                Bundle bundle = intent.getExtras();
                int progress = bundle.getInt("finished");
                int id = bundle.getInt("id");
                mAdpter.updateProgressBar(id, progress);
                Log.i(TAG, "ACTION_FINISHED: progress = " + progress + ", id = " + id);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stopService(new Intent(MultiDownLoad.this, DownloadService.class));
    }
}
