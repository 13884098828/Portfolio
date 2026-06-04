package com.example.campusrun;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import bean.Task;
import bean.WalletRecord;

public class DBManager {

    public static final String STATE_WAITING = "\u5f85\u63a5\u5355";
    public static final String STATE_ACCEPTED = "\u5df2\u63a5\u5355";
    public static final String STATE_FINISHED = "\u5df2\u5b8c\u6210";
    public static final String STATE_SETTLED = "\u5df2\u7ed3\u7b97";

    public static final String TYPE_RECHARGE = "RECHARGE";
    public static final String TYPE_FREEZE = "FREEZE";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_INCOME = "INCOME";

    private static final double DEFAULT_BALANCE = 100.0;
    private final DBHelper dbHelper;

    public DBManager(Context context) {
        dbHelper = new DBHelper(context);
    }

    public boolean register(String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM user WHERE username=?", new String[]{username});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        if (exists) {
            db.close();
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        values.put("balance", DEFAULT_BALANCE);
        values.put("nickname", username);
        values.put("avatarIndex", 0);
        values.put("avatarUri", "");
        long result = db.insert("user", null, values);
        if (result != -1) {
            insertWalletRecord(db, username, DEFAULT_BALANCE, TYPE_RECHARGE, 0,
                    "\u6f14\u793a\u521d\u59cb\u4f59\u989d");
        }
        db.close();
        return result != -1;
    }

