package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathCaseResult {
    public final String caseName;
    public final String algorithm;
    public final String start;
    public final String end;
    public final List<String> viaNodes;
    public final List<String> visitOrder;
    public final boolean reachable;
    public final int totalCost;
    public final List<Integer> legCosts;
    public final List<String> path;

    private PathCaseResult(String caseName, String algorithm, String start, String end,
                           List<String> viaNodes, List<String> visitOrder,
                           boolean reachable, int totalCost,
                           List<Integer> legCosts, List<String> path) {
        this.caseName = caseName;
        this.algorithm = algorithm;
        this.start = start;
        this.end = end;
        this.viaNodes = new ArrayList<>(viaNodes);
        this.visitOrder = new ArrayList<>(visitOrder);
        this.reachable = reachable;
        this.totalCost = totalCost;
        this.legCosts = new ArrayList<>(legCosts);
        this.path = new ArrayList<>(path);
    }

    public static PathCaseResult reachable(String caseName, String algorithm,
                                           String start, String end,
                                           List<String> viaNodes, List<String> visitOrder,
                                           List<String> path, int totalCost,
                                           List<Integer> legCosts) {
        return new PathCaseResult(caseName, algorithm, start, end, viaNodes, visitOrder,
                true, totalCost, legCosts, path);
    }

    public static PathCaseResult unreachable(String caseName, String algorithm,
                                             String start, String end,
                                             List<String> viaNodes, List<String> visitOrder) {
        return new PathCaseResult(caseName, algorithm, start, end, viaNodes, visitOrder,
                false, -1, Collections.emptyList(), Collections.emptyList());
    }
}
