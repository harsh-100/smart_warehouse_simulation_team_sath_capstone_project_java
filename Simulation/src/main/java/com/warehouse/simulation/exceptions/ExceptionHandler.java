package com.warehouse.simulation.exceptions;

import com.warehouse.simulation.logging.LogManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExceptionHandler {

    private static LogManager logManager = null;
    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    static {
        try {
            logManager = LogManager.getInstance("logs");
        } catch (IOException e) {
            // fallback: keep logManager null and continue; we still print to stderr
            System.err.println("[WARN] ExceptionHandler: couldn't initialize LogManager: " + e.getMessage());
        }
    }

    public static void handle(Throwable t, String context) {
        if (t == null) return;
        String ts = LocalDateTime.now().format(TS);
        String msg = t.getMessage() == null ? t.toString() : t.getMessage();
        String header = String.format("[ERROR] %s (%s): %s", ts, context, msg);

        // Always print header to stderr for immediate visibility
        try {
            System.err.println(header);
        } catch (Throwable ignore) {
            // ignore
        }

        // Write to system daily log when possible. Guard against any exceptions here.
        if (logManager != null) {
            try {
                    String fileName = String.format("SystemLogs/SYSTEM-%s.log", LocalDate.now());
                    logManager.writeLog(fileName, header);

                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    logManager.writeLog(fileName, sw.toString());
                // Also store exception in the in-memory ExceptionStore for UI consumption
                try {
                    ExceptionStore.ExceptionRecord rec = new ExceptionStore.ExceptionRecord(ts, context, msg, sw.toString());
                    ExceptionStore.getInstance().add(rec);
                } catch (Throwable ignore) {
                }
            } catch (Throwable writeEx) {
                // best-effort only: print to stderr if writing fails
                try {
                    System.err.println("[WARN] ExceptionHandler failed to write log: " + writeEx.getMessage());
                } catch (Throwable ignore) {
                }
            }
        }
    }

    
    public static void handleAndThrow(Throwable t, String context) throws Exception {
        handle(t, context);
        if (t instanceof Exception) throw (Exception) t;
        throw new RuntimeException(t);
    }
}
