package workflow;

import io.CSVReader;
import model.BenchmarkResult;
import model.Location;
import model.SortingResult;
import sort.BubbleSort;
import sort.HeapSort;
import sort.MergeSort;
import sort.QuickSort;
import sort.Sorter;
import util.TimingStats;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortingWorkflow {

    public static SortingResult run(Path dataDir, int topN, int warmupRuns, int measuredRuns)
            throws IOException {
        System.out.println("=".repeat(60));
        System.out.println("TASK A - Sorting Algorithm Evaluation");
        System.out.println("=".repeat(60));
        System.out.println("Data directory: " + dataDir.toAbsolutePath().normalize());
        System.out.println("Warm-up runs: " + warmupRuns
                + ", measured runs: " + measuredRuns);

        String[] datasets = {"candidates_A.csv", "candidates_B.csv", "candidates_C.csv"};
        String[] labels = {"Dataset A", "Dataset B", "Dataset C"};
        Sorter[] sorters = {new BubbleSort(), new QuickSort(), new MergeSort(), new HeapSort()};

        List<List<Location>> allTop10 = new ArrayList<>();
        List<BenchmarkResult> benchmarkResults = new ArrayList<>();

        for (int i = 0; i < datasets.length; i++) {
            Path filePath = dataDir.resolve(datasets[i]);
            List<Location> original = CSVReader.readLocations(filePath);
            System.out.println("\n" + labels[i] + " (" + original.size() + " locations)");
            System.out.println("-".repeat(40));

            List<Location> top10 = Collections.emptyList();
            for (Sorter sorter : sorters) {
                BenchmarkResult result = benchmarkSorter(labels[i], datasets[i],
                        original, sorter, warmupRuns, measuredRuns);
                benchmarkResults.add(result);
                if (top10.isEmpty()) {
                    List<Location> ranked = deepCopy(original);
                    sorter.sort(ranked);
                    top10 = new ArrayList<>(ranked.subList(0, Math.min(topN, ranked.size())));
                }
            }

            printTop10(top10);
            allTop10.add(new ArrayList<>(top10));
        }

        return new SortingResult(allTop10, benchmarkResults);
    }

    private static BenchmarkResult benchmarkSorter(String label, String dataset,
                                                   List<Location> original, Sorter sorter,
                                                   int warmupRuns, int measuredRuns) {
        for (int run = 0; run < warmupRuns; run++) {
            List<Location> copy = deepCopy(original);
            sorter.sort(copy);
        }

        TimingStats stats = new TimingStats();
        for (int run = 0; run < measuredRuns; run++) {
            List<Location> copy = deepCopy(original);
            long start = System.nanoTime();
            sorter.sort(copy);
            stats.add(System.nanoTime() - start);

            if (!isSorted(copy)) {
                throw new IllegalStateException(sorter.getName()
                        + " produced an invalid ordering for " + dataset);
            }
        }

        System.out.printf("%-12s  Avg: %8.3f ms  Min: %8.3f ms  Max: %8.3f ms  StdDev: %8.3f ms%n",
                sorter.getName() + ":", stats.averageMs(),
                stats.minMs(), stats.maxMs(), stats.standardDeviationMs());
        return new BenchmarkResult(label, sorter.getName(), stats);
    }

    private static List<Location> deepCopy(List<Location> original) {
        List<Location> copy = new ArrayList<>(original.size());
        for (Location loc : original) {
            copy.add(new Location(loc.getLocationId(), loc.getPriorityScore()));
        }
        return copy;
    }

    private static boolean isSorted(List<Location> locations) {
        for (int i = 1; i < locations.size(); i++) {
            if (locations.get(i - 1).compareTo(locations.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static void printTop10(List<Location> top10) {
        System.out.print("Top 10: ");
        for (int j = 0; j < top10.size(); j++) {
            System.out.print(top10.get(j).getLocationId());
            if (j < top10.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }
}
