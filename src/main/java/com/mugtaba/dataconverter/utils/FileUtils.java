package main.java.com.mugtaba.dataconverter.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtils {

    /**
     * Reads the entire content of a file as a string using UTF-8 encoding.
     *
     * @param filePath the path to the file to read
     * @return the file content as a string, trimmed of leading/trailing whitespace
     * @throws IOException              if an I/O error occurs reading the file
     * @throws IllegalArgumentException if the file path is null or empty
     */
    public static String readFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + filePath);
        }

        if (!Files.isReadable(path)) {
            throw new IOException("File is not readable: " + filePath);
        }

        if (Files.isDirectory(path)) {
            throw new IOException("Path is a directory, not a file: " + filePath);
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return content.trim();
        } catch (IOException e) {
            throw new IOException("Failed to read file: " + filePath + ". " + e.getMessage(), e);
        }
    }

    /**
     * Writes content to a file using UTF-8 encoding. Creates the file and any necessary
     * parent directories if they don't exist.
     *
     * @param filePath the path to the file to write
     * @param content  the content to write to the file
     * @throws IOException              if an I/O error occurs writing the file
     * @throws IllegalArgumentException if the file path or content is null
     */
    public static void writeFile(String filePath, String content) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        Path path = Paths.get(filePath);

        // Create parent directories if they don't exist
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new IOException("Failed to create parent directories for: " + filePath + ". " + e.getMessage(), e);
            }
        }

        try {
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Failed to write file: " + filePath + ". " + e.getMessage(), e);
        }
    }
}