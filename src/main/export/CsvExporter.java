package export;

import model.BenchmarkResult;
import model.PathCaseResult;
import model.PathComparisonResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CsvExporter {

    public static Path exportBenchmarkResults(Path outputDir, List<BenchmarkResult> results)
            throws IOException {
        Files.createDirectories(outputDir);
        Path csvPath = outputDir.resolve("sorting_results.csv");
        Files.writeString(csvPath, buildBenchmarkCsv(results));
        return csvPath;
    }

    public static Path exportPathResults(Path outputDir, List<PathCaseResult> results)
            throws IOException {
        Files.createDirectories(outputDir);
        Path csvPath = outputDir.resolve("shortest_paths.csv");
        Files.writeString(csvPath, buildPathCsv(results));
        return csvPath;
    }

    public static Path exportPathComparison(Path outputDir, PathComparisonResult result)
            throws IOException {
        Files.createDirectories(outputDir);
        Path csvPath = outputDir.resolve("bfs_unweighted_comparison.csv");
        Files.writeString(csvPath, buildPathComparisonCsv(result));
        return csvPath;
    }

    private static String buildBenchmarkCsv(List<BenchmarkResult> results) {
        StringBuilder csv = new StringBuilder();
        csv.append("dataset,algorithm,avg_ms,min_ms,max_ms,stddev_ms\n");
        for (BenchmarkResult result : results) {
            csv.append(result.dataset).append(',')
                    .append(result.algorithm).append(',')
                    .append(formatDecimal(result.averageMs)).append(',')
                    .append(formatDecimal(result.minMs)).append(',')
                    .append(formatDecimal(result.maxMs)).append(',')
                    .append(formatDecimal(result.stdDevMs)).append('\n');
        }
        return csv.toString();
    }

    private static String buildPathCsv(List<PathCaseResult> results) {
        StringBuilder csv = new StringBuilder();
        csv.append("case_name,algorithm,start,end,required_via_nodes,visit_order,reachable,total_cost,leg_costs,path\n");
        for (PathCaseResult result : results) {
            csv.append(csvEscape(result.caseName)).append(',')
                    .append(csvEscape(result.algorithm)).append(',')
                    .append(csvEscape(result.start)).append(',')
                    .append(csvEscape(result.end)).append(',')
                    .append(csvEscape(String.join(";", result.viaNodes))).append(',')
                    .append(csvEscape(String.join(";", result.visitOrder))).append(',')
                    .append(result.reachable).append(',')
                    .append(result.totalCost).append(',')
                    .append(csvEscape(joinIntegers(result.legCosts, " + "))).append(',')
                    .append(csvEscape(String.join(" -> ", result.path))).append('\n');
        }
        return csv.toString();
    }

    private static String buildPathComparisonCsv(PathComparisonResult result) {
        StringBuilder csv = new StringBuilder();
        csv.append("algorithm,start,end,reachable,metric_value,metric_name,path\n");
        csv.append("Dijkstra (weighted),")
                .append(csvEscape(result.start)).append(',')
                .append(csvEscape(result.end)).append(',')
                .append(result.weighted.isReachable()).append(',')
                .append(result.weighted.totalCost).append(',')
                .append("total_weight,")
                .append(csvEscape(String.join(" -> ", result.weighted.path))).append('\n');
        csv.append("Floyd + DP (weighted),")
                .append(csvEscape(result.start)).append(',')
                .append(csvEscape(result.end)).append(',')
                .append(result.floyd.isReachable()).append(',')
                .append(result.floyd.totalCost).append(',')
                .append("total_weight,")
                .append(csvEscape(String.join(" -> ", result.floyd.path))).append('\n');
        csv.append("BFS (unweighted),")
                .append(csvEscape(result.start)).append(',')
                .append(csvEscape(result.end)).append(',')
                .append(result.unweighted.isReachable()).append(',')
                .append(result.unweighted.totalCost).append(',')
                .append("edge_count,")
                .append(csvEscape(String.join(" -> ", result.unweighted.path))).append('\n');
        return csv.toString();
    }

    private static String joinIntegers(List<Integer> values, String delimiter) {
        if (values.isEmpty()) {
            return "";
        }

        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                joined.append(delimiter);
            }
            joined.append(values.get(i));
        }
        return joined.toString();
    }

    private static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.US, "%.3f", value);
    }
}
