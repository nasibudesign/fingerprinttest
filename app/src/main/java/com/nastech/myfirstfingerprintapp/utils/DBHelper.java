package com.nastech.myfirstfingerprintapp.utils;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";
    private static final String DB_NAME = "fp.db";//数据库名字
    public static String TABLE_NAME = "fp_table";// 表名
    public static String FID = "fid";// 列名
    public static String  FDATA= "data";// 列名
    private static final int DB_VERSION = 1;   // 数据库版本

    public DBHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    //创建数据库
    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建表
        String sql="CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + FID + " INTEGER PRIMARY KEY AUTOINCREMENT , "+ FDATA + " TEXT not null );";
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            Log.e(TAG, "onCreate " + TABLE_NAME + "Error" + e.toString());
            return;
        }
    }

    /*
    数据库升级
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
