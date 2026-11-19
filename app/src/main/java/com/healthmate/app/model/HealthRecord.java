package com.healthmate.app.model;

public class HealthRecord {
    private final long id;
    private final String date;
    private final int steps;
    private final int calories;
    private final int waterMl;
    private final String note;

    public HealthRecord(long id, String date, int steps, int calories, int waterMl, String note) {
        this.id = id;
        this.date = date;
        this.steps = steps;
        this.calories = calories;
        this.waterMl = waterMl;
        this.note = note == null ? "" : note.trim();
    }

    // Used by the forms (a new record has no id yet)
    public HealthRecord(String date, int steps, int calories, int waterMl, String note) {
        this(0, date, steps, calories, waterMl, note);
    }

    public long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public int getSteps() {
        return steps;
    }

    public int getCalories() {
        return calories;
    }

    public int getWaterMl() {
        return waterMl;
    }

    public String getNote() {
        return note;
    }
}