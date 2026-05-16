package model;

import util.TimingStats;

public class BenchmarkResult {
    public final String dataset;
    public final String algorithm;
    public final double averageMs;
    public final double minMs;
    public final double maxMs;
    public final double stdDevMs;

    public BenchmarkResult(String dataset, String algorithm, TimingStats stats) {
        this.dataset = dataset;
        this.algorithm = algorithm;
        this.averageMs = stats.averageMs();
        this.minMs = stats.minMs();
        this.maxMs = stats.maxMs();
        this.stdDevMs = stats.standardDeviationMs();
    }
}
