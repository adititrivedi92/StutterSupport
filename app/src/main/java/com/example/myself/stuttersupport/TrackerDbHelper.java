package com.example.myself.stuttersupport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by Myself on 6/16/2017.
 *
 * Based off DbHelper.java from Learning Android by Marko Gargenta
 */

//TODO: Refactor so StreakDbHelper & TrackerDbHelper have a parent

public class TrackerDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "tracker.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE = "tracker";
    public static final String C_DATE = "date";
    Context context;

    public TrackerDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    /**
     * Called when DB is first created
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE + " (" + C_DATE + " DATE PRIMARY KEY)";
        db.execSQL(sql);
        Log.d("TrackerDbHelper", "onCreate w sql: " + sql);
    }

    /**
     * Called whenever the database receives an update
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public void addDateToDb(StreakDbHelper sdbh) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.clear();
        String dateString = "";
        Date currentDate = new Date();
        dateString = (currentDate.getYear() + 1900) + "-"
                + (currentDate.getMonth()) + "-" + currentDate.getDate();
        values.put(C_DATE, dateString);
        try {
            db.insertOrThrow(TABLE, null, values);
        } catch (SQLException e){
            Log.e("DATABASE", "ERROR when adding to DB");
            e.printStackTrace();
        }
        db.close();

        updateStreaks(sdbh);
    }

    /**
     * By updating current, may update best as a side effect
     * Calendar is required as it obeys calendar rules when adding and subtracting
     * Date does not appear to do so.
     *
     * @param sdbh Needs to be provided a helper due to Context requirement
     */
    public void updateStreaks(StreakDbHelper sdbh) {
        int newStreak = 0;
        HashSet<Date> dates = getDates();
        Date dateEntry = new Date();
        Calendar cal = Calendar.getInstance();

        dateEntry = clearTime(dateEntry);
        while(dates.contains(dateEntry)){
            newStreak++;

            cal.setTime(dateEntry);
            cal.add(Calendar.DATE, -1);
            dateEntry = cal.getTime();
        }

        sdbh.updateCurrent(newStreak);
    }

    private Date clearTime(Date dateEntry) {
        long fullTime = dateEntry.getTime();
        long millis = fullTime % 1000;
        Date newDate = new Date(fullTime - millis);
        newDate.setHours(0);
        newDate.setMinutes(0);
        newDate.setSeconds(0);
        return newDate;
    }

    public HashSet<Date> getDates() {
        HashSet<Date> dates = new HashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(true, TABLE, null, null, null, null, null, null, null);
        String strDate;
        int year, month, date;
        final int COLUMN_INDEX = 0; //There's only 1 column in this table

        cursor.moveToFirst();
        try {
            if (cursor.isNull(COLUMN_INDEX)) {
                db.close();
                return dates;
            }
        } catch (CursorIndexOutOfBoundsException e){
            db.close();
            return dates;
        }
        //Past this point, save to assume there's dates in the database
        do{ //hoo boy alexis you're really pushing it here
            strDate = cursor.getString(COLUMN_INDEX);
            year = Integer.parseInt(strDate.substring(0, 4)) - 1900;
            if (doubleDigitMonth(strDate)){
                month = Integer.parseInt(strDate.substring(5,7));
                date = Integer.parseInt(strDate.substring(8));
            } else {
                month = Integer.parseInt(String.valueOf(strDate.charAt(5)));
                date = Integer.parseInt(strDate.substring(7));
            }
            dates.add(new Date(year, month, date));
        } while (cursor.moveToNext());
        db.close();
        return dates;
    }

    private boolean doubleDigitMonth(String strDate) {
        return strDate.charAt(5) == '1' && strDate.charAt(6) != '-';
    }
}
