package com.lebron.breakpointdownload.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**用来得到数据库操作helper类，包含了创建数据库，数据库表，更新数据库版本等操作
 * 根据MyDBHelper的实例对象，SQLiteDatabase db = myDBHelper.getWritableDatabase();
 * 可以得到数据库实体对象，然后进行一些操作
 * Created by wuxiangkun on 2016/8/4 13:36.
 * Contacts wuxiangkun@live.com
 */
public class MyDBHelper extends SQLiteOpenHelper{
    private static final String DB_NAME = "download.db";
    private static final String SQL_CREATE = "create table thread_info(_id integer primary key autoincrement," +
            "thread_id integer,url text,start long,end long,finished long,progress integer)";
    private static final String SQL_DROP = "drop table if exists thread_info";
    private static final int VERSION = 1;
    private static final String TAG = "MyDBHelper";

    /**
     * 调用父类构造方法创建数据库，如果已经创建过了就不在创建
     * 针对当前应用创建有且仅有的一个数据库
     * @param context 上下文
     */
    public MyDBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        Log.i(TAG, "MyDBHelper: 创建数据库 download.db");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
        Log.i(TAG, "onCreate: 创建数据库表 thread_info");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP);
        db.execSQL(SQL_CREATE);
    }
}
