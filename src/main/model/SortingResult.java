package model;

import java.util.ArrayList;
import java.util.List;

public class SortingResult {
    public final List<List<Location>> allTop10;
    public final List<BenchmarkResult> benchmarkResults;

    public SortingResult(List<List<Location>> allTop10, List<BenchmarkResult> benchmarkResults) {
        this.allTop10 = new ArrayList<>();
        for (List<Location> locations : allTop10) {
            this.allTop10.add(new ArrayList<>(locations));
        }
        this.benchmarkResults = new ArrayList<>(benchmarkResults);
    }
}
