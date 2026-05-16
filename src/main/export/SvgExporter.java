package export;

import model.BenchmarkResult;
import model.PathCaseResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SvgExporter {

    public static List<Path> exportBenchmarkCharts(Path outputDir, List<BenchmarkResult> results)
            throws IOException {
        Files.createDirectories(outputDir);
        Path fullChart = outputDir.resolve("sorting_benchmark.svg");
        Path fastChart = outputDir.resolve("sorting_benchmark_fast_algorithms.svg");
        Files.writeString(fullChart, buildBenchmarkSvg(results));
        Files.writeString(fastChart, buildBenchmarkFastAlgorithmsSvg(results));
        return List.of(fullChart, fastChart);
    }

    public static List<Path> exportPathCharts(Path outputDir, List<PathCaseResult> results)
            throws IOException {
        Files.createDirectories(outputDir);
        List<Path> written = new ArrayList<>();
        for (PathCaseResult result : results) {
            Path svgPath = outputDir.resolve(toPathSvgFileName(result));
            Files.writeString(svgPath, buildPathSvg(result));
            written.add(svgPath);
        }
        return written;
    }

    private static String buildBenchmarkSvg(List<BenchmarkResult> results) {
        return buildBenchmarkSvg(results,
                "Sorting Benchmark Average Runtime",
                new String[]{"Bubble Sort", "Quick Sort", "Merge Sort", "Heap Sort"},
                new String[]{"#4e79a7", "#f28e2b", "#59a14f", "#b07aa1"},
                "Error bars show one standard deviation over measured runs.");
    }

    private static String buildBenchmarkFastAlgorithmsSvg(List<BenchmarkResult> results) {
        return buildBenchmarkSvg(results,
                "Fast Sorting Algorithms Average Runtime",
                new String[]{"Quick Sort", "Merge Sort", "Heap Sort"},
                new String[]{"#f28e2b", "#59a14f", "#b07aa1"},
                "Bubble Sort is excluded so Quick, Merge, and Heap Sort differences are easier to compare.");
    }

    private static String buildBenchmarkSvg(List<BenchmarkResult> results, String title,
                                            String[] algorithms, String[] colors, String footerNote) {
        int width = 920;
        int height = 520;
        int left = 80;
        int top = 60;
        int chartWidth = 760;
        int chartHeight = 330;
        int baseLine = top + chartHeight;
        double maxValue = 0;

        for (BenchmarkResult result : results) {
            if (contains(algorithms, result.algorithm)) {
                maxValue = Math.max(maxValue, result.averageMs + result.stdDevMs);
            }
        }
        double axisMax = Math.max(0.1, Math.ceil(maxValue * 10.0) / 10.0);

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        svg.append("<text x=\"").append(width / 2)
                .append("\" y=\"32\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"22\" font-weight=\"700\">")
                .append(escapeXml(title)).append("</text>\n");
        svg.append("<line x1=\"").append(left).append("\" y1=\"").append(baseLine)
                .append("\" x2=\"").append(left + chartWidth).append("\" y2=\"").append(baseLine)
                .append("\" stroke=\"#222\"/>\n");
        svg.append("<line x1=\"").append(left).append("\" y1=\"").append(top)
                .append("\" x2=\"").append(left).append("\" y2=\"").append(baseLine)
                .append("\" stroke=\"#222\"/>\n");

        for (int tick = 0; tick <= 5; tick++) {
            double value = axisMax * tick / 5.0;
            int y = baseLine - (int) Math.round(value / axisMax * chartHeight);
            svg.append("<line x1=\"").append(left - 5).append("\" y1=\"").append(y)
                    .append("\" x2=\"").append(left + chartWidth).append("\" y2=\"").append(y)
                    .append("\" stroke=\"#e0e0e0\"/>\n");
            svg.append("<text x=\"").append(left - 12).append("\" y=\"").append(y + 4)
                    .append("\" text-anchor=\"end\" font-family=\"Arial\" font-size=\"12\" fill=\"#333\">")
                    .append(formatDecimal(value)).append("</text>\n");
        }

        int groupWidth = chartWidth / 3;
        int barWidth = 48;
        int barStep = algorithms.length == 3 ? 66 : 54;
        int totalBarsWidth = barWidth + (algorithms.length - 1) * barStep;

        for (int datasetIndex = 0; datasetIndex < 3; datasetIndex++) {
            int groupStart = left + datasetIndex * groupWidth + (groupWidth - totalBarsWidth) / 2;
            for (int algorithmIndex = 0; algorithmIndex < algorithms.length; algorithmIndex++) {
                BenchmarkResult result = findResult(results,
                        "Dataset " + (char) ('A' + datasetIndex), algorithms[algorithmIndex]);
                if (result == null) {
                    continue;
                }
                int x = groupStart + algorithmIndex * barStep;
                int barHeight = (int) Math.round(result.averageMs / axisMax * chartHeight);
                int y = baseLine - barHeight;
                int errorHeight = (int) Math.round(result.stdDevMs / axisMax * chartHeight);
                int errorTop = Math.max(top, y - errorHeight);
                int errorCenter = x + barWidth / 2;

                svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                        .append("\" width=\"").append(barWidth).append("\" height=\"").append(barHeight)
                        .append("\" fill=\"").append(colors[algorithmIndex]).append("\"/>\n");
                svg.append("<line x1=\"").append(errorCenter).append("\" y1=\"").append(errorTop)
                        .append("\" x2=\"").append(errorCenter).append("\" y2=\"").append(y)
                        .append("\" stroke=\"#222\" stroke-width=\"1.5\"/>\n");
                svg.append("<line x1=\"").append(errorCenter - 8).append("\" y1=\"").append(errorTop)
                        .append("\" x2=\"").append(errorCenter + 8).append("\" y2=\"").append(errorTop)
                        .append("\" stroke=\"#222\" stroke-width=\"1.5\"/>\n");
                svg.append("<text x=\"").append(errorCenter).append("\" y=\"").append(y - 14)
                        .append("\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"11\" fill=\"#222\">")
                        .append(formatDecimal(result.averageMs)).append("</text>\n");
            }
            svg.append("<text x=\"").append(groupStart + totalBarsWidth / 2)
                    .append("\" y=\"").append(baseLine + 30)
                    .append("\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\" font-weight=\"700\">")
                    .append("Dataset ").append((char) ('A' + datasetIndex)).append("</text>\n");
        }

        svg.append("<text x=\"26\" y=\"").append(top + chartHeight / 2)
                .append("\" transform=\"rotate(-90 26 ").append(top + chartHeight / 2)
                .append(")\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\">Average runtime (ms)</text>\n");

        int legendX = left + (algorithms.length == 3 ? 130 : 45);
        int legendY = height - 70;
        for (int i = 0; i < algorithms.length; i++) {
            int x = legendX + i * 170;
            svg.append("<rect x=\"").append(x).append("\" y=\"").append(legendY)
                    .append("\" width=\"16\" height=\"16\" fill=\"").append(colors[i]).append("\"/>\n");
            svg.append("<text x=\"").append(x + 24).append("\" y=\"").append(legendY + 13)
                    .append("\" font-family=\"Arial\" font-size=\"13\" fill=\"#222\">")
                    .append(algorithms[i]).append("</text>\n");
        }

        svg.append("<text x=\"").append(width / 2).append("\" y=\"").append(height - 26)
                .append("\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"12\" fill=\"#555\">")
                .append(escapeXml(footerNote)).append("</text>\n");
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static String buildPathSvg(PathCaseResult result) {
        int nodesPerRow = 8;
        int nodeWidth = 96;
        int nodeHeight = 36;
        int gapX = 38;
        int gapY = 82;
        int left = 54;
        int top = 110;
        int rows = Math.max(1, (result.path.size() + nodesPerRow - 1) / nodesPerRow);
        int width = left * 2 + nodesPerRow * nodeWidth + (nodesPerRow - 1) * gapX;
        int height = top + rows * nodeHeight + Math.max(0, rows - 1) * gapY + 100;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">\n");
        svg.append("<defs><marker id=\"arrow\" markerWidth=\"10\" markerHeight=\"10\" refX=\"8\" refY=\"3\" ")
                .append("orient=\"auto\" markerUnits=\"strokeWidth\"><path d=\"M0,0 L0,6 L9,3 z\" fill=\"#444\"/>")
                .append("</marker></defs>\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        svg.append("<text x=\"").append(width / 2)
                .append("\" y=\"32\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"22\" font-weight=\"700\">")
                .append(escapeXml(result.caseName)).append(" ")
                .append(escapeXml(result.algorithm)).append(" Path</text>\n");
        svg.append("<text x=\"").append(width / 2)
                .append("\" y=\"58\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\" fill=\"#444\">")
                .append(escapeXml(result.start)).append(" to ").append(escapeXml(result.end))
                .append(result.visitOrder.isEmpty() ? "" : " visit order "
                        + escapeXml(String.join(", ", result.visitOrder)))
                .append("</text>\n");
        svg.append("<text x=\"").append(width / 2)
                .append("\" y=\"80\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\" fill=\"#444\">")
                .append(result.reachable ? "Total cost: " + result.totalCost
                        : "No reachable path found")
                .append("</text>\n");

        if (!result.reachable || result.path.isEmpty()) {
            svg.append("</svg>\n");
            return svg.toString();
        }

        for (int i = 0; i < result.path.size() - 1; i++) {
            int[] from = nodeCenter(i, nodesPerRow, nodeWidth, nodeHeight, gapX, gapY, left, top);
            int[] to = nodeCenter(i + 1, nodesPerRow, nodeWidth, nodeHeight, gapX, gapY, left, top);
            if (from[1] == to[1]) {
                int startX = from[0] + nodeWidth / 2;
                int endX = to[0] - nodeWidth / 2 - 8;
                svg.append("<line x1=\"").append(startX).append("\" y1=\"").append(from[1])
                        .append("\" x2=\"").append(endX).append("\" y2=\"").append(to[1])
                        .append("\" stroke=\"#444\" stroke-width=\"2\" marker-end=\"url(#arrow)\"/>\n");
            } else {
                int x1 = from[0] + nodeWidth / 2;
                int y1 = from[1];
                int x2 = to[0] - nodeWidth / 2 - 8;
                int y2 = to[1];
                int midY = y1 + (y2 - y1) / 2;
                svg.append("<polyline points=\"").append(x1).append(',').append(y1)
                        .append(' ').append(x1 + 20).append(',').append(midY)
                        .append(' ').append(x2).append(',').append(midY)
                        .append(' ').append(x2).append(',').append(y2)
                        .append("\" fill=\"none\" stroke=\"#444\" stroke-width=\"2\" marker-end=\"url(#arrow)\"/>\n");
            }
        }

        for (int i = 0; i < result.path.size(); i++) {
            String node = result.path.get(i);
            int[] center = nodeCenter(i, nodesPerRow, nodeWidth, nodeHeight, gapX, gapY, left, top);
            int x = center[0] - nodeWidth / 2;
            int y = center[1] - nodeHeight / 2;
            String fill = "#f4f6f8";
            String stroke = "#6b7280";
            String textFill = "#111827";
            if (i == 0) {
                fill = "#4e79a7";
                stroke = "#2f5d85";
                textFill = "#ffffff";
            } else if (i == result.path.size() - 1) {
                fill = "#59a14f";
                stroke = "#3f7a37";
                textFill = "#ffffff";
            } else if (result.visitOrder.contains(node)) {
                fill = "#f28e2b";
                stroke = "#b86616";
                textFill = "#ffffff";
            }

            svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" width=\"").append(nodeWidth).append("\" height=\"").append(nodeHeight)
                    .append("\" rx=\"6\" fill=\"").append(fill).append("\" stroke=\"").append(stroke).append("\"/>\n");
            svg.append("<text x=\"").append(center[0]).append("\" y=\"").append(center[1] + 5)
                    .append("\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\" font-weight=\"700\" fill=\"")
                    .append(textFill).append("\">").append(escapeXml(node)).append("</text>\n");
            svg.append("<text x=\"").append(x + 7).append("\" y=\"").append(y - 7)
                    .append("\" font-family=\"Arial\" font-size=\"11\" fill=\"#555\">")
                    .append(i + 1).append("</text>\n");
        }

        int legendY = height - 44;
        appendLegend(svg, left, legendY, "#4e79a7", "Start");
        appendLegend(svg, left + 140, legendY, "#f28e2b", "Required via node");
        appendLegend(svg, left + 350, legendY, "#59a14f", "Destination");
        appendLegend(svg, left + 540, legendY, "#f4f6f8", "Intermediate node");

        svg.append("</svg>\n");
        return svg.toString();
    }

    private static BenchmarkResult findResult(List<BenchmarkResult> results,
                                              String dataset, String algorithm) {
        for (BenchmarkResult result : results) {
            if (result.dataset.equals(dataset) && result.algorithm.equals(algorithm)) {
                return result;
            }
        }
        return null;
    }

    private static boolean contains(String[] values, String target) {
        for (String value : values) {
            if (value.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static int[] nodeCenter(int index, int nodesPerRow, int nodeWidth, int nodeHeight,
                                    int gapX, int gapY, int left, int top) {
        int column = index % nodesPerRow;
        int row = index / nodesPerRow;
        int x = left + column * (nodeWidth + gapX) + nodeWidth / 2;
        int y = top + row * (nodeHeight + gapY) + nodeHeight / 2;
        return new int[]{x, y};
    }

    private static void appendLegend(StringBuilder svg, int x, int y, String color, String label) {
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"16\" height=\"16\" rx=\"3\" fill=\"").append(color)
                .append("\" stroke=\"#6b7280\"/>\n");
        svg.append("<text x=\"").append(x + 24).append("\" y=\"").append(y + 13)
                .append("\" font-family=\"Arial\" font-size=\"13\" fill=\"#222\">")
                .append(escapeXml(label)).append("</text>\n");
    }

    private static String toPathSvgFileName(PathCaseResult result) {
        return result.caseName.toLowerCase(Locale.US).replace(' ', '_')
                + "_" + result.algorithm.toLowerCase(Locale.US)
                .replace(" + ", "_")
                .replace(' ', '_') + "_path.svg";
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.US, "%.3f", value);
    }
}
