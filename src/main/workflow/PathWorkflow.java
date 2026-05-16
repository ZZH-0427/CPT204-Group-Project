package workflow;

import graph.Graph;
import model.Location;
import model.PathCaseResult;
import model.PathComparisonResult;
import model.PathTaskResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PathWorkflow {

    public static PathTaskResult run(Graph graph, List<List<Location>> allTop10) {
        Location a1 = allTop10.get(0).get(0);
        Location a10 = allTop10.get(0).get(9);
        Location b1 = allTop10.get(1).get(0);
        Location b5 = allTop10.get(1).get(4);
        Location c1 = allTop10.get(2).get(0);
        Location c5 = allTop10.get(2).get(4);

        printSelectedTargets(a1, a10, b1, b5, c1, c5);

        List<PathCaseResult> pathCaseResults = new ArrayList<>();
        System.out.println("\nDijkstra required-order results:");
        pathCaseResults.add(runDijkstraCase("Case 1", graph,
                a1.getLocationId(), Collections.emptyList(), a1.getLocationId()));
        pathCaseResults.add(runDijkstraCase("Case 2", graph,
                a1.getLocationId(), Collections.emptyList(), a10.getLocationId()));
        pathCaseResults.add(runDijkstraCase("Case 3", graph,
                a1.getLocationId(), List.of(b5.getLocationId()), b1.getLocationId()));
        pathCaseResults.add(runDijkstraCase("Case 4", graph, a1.getLocationId(),
                List.of(b5.getLocationId(), c5.getLocationId()), c1.getLocationId()));

        System.out.println("\nFloyd + DP results:");
        long floydStart = System.nanoTime();
        pathCaseResults.add(runFloydDpCase("Case 1", graph,
                a1.getLocationId(), Collections.emptyList(), a1.getLocationId()));
        pathCaseResults.add(runFloydDpCase("Case 2", graph,
                a1.getLocationId(), Collections.emptyList(), a10.getLocationId()));
        pathCaseResults.add(runFloydDpCase("Case 3", graph,
                a1.getLocationId(), List.of(b5.getLocationId()), b1.getLocationId()));
        pathCaseResults.add(runFloydDpCase("Case 4", graph, a1.getLocationId(),
                List.of(b5.getLocationId(), c5.getLocationId()), c1.getLocationId()));
        double floydElapsedMs = (System.nanoTime() - floydStart) / 1_000_000.0;
        System.out.printf(Locale.US, "Floyd + DP queries completed in %.3f ms "
                + "(first query includes all-pairs preprocessing).%n", floydElapsedMs);

        PathComparisonResult comparison = runPathComparison(graph,
                a1.getLocationId(), a10.getLocationId());
        return new PathTaskResult(pathCaseResults, comparison);
    }

    private static PathCaseResult runDijkstraCase(String caseName, Graph graph,
                                                  String start, List<String> viaNodes, String end) {
        System.out.println("\n--- " + caseName + ": " + start
                + routeLabel(viaNodes, end) + " ---");

        List<String> targets = new ArrayList<>(viaNodes);
        targets.add(end);

        List<String> fullPath = new ArrayList<>();
        List<Integer> legCosts = new ArrayList<>();
        int totalCost = 0;
        String current = start;
        fullPath.add(start);

        for (String target : targets) {
            Graph.DijkstraResult leg = graph.shortestPath(current, target);
            if (!leg.isReachable()) {
                System.out.println("  No path found from " + current + " to " + target);
                return PathCaseResult.unreachable(caseName, "Dijkstra", start, end,
                        viaNodes, viaNodes);
            }

            for (int i = 1; i < leg.path.size(); i++) {
                fullPath.add(leg.path.get(i));
            }
            if (leg.totalCost > 0 || !current.equals(target)) {
                legCosts.add(leg.totalCost);
            }
            totalCost += leg.totalCost;
            current = target;
        }

        printPathResult(fullPath, legCosts, totalCost);
        return PathCaseResult.reachable(caseName, "Dijkstra", start, end,
                viaNodes, viaNodes, fullPath, totalCost, legCosts);
    }

    private static PathCaseResult runFloydDpCase(String caseName, Graph graph,
                                                 String start, List<String> viaNodes, String end) {
        System.out.println("\n--- " + caseName + ": " + start
                + routeLabel(viaNodes, end) + " ---");

        Graph.RoutePlanResult result = graph.shortestPathVisitingFloyd(start, viaNodes, end);
        if (!result.isReachable()) {
            System.out.println("  No path found.");
            return PathCaseResult.unreachable(caseName, "Floyd + DP", start, end,
                    viaNodes, result.visitOrder);
        }

        System.out.println("  Visit order: "
                + (result.visitOrder.isEmpty() ? "none" : String.join(" -> ", result.visitOrder)));
        printPathResult(result.path, result.legCosts, result.totalCost);
        return PathCaseResult.reachable(caseName, "Floyd + DP", start, end,
                viaNodes, result.visitOrder, result.path, result.totalCost, result.legCosts);
    }

    private static PathComparisonResult runPathComparison(Graph graph, String start, String end) {
        System.out.println("\n--- Path Comparison: " + start + " -> " + end
                + " (Dijkstra vs Floyd + DP vs BFS) ---");
        Graph.DijkstraResult weighted = graph.shortestPath(start, end);
        Graph.DijkstraResult floyd = graph.shortestPathFloyd(start, end);
        Graph.DijkstraResult unweighted = graph.shortestPathUnweighted(start, end);

        if (!weighted.isReachable() || !floyd.isReachable() || !unweighted.isReachable()) {
            System.out.println("  No comparison available because one path is unreachable.");
            return new PathComparisonResult(start, end, weighted, floyd, unweighted);
        }

        System.out.println("  Dijkstra weighted cost: " + weighted.totalCost);
        System.out.println("  Dijkstra path: " + String.join(" -> ", weighted.path));
        System.out.println("  Floyd + DP weighted cost: " + floyd.totalCost);
        System.out.println("  Floyd + DP path: " + String.join(" -> ", floyd.path));
        System.out.println("  BFS edge count: " + unweighted.totalCost);
        System.out.println("  BFS path: " + String.join(" -> ", unweighted.path));
        System.out.println("  Interpretation: Dijkstra and Floyd + DP minimise total path weight, "
                + "while BFS minimises number of edges.");

        return new PathComparisonResult(start, end, weighted, floyd, unweighted);
    }

    private static void printSelectedTargets(Location a1, Location a10,
                                             Location b1, Location b5,
                                             Location c1, Location c5) {
        System.out.println("\nSelected targets for Task B:");
        System.out.println("  A[1]  = " + a1);
        System.out.println("  A[10] = " + a10);
        System.out.println("  B[1]  = " + b1);
        System.out.println("  B[5]  = " + b5);
        System.out.println("  C[1]  = " + c1);
        System.out.println("  C[5]  = " + c5);
    }

    private static void printPathResult(List<String> path, List<Integer> legCosts, int totalCost) {
        System.out.println("  Path: " + String.join(" -> ", path));
        System.out.println("  Leg costs: " + (legCosts.isEmpty()
                ? "none" : joinIntegers(legCosts, " + ")));
        System.out.println("  Total cost: " + totalCost);
    }

    private static String routeLabel(List<String> viaNodes, String end) {
        if (viaNodes.isEmpty()) {
            return " -> " + end;
        }
        return " -> " + String.join(" -> ", viaNodes) + " -> " + end;
    }

    private static String joinIntegers(List<Integer> values, String delimiter) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                joined.append(delimiter);
            }
            joined.append(values.get(i));
        }
        return joined.toString();
    }
}
