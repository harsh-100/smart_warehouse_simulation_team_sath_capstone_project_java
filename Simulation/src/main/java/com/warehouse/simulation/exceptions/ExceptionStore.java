package com.warehouse.simulation.exceptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Thread-safe in-memory store for recent exceptions so the UI can display them quickly.
 */
public class ExceptionStore {

    public static class ExceptionRecord {
        public final String timestamp;
        public final String context;
        public final String message;
        public final String stacktrace;

        public ExceptionRecord(String timestamp, String context, String message, String stacktrace) {
            this.timestamp = timestamp;
            this.context = context;
            this.message = message;
            this.stacktrace = stacktrace;
        }

        @Override
        public String toString() {
            return String.format("%s [%s] %s", timestamp, context, message);
        }
    }

    private static final int DEFAULT_CAPACITY = 200;
    private final LinkedList<ExceptionRecord> buffer = new LinkedList<>();
    private final int capacity;

    private static final ExceptionStore INSTANCE = new ExceptionStore(DEFAULT_CAPACITY);

    public static ExceptionStore getInstance() {
        return INSTANCE;
    }

    public ExceptionStore(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void add(ExceptionRecord rec) {
        buffer.addFirst(rec);
        if (buffer.size() > capacity) buffer.removeLast();
    }

    public synchronized List<ExceptionRecord> recent() {
        return new ArrayList<>(buffer);
    }

    public synchronized List<ExceptionRecord> recent(int max) {
        if (max <= 0) return Collections.emptyList();
        List<ExceptionRecord> copy = new ArrayList<>();
        int i = 0;
        for (ExceptionRecord r : buffer) {
            if (i++ >= max) break;
            copy.add(r);
        }
        return copy;
    }
}