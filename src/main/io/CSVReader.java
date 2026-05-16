package io;

import model.Edge;
import model.Location;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

    public static List<Location> readLocations(Path filePath) throws IOException {
        List<Location> locations = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            requireHeader(br.readLine(), filePath);
            int lineNumber = 1;
            String line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length != 2) {
                    throw new IOException("Invalid location row in " + filePath
                            + " at line " + lineNumber + ": " + line);
                }

                String id = parts[0].trim();
                int score = parseInt(parts[1].trim(), filePath, lineNumber);
                locations.add(new Location(id, score));
            }
        }
        return locations;
    }

    public static List<Edge> readEdges(Path filePath) throws IOException {
        List<Edge> edges = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            requireHeader(br.readLine(), filePath);
            int lineNumber = 1;
            String line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length != 3) {
                    throw new IOException("Invalid edge row in " + filePath
                            + " at line " + lineNumber + ": " + line);
                }

                String from = parts[0].trim();
                String to = parts[1].trim();
                int weight = parseInt(parts[2].trim(), filePath, lineNumber);
                edges.add(new Edge(from, to, weight));
            }
        }
        return edges;
    }

    private static void requireHeader(String header, Path filePath) throws IOException {
        if (header == null || header.trim().isEmpty()) {
            throw new IOException("Missing CSV header in " + filePath);
        }
    }

    private static int parseInt(String value, Path filePath, int lineNumber) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid number in " + filePath
                    + " at line " + lineNumber + ": " + value, e);
        }
    }
}
