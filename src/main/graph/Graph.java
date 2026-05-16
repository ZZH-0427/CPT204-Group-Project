package graph;

import model.Edge;
import java.util.*;

public class Graph {
    private static final int INF = Integer.MAX_VALUE / 4;

    private final Map<String, List<Edge>> adjacencyList;
    private FloydCache floydCache;
    private boolean floydDirty;

    public Graph() {
        this.adjacencyList = new HashMap<>();
        this.floydDirty = true;
    }

    public void addEdge(String from, String to, int weight) {
        adjacencyList.computeIfAbsent(from, k -> new ArrayList<>())
                .add(new Edge(from, to, weight));
        adjacencyList.computeIfAbsent(to, k -> new ArrayList<>())
                .add(new Edge(to, from, weight));
        floydDirty = true;
    }

    public void addEdges(List<Edge> edges) {
        for (Edge e : edges) {
            addEdge(e.getFrom(), e.getTo(), e.getWeight());
        }
    }

    public int nodeCount() {
        return adjacencyList.size();
    }

    public int edgeCount() {
        int count = 0;
        for (List<Edge> edges : adjacencyList.values()) {
            count += edges.size();
        }
        return count / 2; // undirected, each edge stored twice
    }

    public boolean containsNode(String nodeId) {
        return adjacencyList.containsKey(nodeId);
    }

    public int edgeWeight(String from, String to) {
        for (Edge edge : adjacencyList.getOrDefault(from, Collections.emptyList())) {
            if (edge.getTo().equals(to)) {
                return edge.getWeight();
            }
        }
        return -1;
    }

    public List<Edge> neighbors(String nodeId) {
        return new ArrayList<>(adjacencyList.getOrDefault(nodeId, Collections.emptyList()));
    }