    public boolean login(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM user WHERE username=? AND password=?",
                new String[]{username, password});
        boolean success = cursor.moveToFirst();
        cursor.close();
        db.close();
        return success;
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM user WHERE username=? AND password=?",
                new String[]{username, oldPassword});
        boolean matched = cursor.moveToFirst();
        cursor.close();
        if (!matched) {
            db.close();
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        int rows = db.update("user", values, "username=?",
                new String[]{username});
        db.close();
        return rows > 0;
    }

    public String getNickname(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT nickname FROM user WHERE username=?",
                new String[]{username});
        String nickname = username;
        if (cursor.moveToFirst()) {
            String value = cursor.getString(0);
            if (value != null && !value.isEmpty()) {
                nickname = value;
            }
        }
        cursor.close();
        db.close();
        return nickname;
    }

    public int getAvatarIndex(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT avatarIndex FROM user WHERE username=?",
                new String[]{username});
        int avatarIndex = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return avatarIndex;
    }

    public String getAvatarUri(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT avatarUri FROM user WHERE username=?",
                new String[]{username});
        String avatarUri = "";
        if (cursor.moveToFirst()) {
            String value = cursor.getString(0);
            if (value != null) {
                avatarUri = value;
            }
        }
        cursor.close();
        db.close();
        return avatarUri;
    }

    public boolean updateProfile(String username, String nickname, int avatarIndex) {
        return updateProfile(username, nickname, avatarIndex, "");
    }

    public boolean updateProfile(String username, String nickname, int avatarIndex, String avatarUri) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nickname", nickname);
        values.put("avatarIndex", avatarIndex);
        values.put("avatarUri", avatarUri == null ? "" : avatarUri);
        int rows = db.update("user", values, "username=?",
                new String[]{username});
        db.close();
        return rows > 0;
    }

    public double getUserBalance(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT balance FROM user WHERE username=?",
                new String[]{username});
        double balance = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        db.close();
        return balance;
    }

    public double getFrozenAmount(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(frozenAmount) FROM task WHERE publisher=? AND state IN (?,?,?)",
                new String[]{username, STATE_WAITING, STATE_ACCEPTED, STATE_FINISHED});
        double amount = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        db.close();
        return amount;
    }

    public boolean recharge(String username, double amount) {
        if (amount <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            addBalance(db, username, amount);
            insertWalletRecord(db, username, amount, TYPE_RECHARGE, 0,
                    "\u5145\u503c");
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public boolean publishTask(Task task) {
        if (task.getPrice() <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            double balance = getUserBalance(db, task.getPublisher());
            if (balance < task.getPrice()) {
                return false;
            }

            addBalance(db, task.getPublisher(), -task.getPrice());

            ContentValues values = new ContentValues();
            values.put("title", task.getTitle());
            values.put("content", task.getContent());
            values.put("price", task.getPrice());
            values.put("route", task.getRoute());
            values.put("pickupAddress", task.getPickupAddress());
            values.put("deliveryAddress", task.getDeliveryAddress());
            values.put("phone", task.getPhone());
            values.put("remark", task.getRemark());
            values.put("publisher", task.getPublisher());
            values.put("acceptUser", "");
            values.put("state", STATE_WAITING);
            values.put("createTime", now());
            values.put("acceptTime", "");
            values.put("finishTime", "");
            values.put("settleTime", "");
            values.put("frozenAmount", task.getPrice());
            long taskId = db.insert("task", null, values);
            if (taskId == -1) {
                return false;
            }

            insertWalletRecord(db, task.getPublisher(), -task.getPrice(), TYPE_FREEZE,
                    (int) taskId, "\u53d1\u5e03\u8ba2\u5355\u51bb\u7ed3\u4f63\u91d1");
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public boolean insertTask(Task task) {
        return publishTask(task);
    }

    public List<Task> getWaitingTasks(String currentUser) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM task WHERE state=? AND publisher<>? ORDER BY id DESC",
                new String[]{STATE_WAITING, currentUser == null ? "" : currentUser});
        while (cursor.moveToNext()) {
            taskList.add(cursorToTask(cursor));
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public List<Task> getAllTask() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM task ORDER BY id DESC", null);
        while (cursor.moveToNext()) {
            taskList.add(cursorToTask(cursor));
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public Task getTaskById(int taskId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM task WHERE id=?",
                new String[]{String.valueOf(taskId)});
        Task task = cursor.moveToFirst() ? cursorToTask(cursor) : null;
        cursor.close();
        db.close();
        return task;
    }

    public List<Task> getTasksByPublisher(String username) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM task WHERE publisher=? ORDER BY id DESC",
                new String[]{username});
        while (cursor.moveToNext()) {
            taskList.add(cursorToTask(cursor));
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public List<Task> getTasksByAcceptUser(String username) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM task WHERE acceptUser=? ORDER BY id DESC",
                new String[]{username});
        while (cursor.moveToNext()) {
            taskList.add(cursorToTask(cursor));
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public List<Task> getMyPublish(String username) {
        return getTasksByPublisher(username);
    }

    public List<Task> getMyAccept(String username) {
        return getTasksByAcceptUser(username);
    }

    public boolean acceptTask(int taskId, String acceptUser) {
        Task task = getTaskById(taskId);
        if (task == null || acceptUser == null || acceptUser.equals(task.getPublisher())) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("state", STATE_ACCEPTED);
        values.put("acceptUser", acceptUser);
        values.put("acceptTime", now());
        int rows = db.update("task", values,
                "id=? AND state=?",
                new String[]{String.valueOf(taskId), STATE_WAITING});
        db.close();
        return rows > 0;
    }

    public boolean markDelivered(int taskId, String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("state", STATE_FINISHED);
        values.put("finishTime", now());
        int rows = db.update("task", values,
                "id=? AND state=? AND acceptUser=?",
                new String[]{String.valueOf(taskId), STATE_ACCEPTED, username});
        db.close();
        return rows > 0;
    }

    public boolean confirmFinish(int taskId, String publisher) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Task task = getTaskById(db, taskId);
            if (task == null || !publisher.equals(task.getPublisher())
                    || !STATE_FINISHED.equals(task.getState())
                    || task.getAcceptUser() == null || task.getAcceptUser().isEmpty()) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("state", STATE_SETTLED);
            values.put("settleTime", now());
            values.put("frozenAmount", 0);
            int rows = db.update("task", values, "id=?",
                    new String[]{String.valueOf(taskId)});
            if (rows <= 0) return false;

            addBalance(db, task.getAcceptUser(), task.getFrozenAmount());
            insertWalletRecord(db, task.getAcceptUser(), task.getFrozenAmount(), TYPE_INCOME,
                    taskId, "\u8ba2\u5355\u7ed3\u7b97\u6536\u5165");
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public boolean deleteWaitingTask(int taskId, String publisher) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Task task = getTaskById(db, taskId);
            if (task == null || !publisher.equals(task.getPublisher())
                    || !STATE_WAITING.equals(task.getState())) {
                return false;
            }

            int rows = db.delete("task", "id=?",
                    new String[]{String.valueOf(taskId)});
            if (rows <= 0) return false;

            addBalance(db, publisher, task.getFrozenAmount());
            insertWalletRecord(db, publisher, task.getFrozenAmount(), TYPE_REFUND,
                    taskId, "\u5220\u9664\u8ba2\u5355\u9000\u56de\u51bb\u7ed3\u4f63\u91d1");
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public boolean deleteTask(int taskId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("task", "id=?", new String[]{String.valueOf(taskId)});
        db.close();
        return rows > 0;
    }

    public void updateTaskState(int taskId, String newState) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("state", newState);
        db.update("task", values, "id=?",
                new String[]{String.valueOf(taskId)});
        db.close();
    }

    public List<WalletRecord> getWalletRecords(String username) {
        List<WalletRecord> records = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM wallet_record WHERE username=? ORDER BY id DESC",
                new String[]{username});
        while (cursor.moveToNext()) {
            records.add(new WalletRecord(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("taskId")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    cursor.getString(cursor.getColumnIndexOrThrow("createTime"))
            ));
        }
        cursor.close();
        db.close();
        return records;
    }

    public static String money(double amount) {
        return new DecimalFormat("0.00").format(amount);
    }

    private double getUserBalance(SQLiteDatabase db, String username) {
        Cursor cursor = db.rawQuery("SELECT balance FROM user WHERE username=?",
                new String[]{username});
        double balance = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        return balance;
    }

    private void addBalance(SQLiteDatabase db, String username, double amount) {
        db.execSQL("UPDATE user SET balance=balance+? WHERE username=?",
                new Object[]{amount, username});
    }

    private void insertWalletRecord(SQLiteDatabase db, String username, double amount,
                                    String type, int taskId, String description) {
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("amount", amount);
        values.put("type", type);
        values.put("taskId", taskId);
        values.put("description", description);
        values.put("createTime", now());
        db.insert("wallet_record", null, values);
    }

    private Task getTaskById(SQLiteDatabase db, int taskId) {
        Cursor cursor = db.rawQuery("SELECT * FROM task WHERE id=?",
                new String[]{String.valueOf(taskId)});
        Task task = cursor.moveToFirst() ? cursorToTask(cursor) : null;
        cursor.close();
        return task;
    }

    private Task cursorToTask(Cursor cursor) {
        return new Task(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("content")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                cursor.getString(cursor.getColumnIndexOrThrow("state")),
                cursor.getString(cursor.getColumnIndexOrThrow("route")),
                cursor.getString(cursor.getColumnIndexOrThrow("pickupAddress")),
                cursor.getString(cursor.getColumnIndexOrThrow("deliveryAddress")),
                cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("remark")),
                cursor.getString(cursor.getColumnIndexOrThrow("publisher")),
                cursor.getString(cursor.getColumnIndexOrThrow("acceptUser")),
                cursor.getString(cursor.getColumnIndexOrThrow("createTime")),
                cursor.getString(cursor.getColumnIndexOrThrow("acceptTime")),
                cursor.getString(cursor.getColumnIndexOrThrow("finishTime")),
                cursor.getString(cursor.getColumnIndexOrThrow("settleTime")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("frozenAmount"))
        );
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date());
    }
}
