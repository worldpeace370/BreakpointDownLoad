package com.lebron.breakpointdownload.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lebron.breakpointdownload.bean.DownloadInfo;
import com.lebron.breakpointdownload.db.MyDBHelper;
import com.lebron.breakpointdownload.interfaces.DownloadInfoMannager;

import java.util.ArrayList;
import java.util.List;

/** 实现了DownloadInfoMannager中的所有方法，
 * 对DownloadInfo 进行数据库CURD的操作。
 * Created by wuxiangkun on 2016/8/4 20:16.
 * Contacts wuxiangkun@live.com
 */
public class DownloadInfoManagerImpl implements DownloadInfoMannager{
    private static final String TAG = "DownloadInfoManagerImpl";
    private MyDBHelper myDBHelper;
    private static final String SQL_INSERT = "insert into download_info(id,url,start,end,finished,progress) values(?,?,?,?,?,?)";
    private static final String SQL_DELETE = "delete from download_info where url = ? and id = ?";
    private static final String SQL_UPDATE = "update download_info set finished = ?, progress = ? where url = ? and id = ?";

    public DownloadInfoManagerImpl(Context context) {
        this.myDBHelper = new MyDBHelper(context);
    }

    @Override
    public void insertDownloadInfo(DownloadInfo downloadInfo) {
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        db.execSQL(SQL_INSERT, new Object[]{downloadInfo.getId(), downloadInfo.getUrl(), downloadInfo.getStart(),
        downloadInfo.getEnd(), downloadInfo.getFinished(), downloadInfo.getProgress()});
        db.close();
    }

    @Override
    public void deleteDownloadInfo(String url, int id) {
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        db.execSQL(SQL_DELETE, new Object[]{url, id});
        db.close();
    }

    @Override
    public void updateDownloadInfo(String url, int id, long finished, int progress) {
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        db.execSQL(SQL_UPDATE, new Object[]{finished, progress, url, id});
        db.close();
    }

    @Override
    public List<DownloadInfo> getDownloadInfoList(String url) {
        List<DownloadInfo> list = new ArrayList<>();
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from download_info where url = ?", new String[]{url});
        while (cursor.moveToNext()){
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.setId(cursor.getInt(cursor.getColumnIndex("id")));
            downloadInfo.setUrl(cursor.getString(cursor.getColumnIndex("url")));
            downloadInfo.setStart(cursor.getLong(cursor.getColumnIndex("start")));
            downloadInfo.setEnd(cursor.getLong(cursor.getColumnIndex("end")));
            downloadInfo.setFinished(cursor.getLong(cursor.getColumnIndex("finished")));
            downloadInfo.setProgress(cursor.getInt(cursor.getColumnIndex("progress")));
            list.add(downloadInfo);
        }
        cursor.close();
        db.close();
        return list;
    }

    @Override
    public boolean isExists(String url, int id) {
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from download_info where url = ? and id = ?", new String[]{url, String.valueOf(id)});
        boolean isExist = cursor.moveToNext();
        cursor.close();
        db.close();
        return isExist;
    }
}