    public int pathWeight(List<String> path) {
        if (path.size() < 2) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int weight = edgeWeight(path.get(i), path.get(i + 1));
            if (weight < 0) {
                return -1;
            }
            total += weight;
        }
        return total;
    }

    /**
     * Dijkstra's algorithm returning shortest distance and path.
     */
    public DijkstraResult shortestPath(String start, String end) {
        if (!adjacencyList.containsKey(start) || !adjacencyList.containsKey(end)) {
            return new DijkstraResult(Collections.emptyList(), -1);
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<NodeDist> pq = new PriorityQueue<>();

        for (String node : adjacencyList.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }
        dist.put(start, 0);
        pq.offer(new NodeDist(start, 0));

        while (!pq.isEmpty()) {
            NodeDist current = pq.poll();
            String u = current.nodeId;

            if (current.distance > dist.get(u)) {
                continue;
            }

            if (u.equals(end)) {
                break;
            }

            for (Edge edge : adjacencyList.getOrDefault(u, Collections.emptyList())) {
                String v = edge.getTo();
                int alt = dist.get(u) + edge.getWeight();
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.offer(new NodeDist(v, alt));
                }
            }
        }

        if (dist.get(end) == Integer.MAX_VALUE) {
            return new DijkstraResult(Collections.emptyList(), -1);
        }

        List<String> path = reconstructPath(prev, start, end);
        return new DijkstraResult(path, dist.get(end));
    }

    /**
     * Breadth-first search treating every edge as cost 1.
     * The returned totalCost is the number of edges in the path.
     */
    public DijkstraResult shortestPathUnweighted(String start, String end) {
        if (!adjacencyList.containsKey(start) || !adjacencyList.containsKey(end)) {
            return new DijkstraResult(Collections.emptyList(), -1);
        }

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, Integer> edgeCount = new HashMap<>();

        queue.offer(start);
        visited.add(start);
        edgeCount.put(start, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(end)) {
                break;
            }

            for (Edge edge : adjacencyList.getOrDefault(current, Collections.emptyList())) {
                String next = edge.getTo();
                if (!visited.contains(next)) {
                    visited.add(next);
                    prev.put(next, current);
                    edgeCount.put(next, edgeCount.get(current) + 1);
                    queue.offer(next);
                }
            }
        }

        if (!visited.contains(end)) {
            return new DijkstraResult(Collections.emptyList(), -1);
        }

        return new DijkstraResult(reconstructPath(prev, start, end), edgeCount.get(end));
    }

    /**
     * Floyd-Warshall all-pairs shortest path query.
     *
     * The matrix is built lazily and reused until the graph changes. This is
     * useful when many weighted path queries are needed on the same graph.
     */
    public DijkstraResult shortestPathFloyd(String start, String end) {
        FloydCache cache = ensureFloydCache();
        Integer startIndex = cache.indexByNode.get(start);
        Integer endIndex = cache.indexByNode.get(end);
        if (startIndex == null || endIndex == null) {
            return new DijkstraResult(Collections.emptyList(), -1);
        }

        if (cache.dist[startIndex][endIndex] >= INF
                || cache.next[startIndex][endIndex] < 0) {
            return new DijkstraResult(Collections.emptyList(), -1);
        }

        return new DijkstraResult(
                reconstructFloydPath(cache, startIndex, endIndex),
                cache.dist[startIndex][endIndex]);
    }

    /**
     * Uses Floyd-Warshall distances inside a bitmask dynamic program to find
     * the cheapest order for visiting all required via nodes.
     */
    public RoutePlanResult shortestPathVisitingFloyd(String start, List<String> viaNodes, String end) {
        if (viaNodes.isEmpty()) {
            DijkstraResult direct = shortestPathFloyd(start, end);
            if (!direct.isReachable()) {
                return RoutePlanResult.unreachable(start, end, viaNodes);
            }
            return new RoutePlanResult(direct.path, direct.totalCost,
                    direct.totalCost == 0 ? Collections.emptyList() : Collections.singletonList(direct.totalCost),
                    Collections.emptyList());
        }

        FloydCache cache = ensureFloydCache();
        Integer startIndex = cache.indexByNode.get(start);
        Integer endIndex = cache.indexByNode.get(end);
        if (startIndex == null || endIndex == null) {
            return RoutePlanResult.unreachable(start, end, viaNodes);
        }

        int viaCount = viaNodes.size();
        int[] viaIndexes = new int[viaCount];
        for (int i = 0; i < viaCount; i++) {
            Integer viaIndex = cache.indexByNode.get(viaNodes.get(i));
            if (viaIndex == null) {
                return RoutePlanResult.unreachable(start, end, viaNodes);
            }
            viaIndexes[i] = viaIndex;
        }

        int stateCount = 1 << viaCount;
        int[][] dp = new int[stateCount][viaCount];
        int[][] previous = new int[stateCount][viaCount];
        for (int mask = 0; mask < stateCount; mask++) {
            Arrays.fill(dp[mask], INF);
            Arrays.fill(previous[mask], -1);
        }

        for (int i = 0; i < viaCount; i++) {
            int cost = cache.dist[startIndex][viaIndexes[i]];
            if (cost < INF) {
                dp[1 << i][i] = cost;
            }
        }

        for (int mask = 0; mask < stateCount; mask++) {
            for (int last = 0; last < viaCount; last++) {
                if (dp[mask][last] >= INF) {
                    continue;
                }
                for (int next = 0; next < viaCount; next++) {
                    if ((mask & (1 << next)) != 0) {
                        continue;
                    }
                    int legCost = cache.dist[viaIndexes[last]][viaIndexes[next]];
                    if (legCost >= INF) {
                        continue;
                    }
                    int nextMask = mask | (1 << next);
                    int candidate = dp[mask][last] + legCost;
                    if (candidate < dp[nextMask][next]) {
                        dp[nextMask][next] = candidate;
                        previous[nextMask][next] = last;
                    }
                }
            }
        }

        int fullMask = stateCount - 1;
        int bestCost = INF;
        int bestLast = -1;
        for (int last = 0; last < viaCount; last++) {
            int endLeg = cache.dist[viaIndexes[last]][endIndex];
            if (dp[fullMask][last] >= INF || endLeg >= INF) {
                continue;
            }
            int candidate = dp[fullMask][last] + endLeg;
            if (candidate < bestCost) {
                bestCost = candidate;
                bestLast = last;
            }
        }

        if (bestLast < 0) {
            return RoutePlanResult.unreachable(start, end, viaNodes);
        }

        List<Integer> orderIndexes = new ArrayList<>();
        int mask = fullMask;
        int last = bestLast;
        while (last >= 0) {
            orderIndexes.add(last);
            int oldLast = last;
            last = previous[mask][last];
            mask &= ~(1 << oldLast);
        }
        Collections.reverse(orderIndexes);

        List<String> visitOrder = new ArrayList<>();
        for (int index : orderIndexes) {
            visitOrder.add(viaNodes.get(index));
        }

        return buildFloydRouteFromOrder(start, visitOrder, end);
    }

    private RoutePlanResult buildFloydRouteFromOrder(String start, List<String> visitOrder, String end) {
        List<String> targets = new ArrayList<>(visitOrder);
        targets.add(end);

        List<String> fullPath = new ArrayList<>();
        List<Integer> legCosts = new ArrayList<>();
        int totalCost = 0;
        String current = start;
        fullPath.add(start);

        for (String target : targets) {
            DijkstraResult leg = shortestPathFloyd(current, target);
            if (!leg.isReachable()) {
                return RoutePlanResult.unreachable(start, end, visitOrder);
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

        return new RoutePlanResult(fullPath, totalCost, legCosts, visitOrder);
    }

    private FloydCache ensureFloydCache() {
        if (!floydDirty && floydCache != null) {
            return floydCache;
        }

        List<String> nodeIds = new ArrayList<>(adjacencyList.keySet());
        Collections.sort(nodeIds);
        Map<String, Integer> indexByNode = new HashMap<>();
        for (int i = 0; i < nodeIds.size(); i++) {
            indexByNode.put(nodeIds.get(i), i);
        }

        int size = nodeIds.size();
        int[][] dist = new int[size][size];
        int[][] next = new int[size][size];
        for (int i = 0; i < size; i++) {
            Arrays.fill(dist[i], INF);
            Arrays.fill(next[i], -1);
            dist[i][i] = 0;
            next[i][i] = i;
        }

        for (String from : nodeIds) {
            int fromIndex = indexByNode.get(from);
            for (Edge edge : adjacencyList.getOrDefault(from, Collections.emptyList())) {
                Integer toIndex = indexByNode.get(edge.getTo());
                if (toIndex != null && edge.getWeight() < dist[fromIndex][toIndex]) {
                    dist[fromIndex][toIndex] = edge.getWeight();
                    next[fromIndex][toIndex] = toIndex;
                }
            }
        }

        for (int k = 0; k < size; k++) {
            for (int i = 0; i < size; i++) {
                if (dist[i][k] >= INF) {
                    continue;
                }
                for (int j = 0; j < size; j++) {
                    if (dist[k][j] >= INF) {
                        continue;
                    }
                    int candidate = dist[i][k] + dist[k][j];
                    if (candidate < dist[i][j]) {
                        dist[i][j] = candidate;
                        next[i][j] = next[i][k];
                    }
                }
            }
        }

        floydCache = new FloydCache(nodeIds, indexByNode, dist, next);
        floydDirty = false;
        return floydCache;
    }

    private List<String> reconstructFloydPath(FloydCache cache, int startIndex, int endIndex) {
        List<String> path = new ArrayList<>();
        int current = startIndex;
        path.add(cache.nodeIds.get(current));

        int guard = 0;
        while (current != endIndex) {
            current = cache.next[current][endIndex];
            if (current < 0 || guard++ > cache.nodeIds.size()) {
                return Collections.emptyList();
            }
            path.add(cache.nodeIds.get(current));
        }
        return path;
    }

    private List<String> reconstructPath(Map<String, String> prev, String start, String end) {
        List<String> path = new ArrayList<>();
        String current = end;
        while (current != null) {
            path.add(current);
            current = prev.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private static class NodeDist implements Comparable<NodeDist> {
        final String nodeId;
        final int distance;

        NodeDist(String nodeId, int distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDist other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    private static class FloydCache {
        final List<String> nodeIds;
        final Map<String, Integer> indexByNode;
        final int[][] dist;
        final int[][] next;

        FloydCache(List<String> nodeIds, Map<String, Integer> indexByNode,
                   int[][] dist, int[][] next) {
            this.nodeIds = nodeIds;
            this.indexByNode = indexByNode;
            this.dist = dist;
            this.next = next;
        }
    }

    public static class DijkstraResult {
        public final List<String> path;
        public final int totalCost;

        DijkstraResult(List<String> path, int totalCost) {
            this.path = path;
            this.totalCost = totalCost;
        }

        public boolean isReachable() {
            return totalCost >= 0;
        }
    }

    public static class RoutePlanResult {
        public final List<String> path;
        public final int totalCost;
        public final List<Integer> legCosts;
        public final List<String> visitOrder;

        RoutePlanResult(List<String> path, int totalCost,
                        List<Integer> legCosts, List<String> visitOrder) {
            this.path = new ArrayList<>(path);
            this.totalCost = totalCost;
            this.legCosts = new ArrayList<>(legCosts);
            this.visitOrder = new ArrayList<>(visitOrder);
        }

        static RoutePlanResult unreachable(String start, String end, List<String> viaNodes) {
            return new RoutePlanResult(Collections.emptyList(), -1,
                    Collections.emptyList(), viaNodes);
        }

        public boolean isReachable() {
            return totalCost >= 0;
        }
    }
}
