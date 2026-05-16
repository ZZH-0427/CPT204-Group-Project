
import graph.Graph;
import model.Location;
import sort.BubbleSort;
import sort.HeapSort;
import sort.MergeSort;
import sort.QuickSort;
import sort.Sorter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestRunner {

    public static void main(String[] args) {
        testSortingAlgorithms();
        testLocationTieBreaker();
        testDijkstraSelfPath();
        testDijkstraShortestPath();
        testDijkstraUnreachablePath();
        testFloydShortestPath();
        testFloydDpReordersViaNodes();
        testBfsUnweightedShortestPath();
        testBfsUnreachablePath();
        testPathWeightHelpers();
        System.out.println("All tests passed.");
    }

    private static void testSortingAlgorithms() {
        Sorter[] sorters = {new BubbleSort(), new QuickSort(), new MergeSort(), new HeapSort()};
        for (Sorter sorter : sorters) {
            List<Location> locations = new ArrayList<>(Arrays.asList(
                    new Location("L003", 10),
                    new Location("L001", 30),
                    new Location("L004", 20),
                    new Location("L002", 30)
            ));

            sorter.sort(locations);

            assertEquals("L001", locations.get(0).getLocationId(),
                    sorter.getName() + " should place highest priority first");
            assertEquals("L002", locations.get(1).getLocationId(),
                    sorter.getName() + " should break priority ties by location id");
            assertTrue(isSorted(locations), sorter.getName() + " output should be sorted");
        }
    }

    private static void testLocationTieBreaker() {
        Location first = new Location("L001", 50);
        Location second = new Location("L002", 50);
        assertTrue(first.compareTo(second) < 0,
                "Location tie-breaker should sort lower location id first");
    }

    private static void testDijkstraSelfPath() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 5);

        Graph.DijkstraResult result = graph.shortestPath("A", "A");

        assertTrue(result.isReachable(), "Self path should be reachable");
        assertEquals(0, result.totalCost, "Self path should have zero cost");
        assertEquals(Arrays.asList("A"), result.path, "Self path should contain only start node");
    }

    private static void testDijkstraShortestPath() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 10);
        graph.addEdge("A", "C", 2);
        graph.addEdge("C", "B", 3);

        Graph.DijkstraResult result = graph.shortestPath("A", "B");

        assertTrue(result.isReachable(), "A to B should be reachable");
        assertEquals(5, result.totalCost, "Dijkstra should choose cheaper indirect path");
        assertEquals(Arrays.asList("A", "C", "B"), result.path,
                "Dijkstra should return the shortest path nodes");
    }

    private static void testDijkstraUnreachablePath() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 1);
        graph.addEdge("C", "D", 1);

        Graph.DijkstraResult result = graph.shortestPath("A", "D");

        assertTrue(!result.isReachable(), "Disconnected nodes should be unreachable");
        assertEquals(-1, result.totalCost, "Unreachable path should have cost -1");
    }

    private static void testFloydShortestPath() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 10);
        graph.addEdge("A", "C", 2);
        graph.addEdge("C", "B", 3);

        Graph.DijkstraResult result = graph.shortestPathFloyd("A", "B");

        assertTrue(result.isReachable(), "Floyd should report reachable nodes");
        assertEquals(5, result.totalCost, "Floyd should choose cheaper indirect path");
        assertEquals(Arrays.asList("A", "C", "B"), result.path,
                "Floyd should reconstruct the shortest path nodes");
    }

    private static void testFloydDpReordersViaNodes() {
        Graph graph = new Graph();
        graph.addEdge("S", "A", 1);
        graph.addEdge("A", "B", 1);
        graph.addEdge("B", "T", 1);
        graph.addEdge("S", "B", 50);
        graph.addEdge("A", "T", 50);

        Graph.RoutePlanResult result = graph.shortestPathVisitingFloyd(
                "S", Arrays.asList("B", "A"), "T");

        assertTrue(result.isReachable(), "Floyd + DP route should be reachable");
        assertEquals(Arrays.asList("A", "B"), result.visitOrder,
                "DP should choose the cheapest via-node order");
        assertEquals(Arrays.asList("S", "A", "B", "T"), result.path,
                "Floyd + DP should reconstruct the complete route");
        assertEquals(3, result.totalCost, "Floyd + DP route should minimise total weight");
    }

    private static void testBfsUnweightedShortestPath() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 100);
        graph.addEdge("A", "C", 1);
        graph.addEdge("C", "D", 1);
        graph.addEdge("D", "B", 1);

        Graph.DijkstraResult weighted = graph.shortestPath("A", "B");
        Graph.DijkstraResult unweighted = graph.shortestPathUnweighted("A", "B");

        assertEquals(Arrays.asList("A", "C", "D", "B"), weighted.path,
                "Dijkstra should minimise total edge weight");
        assertEquals(3, weighted.totalCost, "Dijkstra weighted path should cost 3");
        assertEquals(Arrays.asList("A", "B"), unweighted.path,
                "BFS should minimise number of edges");
        assertEquals(1, unweighted.totalCost, "BFS path cost should be edge count");
    }

    private static void testBfsUnreachablePath() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 1);
        graph.addEdge("C", "D", 1);

        Graph.DijkstraResult result = graph.shortestPathUnweighted("A", "D");

        assertTrue(!result.isReachable(), "BFS should report disconnected nodes as unreachable");
        assertEquals(-1, result.totalCost, "Unreachable BFS path should have cost -1");
    }

    private static void testPathWeightHelpers() {
        Graph graph = new Graph();
        graph.addEdge("A", "B", 4);
        graph.addEdge("B", "C", 6);

        assertEquals(4, graph.edgeWeight("A", "B"),
                "edgeWeight should return the stored undirected edge cost");
        assertEquals(10, graph.pathWeight(Arrays.asList("A", "B", "C")),
                "pathWeight should sum consecutive edge weights");
        assertEquals(-1, graph.pathWeight(Arrays.asList("A", "C")),
                "pathWeight should report invalid paths");
    }

    private static boolean isSorted(List<Location> locations) {
        for (int i = 1; i < locations.size(); i++) {
            if (locations.get(i - 1).compareTo(locations.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " Expected: " + expected + ", actual: " + actual);
        }
    }
}
