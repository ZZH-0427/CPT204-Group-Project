
import export.CsvExporter;
import export.SvgExporter;
import graph.Graph;
import io.CSVReader;
import model.Edge;
import model.PathTaskResult;
import model.SortingResult;
import workflow.PathWorkflow;
import workflow.SortingWorkflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    private static final int TOP_N = 10;
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 30;
    private static final Path OUTPUT_DIR = Paths.get("out");

    public static void main(String[] args) throws IOException {
        Path dataDir = resolveDataDir(args);

        SortingResult sortingResult = SortingWorkflow.run(
                dataDir, TOP_N, WARMUP_RUNS, MEASURED_RUNS);
        exportBenchmarkArtifacts(sortingResult);

        Graph graph = loadGraph(dataDir);
        PathTaskResult pathTaskResult = PathWorkflow.run(graph, sortingResult.allTop10);
        exportPathArtifacts(pathTaskResult);
    }

    private static Graph loadGraph(Path dataDir) throws IOException {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TASK B - Graph Algorithm (Shortest Path)");
        System.out.println("=".repeat(60));

        Graph graph = new Graph();
        List<Edge> edges = CSVReader.readEdges(dataDir.resolve("paths.csv"));
        graph.addEdges(edges);
        System.out.println("\nGraph loaded: " + graph.nodeCount() + " nodes, "
                + graph.edgeCount() + " undirected edges");
        return graph;
    }

    private static void exportBenchmarkArtifacts(SortingResult result) throws IOException {
        Path csvPath = CsvExporter.exportBenchmarkResults(OUTPUT_DIR, result.benchmarkResults);
        List<Path> svgPaths = SvgExporter.exportBenchmarkCharts(OUTPUT_DIR, result.benchmarkResults);

        System.out.println("\nBenchmark artifacts written:");
        System.out.println("  CSV: " + normalized(csvPath));
        for (Path svgPath : svgPaths) {
            System.out.println("  SVG: " + normalized(svgPath));
        }
    }

    private static void exportPathArtifacts(PathTaskResult result) throws IOException {
        Path csvPath = CsvExporter.exportPathResults(OUTPUT_DIR, result.pathCaseResults);
        List<Path> svgPaths = SvgExporter.exportPathCharts(OUTPUT_DIR, result.pathCaseResults);

        System.out.println("\nPath artifacts written:");
        System.out.println("  CSV: " + normalized(csvPath));
        for (Path svgPath : svgPaths) {
            System.out.println("  SVG: " + normalized(svgPath));
        }

        Path comparisonCsvPath = CsvExporter.exportPathComparison(
                OUTPUT_DIR, result.comparisonResult);
        System.out.println("\nPath comparison artifact written:");
        System.out.println("  CSV: " + normalized(comparisonCsvPath));
    }

    private static Path resolveDataDir(String[] args) throws IOException {
        if (args.length > 0) {
            Path explicitPath = Paths.get(args[0]);
            if (Files.isDirectory(explicitPath)) {
                return explicitPath;
            }
            throw new IOException("Data directory does not exist: " + explicitPath);
        }

        Path[] candidates = {
                Paths.get("../resource/Group Project Datasets"),
                Paths.get("resource/Group Project Datasets")
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }

        throw new IOException("Could not find the dataset directory. "
                + "Run from the project directory, or pass the dataset path as the first argument.");
    }

    private static String normalized(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
