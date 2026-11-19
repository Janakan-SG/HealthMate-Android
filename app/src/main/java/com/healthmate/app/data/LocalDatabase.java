package com.healthmate.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.healthmate.app.model.HealthRecord;
import com.healthmate.app.model.User;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * SQLite database for HealthMate.
 * Tables: users and health_records (id, date, steps, calories, water).
 */
public class LocalDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "healthmate.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_RECORDS = "health_records";

    private static final String SESSION_PREFS = "healthmate-session";
    private static final String SESSION_EMAIL = "signed_in_email";
    private static final int HASH_ITERATIONS = 120_000;
    private static final int HASH_BYTES = 32;

    private final SharedPreferences sessionPreferences;

    private static LocalDatabase instance;

    public static synchronized LocalDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDatabase(context.getApplicationContext());
        }
        return instance;
    }

    public LocalDatabase(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
        sessionPreferences = context.getApplicationContext()
                .getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- CREATE TABLES

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "email TEXT NOT NULL UNIQUE, "
                + "salt TEXT NOT NULL, "
                + "password_hash TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_RECORDS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT NOT NULL, "
                + "date TEXT NOT NULL, "
                + "steps INTEGER NOT NULL, "
                + "calories INTEGER NOT NULL, "
                + "water INTEGER NOT NULL, "
                + "note TEXT, "
                + "UNIQUE (user_email, date), "
                + "FOREIGN KEY (user_email) REFERENCES " + TABLE_USERS + "(email) ON DELETE CASCADE)");

        seedDummyData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple approach for coursework: rebuild the tables when the version number changes.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /** Dummy data for testing: a demo account with 7 days of records. */
    private void seedDummyData(SQLiteDatabase db) {
        try {
            String demoEmail = "janakan@gmail.com";
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            ContentValues user = new ContentValues();
            user.put("name", "Janakan");
            user.put("email", demoEmail);
            user.put("salt", toHex(salt));
            user.put("password_hash", toHex(hashPassword("janakan123".toCharArray(), salt)));
            db.insert(TABLE_USERS, null, user);

            int[][] sample = {
                    {7850, 420, 2000}, {10250, 610, 2500}, {5400, 300, 1500},
                    {12000, 680, 3000}, {8300, 450, 2200}, {6100, 350, 1800}, {9600, 520, 2600}
            };
            String[] notes = {"Evening walk", "Gym session", "Rest day", "Long hike",
                    "Morning jog", "Cycling", "Swimming"};
            LocalDate today = LocalDate.now();
            for (int i = 0; i < sample.length; i++) {
                ContentValues record = new ContentValues();
                record.put("user_email", demoEmail);
                record.put("date", today.minusDays(i).toString());
                record.put("steps", sample[i][0]);
                record.put("calories", sample[i][1]);
                record.put("water", sample[i][2]);
                record.put("note", notes[i]);
                db.insert(TABLE_RECORDS, null, record);
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not create demo data", error);
        }
    }


    //  USERS

    public User register(String name, String email, char[] password) throws Exception {
        String normalizedEmail = normalizeEmail(email);
        SQLiteDatabase db = getWritableDatabase();

        try (Cursor cursor = db.query(TABLE_USERS, new String[]{"id"}, "email = ?",
                new String[]{normalizedEmail}, null, null, null)) {
            if (cursor.moveToFirst()) {
                throw new IllegalArgumentException("An account with that email already exists.");
            }
        }

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("email", normalizedEmail);
        values.put("salt", toHex(salt));
        values.put("password_hash", toHex(hashPassword(password, salt)));
        db.insertOrThrow(TABLE_USERS, null, values);
        return new User(name.trim(), normalizedEmail);
    }

    public User authenticate(String email, char[] password) throws Exception {
        String normalizedEmail = normalizeEmail(email);
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS,
                new String[]{"name", "salt", "password_hash"}, "email = ?",
                new String[]{normalizedEmail}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            byte[] salt = fromHex(cursor.getString(1));
            byte[] expectedHash = fromHex(cursor.getString(2));
            byte[] actualHash = hashPassword(password, salt);
            if (MessageDigest.isEqual(expectedHash, actualHash)) {
                return new User(cursor.getString(0), normalizedEmail);
            }
            return null;
        }
    }

    public User findUser(String email) throws Exception {
        String normalizedEmail = normalizeEmail(email);
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS, new String[]{"name"}, "email = ?",
                new String[]{normalizedEmail}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new User(cursor.getString(0), normalizedEmail);
            }
            return null;
        }
    }

    // ---------------------------------------------------------------- SESSION (Remember me)

    public void saveSession(String email) {
        sessionPreferences.edit().putString(SESSION_EMAIL, normalizeEmail(email)).apply();
    }

    public String getSessionEmail() {
        return sessionPreferences.getString(SESSION_EMAIL, null);
    }

    public void clearSession() {
        sessionPreferences.edit().remove(SESSION_EMAIL).apply();
    }

    // ---------------------------------------------------------------- RECORDS (CRUD)

    /** READ: all records of a user, newest first. */
    public List<HealthRecord> getRecords(String email) throws Exception {
        SQLiteDatabase db = getReadableDatabase();
        List<HealthRecord> result = new ArrayList<>();
        try (Cursor cursor = db.query(TABLE_RECORDS, null, "user_email = ?",
                new String[]{normalizeEmail(email)}, null, null, "date DESC")) {
            while (cursor.moveToNext()) {
                result.add(toRecord(cursor));
            }
        }
        return result;
    }

    /** READ: one record by date. */
    public HealthRecord getRecord(String email, String date) throws Exception {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_RECORDS, null, "user_email = ? AND date = ?",
                new String[]{normalizeEmail(email), date}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return toRecord(cursor);
            }
            return null;
        }
    }

    /** CREATE or UPDATE: updates the day's record if it exists, otherwise inserts a new one. */
    public void saveRecord(String email, HealthRecord record) throws Exception {
        String normalizedEmail = normalizeEmail(email);
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_email", normalizedEmail);
        values.put("date", record.getDate());
        values.put("steps", record.getSteps());
        values.put("calories", record.getCalories());
        values.put("water", record.getWaterMl());
        values.put("note", record.getNote());

        int updatedRows = db.update(TABLE_RECORDS, values, "user_email = ? AND date = ?",
                new String[]{normalizedEmail, record.getDate()});
        if (updatedRows == 0) {
            db.insertOrThrow(TABLE_RECORDS, null, values);
        }
    }

    /** DELETE: removes one day's record. */
    public void deleteRecord(String email, String date) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_RECORDS, "user_email = ? AND date = ?",
                new String[]{normalizeEmail(email), date});
    }

    private HealthRecord toRecord(Cursor cursor) {
        return new HealthRecord(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                cursor.getInt(cursor.getColumnIndexOrThrow("steps")),
                cursor.getInt(cursor.getColumnIndexOrThrow("calories")),
                cursor.getInt(cursor.getColumnIndexOrThrow("water")),
                cursor.getString(cursor.getColumnIndexOrThrow("note"))
        );
    }

    // ---------------------------------------------------------------- HELPERS

    private byte[] hashPassword(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String toHex(byte[] data) {
        StringBuilder result = new StringBuilder(data.length * 2);
        for (byte value : data) {
            result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        }
        return result.toString();
    }

    private byte[] fromHex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            result[i] = (byte) ((high << 4) + low);
        }
        return result;
    }
}