package com.healthmate.app.controller;

import android.content.Context;

import com.healthmate.app.data.LocalDatabase;
import com.healthmate.app.model.HealthRecord;

import java.util.List;

public class HealthController {

    private final LocalDatabase database;

    public HealthController(Context context) {
        database = LocalDatabase.getInstance(context);
    }

    public static final int MAX_STEPS = 100_000;
    public static final int MAX_CALORIES = 10_000;
    public static final int MAX_WATER_ML = 10_000;

    /** Input validation. Returns an error message, or null when valid. */
    public String validate(int steps, int calories, int waterMl) {
        if (steps < 0 || steps > MAX_STEPS
                || calories < 0 || calories > MAX_CALORIES
                || waterMl < 0 || waterMl > MAX_WATER_ML) {
            return "Keep steps under 100,000, calories under 10,000 "
                    + "and water under 10,000 ml.";
        }
        return null;
    }

    public void saveHealthRecord(String email,
                                 HealthRecord record) throws Exception {
        database.saveRecord(email, record);
    }

    public List<HealthRecord> getHealthRecords(String email)
            throws Exception {
        return database.getRecords(email);
    }

    public HealthRecord getHealthRecord(String email, String date)
            throws Exception {
        return database.getRecord(email, date);
    }

    public void deleteHealthRecord(String email, String date)
            throws Exception {
        database.deleteRecord(email, date);
    }
}