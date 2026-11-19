
package com.healthmate.app.controller;

import com.healthmate.app.model.HealthRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HistoryController {

    public List<HealthRecord> searchHistory(
            List<HealthRecord> records, String query) {

        List<HealthRecord> results = new ArrayList<>();

        if (records == null) return results;

        if (query == null || query.trim().isEmpty()) {
            results.addAll(records);
            return results;
        }

        String search = query.trim().toLowerCase(Locale.ROOT);

        for (HealthRecord record : records) {

            String date = record.getDate() == null
                    ? "" : record.getDate();

            String note = record.getNote() == null
                    ? "" : record.getNote();

            String formattedDate = date;

            try {
                formattedDate = LocalDate.parse(date)
                        .format(DateTimeFormatter.ofPattern(
                                "dd MMM yyyy", Locale.ENGLISH));
            } catch (Exception ignored) {
            }

            if (date.toLowerCase(Locale.ROOT).contains(search)
                    || formattedDate.toLowerCase(Locale.ROOT).contains(search)
                    || note.toLowerCase(Locale.ROOT).contains(search)) {

                results.add(record);
            }
        }

        return results;
    }

    public List<HealthRecord> sortNewestFirst(
            List<HealthRecord> records) {

        List<HealthRecord> results = new ArrayList<>();

        if (records != null) results.addAll(records);

        results.sort(
                Comparator.comparing(HealthRecord::getDate).reversed());

        return results;
    }

    public List<HealthRecord> sortOldestFirst(
            List<HealthRecord> records) {

        List<HealthRecord> results = new ArrayList<>();

        if (records != null) results.addAll(records);

        results.sort(
                Comparator.comparing(HealthRecord::getDate));

        return results;
    }

    public List<HealthRecord> filterHistory(
            List<HealthRecord> records,
            String query,
            String range,
            boolean oldestFirst) {

        List<HealthRecord> results = searchHistory(records, query);

        if ("7days".equals(range)) {

            LocalDate cutoff = LocalDate.now().minusDays(6);

            results.removeIf(record -> {
                try {
                    LocalDate date = LocalDate.parse(record.getDate());
                    return date.isBefore(cutoff);
                } catch (Exception error) {
                    return true;
                }
            });
        }

        return oldestFirst
                ? sortOldestFirst(results)
                : sortNewestFirst(results);
    }
}
