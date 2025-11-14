package com.warehouse.simulation.logging;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Small helper for listing log files and reading the last N lines from a log file.
 */
public class LogService {

    private final Path logsDir;

    public LogService(String logsDirectory) {
        this.logsDir = Paths.get(logsDirectory);
    }

    public List<String> listLogFiles() {
        try {
            if (!Files.exists(logsDir)) return Collections.emptyList();
            // include files in the root logs directory and one-level subdirectories
            List<String> files = new ArrayList<>();
            Files.list(logsDir).forEach(p -> {
                try {
                    if (Files.isRegularFile(p)) files.add(p.getFileName().toString());
                    else if (Files.isDirectory(p)) {
                        Files.list(p).filter(Files::isRegularFile).forEach(f -> {
                            Path rel = logsDir.relativize(f);
                            files.add(rel.toString());
                        });
                    }
                } catch (IOException ignored) {
                }
            });
            Collections.sort(files);
            return files;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Read up to maxLines from the end of the file. If file does not exist, returns empty list.
     */
    public List<String> readLastLines(String fileName, int maxLines) {
        Path file = logsDir.resolve(fileName);
        if (!Files.exists(file)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long fileLength = raf.length();
            long pos = fileLength - 1;
            StringBuilder line = new StringBuilder();
            int lines = 0;

            for (; pos >= 0 && lines < maxLines; pos--) {
                raf.seek(pos);
                int read = raf.read();
                if (read == '\n') {
                    if (line.length() > 0) {
                        result.add(line.reverse().toString());
                        line.setLength(0);
                        lines++;
                    }
                } else if (read != '\r') {
                    line.append((char) read);
                }
            }

            if (line.length() > 0 && lines < maxLines) {
                result.add(line.reverse().toString());
            }

            Collections.reverse(result);
            return result;
        } catch (IOException e) {
            return Collections.singletonList("[ERROR] could not read file: " + e.getMessage());
        }
    }

    public Path getLogsDir() {
        return logsDir;
    }
}