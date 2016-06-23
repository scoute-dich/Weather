package de.baumann.weather.helper;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;

public class BrowserDatabase extends SQLiteOpenHelper
{
    public BrowserDatabase(Context context)
            throws NameNotFoundException
    {
        super(context,
                "browser.db",
                null,
                context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newViewsion) {
    }

    private void createTable(SQLiteDatabase db)
    {
        db.execSQL(
                "CREATE TABLE bookmarks (" +
                        "seqno NUMBER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "url TEXT NOT NULL, " +
                        "PRIMARY KEY(seqno))"
        );
    }

    public void loadInitialData()
    {
        int seqno = 0;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        SQLiteStatement stmt = db.compileStatement("INSERT INTO bookmarks VALUES(?, ?, ?)");
        stmt.bindLong(1, seqno);
        stmt.bindString(2, "Wetterdienst");
        stmt.bindString(3, "http://m.wetterdienst.de/");
        stmt.executeInsert();

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public int getRecordCount()
    {
        SQLiteDatabase db = getReadableDatabase();

        int ret = 0;

        String sql = "SELECT COUNT(*) FROM bookmarks";
        Cursor c = db.rawQuery(sql, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            ret = c.getInt(0);
        }
        c.close();
        db.close();

        return ret;
    }

    public void getBookmarks(ArrayList<String[]> data)
    {
        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT seqno,title,url FROM bookmarks ORDER BY seqno";
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            String[] strAry = {c.getString(0), c.getString(1), c.getString(2)};
            data.add(strAry);
            c.moveToNext();
        }
        c.close();
        db.close();
    }

    public void addBookmark(String title, String url)
    {
        int seqno;

        SQLiteDatabase db = getWritableDatabase();

        String sql = "SELECT MAX(seqno) FROM bookmarks";
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        seqno = c.getInt(0) + 1;

        db.beginTransaction();

        SQLiteStatement stmt = db.compileStatement("INSERT INTO bookmarks VALUES(?, ?, ?)");
        stmt.bindLong(1, seqno);
        stmt.bindString(2, title);
        stmt.bindString(3, url);
        stmt.executeInsert();

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();

        c.close();
    }

// --Commented out by Inspection START (09.04.16 19:24):
//    public void updateBookmark(int seqno, String title, String url)
//    {
//        SQLiteDatabase db = getWritableDatabase();
//        db.beginTransaction();
//
//        SQLiteStatement stmt = db.compileStatement("UPDATE bookmarks SET title=?, url=? WHERE seqno=?");
//        stmt.bindLong(1, seqno);
//        stmt.bindString(2, title);
//        stmt.bindString(3, url);
//        stmt.execute();
//
//        db.setTransactionSuccessful();
//        db.endTransaction();
//        db.close();
//    }
// --Commented out by Inspection STOP (09.04.16 19:24)

    public void deleteBookmark(int seqno)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        SQLiteStatement stmt = db.compileStatement("DELETE FROM bookmarks WHERE seqno = ?");
        stmt.bindLong(1, seqno);
        stmt.execute();

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }
}