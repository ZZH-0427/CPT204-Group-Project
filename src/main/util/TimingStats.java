package util;

public class TimingStats {
    private long totalNano;
    private double totalSquaredNano;
    private long minNano = Long.MAX_VALUE;
    private long maxNano = Long.MIN_VALUE;
    private int count;

    public void add(long nano) {
        totalNano += nano;
        totalSquaredNano += (double) nano * nano;
        minNano = Math.min(minNano, nano);
        maxNano = Math.max(maxNano, nano);
        count++;
    }

    public double averageMs() {
        return (totalNano / (double) count) / 1_000_000.0;
    }

    public double minMs() {
        return minNano / 1_000_000.0;
    }

    public double maxMs() {
        return maxNano / 1_000_000.0;
    }

    public double standardDeviationMs() {
        double mean = totalNano / (double) count;
        double variance = totalSquaredNano / count - mean * mean;
        return Math.sqrt(Math.max(0, variance)) / 1_000_000.0;
    }
}
