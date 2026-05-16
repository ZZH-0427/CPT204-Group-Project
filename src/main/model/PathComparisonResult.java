package model;

import graph.Graph;

public class PathComparisonResult {
    public final String start;
    public final String end;
    public final Graph.DijkstraResult weighted;
    public final Graph.DijkstraResult floyd;
    public final Graph.DijkstraResult unweighted;

    public PathComparisonResult(String start, String end,
                                Graph.DijkstraResult weighted,
                                Graph.DijkstraResult floyd,
                                Graph.DijkstraResult unweighted) {
        this.start = start;
        this.end = end;
        this.weighted = weighted;
        this.floyd = floyd;
        this.unweighted = unweighted;
    }
}
