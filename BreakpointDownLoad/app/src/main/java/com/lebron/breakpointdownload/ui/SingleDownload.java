package com.lebron.breakpointdownload.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lebron.breakpointdownload.R;
import com.lebron.breakpointdownload.bean.FileInfo;
import com.lebron.breakpointdownload.service.DownloadService;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class SingleDownload extends AppCompatActivity {
    @InjectView(R.id.name)
    TextView mFileName;
    @InjectView(R.id.progressBar)
    ProgressBar mProgressBar;
    @InjectView(R.id.pro_text)
    TextView mProText;
    //待下载文件url地址
    private final String url = "http://dldir1.qq.com/weixin/android/weixin6316android780.apk";
    //待下载的文件的文件相关信息,java bean类
    FileInfo mFileInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_download);
        ButterKnife.inject(this);
        initView();
    }

    private void initView() {
        //下载百分比可见
        mProText.setVisibility(View.VISIBLE);
        //设置进度条信息
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setMax(100);
        //创建文件信息对象
        mFileInfo = new FileInfo(0, url, "WeChat", 0, 0);
        mFileName.setText(mFileInfo.getFileName());
    }

    @OnClick({R.id.start, R.id.pause})
    public void onClick(View view){
        Intent intent = new Intent(SingleDownload.this, DownloadService.class);
        switch (view.getId()){
            case R.id.start:

                break;
            case R.id.pause:

                break;
        }
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(SingleDownload.this, DownloadService.class));
    }
}
