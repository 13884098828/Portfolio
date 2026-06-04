package com.example.campusrun;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "CampusRun.db";
    private static final int DB_VERSION = 5;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE user (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE," +
                "password TEXT," +
                "balance REAL DEFAULT 100.0," +
                "nickname TEXT," +
                "avatarIndex INTEGER DEFAULT 0," +
                "avatarUri TEXT)");

        db.execSQL("CREATE TABLE task (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "content TEXT," +
                "price REAL," +
                "route TEXT," +
                "pickupAddress TEXT," +
                "deliveryAddress TEXT," +
                "phone TEXT," +
                "remark TEXT," +
                "publisher TEXT," +
                "acceptUser TEXT," +
                "state TEXT," +
                "createTime TEXT," +
                "acceptTime TEXT," +
                "finishTime TEXT," +
                "settleTime TEXT," +
                "frozenAmount REAL DEFAULT 0)");

        db.execSQL("CREATE TABLE wallet_record (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT," +
                "amount REAL," +
                "type TEXT," +
                "taskId INTEGER," +
                "description TEXT," +
                "createTime TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS wallet_record");
        db.execSQL("DROP TABLE IF EXISTS user");
        db.execSQL("DROP TABLE IF EXISTS task");
        onCreate(db);
    }
}
