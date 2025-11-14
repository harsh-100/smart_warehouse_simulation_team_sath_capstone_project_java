package com.warehouse.simulation.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized LogManager singleton.
 * Keeps per-file in-memory ring buffers and allows listeners to be notified of new log lines.
 */
public class LogManager {

    public interface LogListener {
        void onLog(String fileName, String line);
    }

    private static volatile LogManager INSTANCE;

    private final Path logDir;
    private final int perFileCapacity = 1000;
    private final Map<String, Deque<String>> fileBuffers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<LogListener> listeners = new CopyOnWriteArrayList<>();

    private LogManager(String directory) throws IOException {
        this.logDir = Paths.get(directory);
        if (!Files.exists(logDir)) Files.createDirectories(logDir);
    }

    public static synchronized LogManager getInstance(String baseDir) throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new LogManager(baseDir);
        }
        return INSTANCE;
    }

    public static LogManager getInstance() {
        if (INSTANCE == null) {
            try {
                return getInstance("logs");
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize LogManager: " + e.getMessage(), e);
            }
        }
        return INSTANCE;
    }

    public void addListener(LogListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeListener(LogListener l) {
        listeners.remove(l);
    }

    public void writeLog(String fileName, String logMessage) {
        Path logFilePath = logDir.resolve(fileName);
        try {
            Path parent = logFilePath.getParent();
            if (parent != null) {
                if (Files.exists(parent) && !Files.isDirectory(parent)) {
                    // previous code might have created a file where we now expect a directory.
                    // Move the file aside with a .bak suffix so we can create the directory.
                    try {
                        Path backup = parent.resolveSibling(parent.getFileName().toString() + ".bak");
                        Files.move(parent, backup, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignored) {
                        // ignore and attempt to create directory; will fail later if not possible
                    }
                }
                if (!Files.exists(parent)) Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(logFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(logMessage);
                writer.newLine();
            }

            // update in-memory buffer
            Deque<String> dq = fileBuffers.computeIfAbsent(fileName, k -> new ConcurrentLinkedDeque<>());
            synchronized (dq) {
                dq.addLast(logMessage);
                while (dq.size() > perFileCapacity) dq.removeFirst();
            }

            // notify listeners
            for (LogListener l : listeners) {
                try {
                    l.onLog(fileName, logMessage);
                } catch (Throwable ignore) {
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            // best-effort only
        }
    }

    public List<String> tail(String fileName, int maxLines) {
        Deque<String> dq = fileBuffers.get(fileName);
        if (dq != null) {
            synchronized (dq) {
                List<String> copy = new ArrayList<>(dq);
                int from = Math.max(0, copy.size() - maxLines);
                return copy.subList(from, copy.size());
            }
        }

        // fallback: read from disk
        try {
            LogService ls = new LogService(logDir.toString());
            return ls.readLastLines(fileName, maxLines);
        } catch (Exception e) {
            return Collections.singletonList("[ERROR] could not read logs: " + e.getMessage());
        }
    }

    //move a log file
    public void moveLog(String from , String to) throws IOException {
        try{
            Path sourcePath = logDir.resolve(from);
            Path targetPath = logDir.resolve(to);
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Delete log file
    public void deleteLog(String fileName) {
        Path logFilePath = logDir.resolve(fileName);
        try {
            Files.deleteIfExists(logFilePath);
            fileBuffers.remove(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Archive log file using byte stream
    public void archiveLog(String fileName) {
        Path logFilePath = logDir.resolve(fileName);
        Path archiveFilePath = logDir.resolve("archive");
        Path dest = archiveFilePath.resolve(fileName);
        try {
            if (!Files.exists(archiveFilePath)) Files.createDirectories(archiveFilePath);
            try (InputStream in = Files.newInputStream(logFilePath);
                 OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE)) {

                byte[] buffer = new byte[1024]; // 1KB
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Log file archived to: " + dest.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Path getLogsDir() {
        return logDir;
    }

}