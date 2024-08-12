package com.example.messagemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "messages.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT, image BLOB)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(db);
    }

    public void addMessage(String text, byte[] image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("text", text);
        values.put("image", image);
        db.insert("messages", null, values);
        db.close();
    }

    public Cursor getAllMessages() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id AS _id, text, image FROM messages", null);
    }

    public Cursor searchMessages(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id AS _id, text, image FROM messages WHERE text LIKE ?", new String[]{"%" + query + "%"});
    }

    public Cursor getMessageId(String text) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id AS _id, text, image FROM messages WHERE text = ?", new String[]{text});
    }

    public void updateMessage(int id, String newText, byte[] newImage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("text", newText);
        values.put("image", newImage);
        db.update("messages", values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteMessage(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("messages", "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
