package com.example.medpro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class userDetailsDBHelper extends SQLiteOpenHelper {

    // Database constants
    private static final String DATABASE_NAME = "UserData.db";
    private static final int DATABASE_VERSION = 2; // Incremented version due to schema change
    private static final String TABLE_NAME = "UserDetails";

    // Table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_AGE = "age";
    private static final String COLUMN_HEIGHT = "height";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_PHONE_NUMBER = "phone_number";
    private static final String COLUMN_GMAIL = "gmail"; // New column

    // Constructor
    public userDetailsDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table query
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_AGE + " INTEGER, " +
                COLUMN_HEIGHT + " REAL, " +
                COLUMN_WEIGHT + " REAL, " +
                COLUMN_PHONE_NUMBER + " TEXT, " +
                COLUMN_GMAIL + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database schema updates
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_GMAIL + " TEXT");
        }
    }

    // Insert data into the database
    public boolean insertData( int age, double height, double weight, String phoneNumber, String gmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AGE, age);
        values.put(COLUMN_HEIGHT, height);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_PHONE_NUMBER, phoneNumber);
        values.put(COLUMN_GMAIL, gmail);
        Log.d("userDetailsDBHelper", "Gmail value: " + gmail);
        onCreate(db);

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1; // Returns true if data is inserted successfully
    }

    // Retrieve all data from the database
    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    // Update a record in the database
    public boolean updateData(int id, String name, int age, double height, double weight, String phoneNumber, String gmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_AGE, age);
        values.put(COLUMN_HEIGHT, height);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_PHONE_NUMBER, phoneNumber);
        values.put(COLUMN_GMAIL, gmail);

        int rowsUpdated = db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return rowsUpdated > 0; // Returns true if the update is successful
    }

    public Cursor getDataByGmail(String gmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Query the database to get all columns where the Gmail matches the input
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_GMAIL + " = ?", new String[]{gmail});
        return cursor;
    }


    // Delete a record from the database
    public boolean deleteData(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return rowsDeleted > 0; // Returns true if the deletion is successful
    }
}
