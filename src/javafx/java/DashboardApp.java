
import graph.Graph;
import io.CSVReader;
import model.Edge;
import model.Location;
import sort.BubbleSort;
import sort.HeapSort;
import sort.MergeSort;
import sort.QuickSort;
import sort.Sorter;
import util.TimingStats;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class DashboardApp extends Application {

    private static final int TOP_N = 10;
    private static final int MEASURED_RUNS = 12;
    private static final int SORT_MAX_FRAMES = 520;
    private static final Path OUTPUT_DIR = Paths.get("out");

    private final StackPane content = new StackPane();
    private final Label status = new Label();
    private final List<Button> navigationButtons = new ArrayList<>();

    private Path dataDir;
    private List<List<Location>> originalDatasets;
    private List<List<Location>> allTop10;
    private List<BenchmarkRow> benchmarkRows;
    private List<PathCase> pathCases;
    private BfsComparison bfsComparison;
    private Graph graph;
    private SequentialTransition routeAnimation;
    private Timeline processAnimation;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            dataDir = resolveDataDir(getParameters().getRaw());
            loadResults();
        } catch (Exception e) {
            showStartupError(stage, e);
            return;
        }

        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(content);
        root.setBottom(buildStatusBar());
        root.getStyleClass().add("root-shell");

        Scene scene = new Scene(root, 1220, 760);
        scene.getStylesheets().add(DashboardApp.class
                .getResource("/dashboard.css")
                .toExternalForm());

        stage.setTitle("CPT204 Algorithm Visual Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        stage.show();

        showOverview();
    }

    private VBox buildSidebar() {
        Label title = new Label("CPT204 Demo");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Sorting + shortest path visualisation");
        subtitle.getStyleClass().add("muted");
        subtitle.setWrapText(true);

        Button overview = navButton("Overview", this::showOverview);
        Button sorting = navButton("Sorting Benchmark", this::showSortingBenchmark);
        Button targets = navButton("Top 10 Targets", this::showTopTargets);
        Button paths = navButton("Shortest Paths", this::showPathCases);
        Button bfs = navButton("Path Comparison", this::showBfsComparison);

        Button regenerate = new Button("Regenerate Artifacts");
        regenerate.setMaxWidth(Double.MAX_VALUE);
        regenerate.setOnAction(event -> regenerateArtifacts());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox box = new VBox(12, title, subtitle, overview, sorting, targets, paths, bfs,
                spacer, regenerate);
        box.getStyleClass().add("sidebar");
        return box;
    }

    private HBox buildStatusBar() {
        status.setText("Data: " + dataDir.toAbsolutePath().normalize());
        status.getStyleClass().add("status-text");
        HBox bar = new HBox(status);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private Button navButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.setOnAction(event -> {
            stopRouteAnimation();
            action.run();
            setActive(button);
        });
        navigationButtons.add(button);
        return button;
    }

    private void setActive(Button active) {
        for (Button button : navigationButtons) {
            button.getStyleClass().remove("active");
        }
        if (active != null && !active.getStyleClass().contains("active")) {
            active.getStyleClass().add("active");
        }
    }

    private void showOverview() {
        VBox metrics = new VBox(14,
                metric("1. Load Data", "3 candidate files + " + graph.edgeCount() + " weighted paths"),
                metric("2. Rank Candidates", "Bubble, Quick, Merge, and Heap Sort are benchmarked"),
                metric("3. Build Graph", graph.nodeCount() + " locations become weighted graph nodes"),
                metric("4. Explore Routes", "Dijkstra, Floyd + DP, and BFS can be compared interactively")
        );
        metrics.getStyleClass().add("metric-grid");

        TextArea script = new TextArea(
                "Suggested talking flow:\n"
                        + "1. Use Sorting Benchmark to compare all algorithms, then switch to fast algorithms only.\n"
                        + "2. Open Top 10 Targets and identify how route endpoints are selected.\n"
                        + "3. Use Interactive Path Explorer: choose a case, choose Dijkstra, Floyd + DP, or BFS, then show/animate the route.\n"
                        + "4. Finish with Path Comparison to compare Dijkstra, Floyd + DP, and BFS.");
        script.setEditable(false);
        script.setWrapText(true);
        script.getStyleClass().add("script-box");

        VBox page = page("One-click Demonstration Dashboard",
                "The buttons on the left switch between the exact artefacts needed for the coursework presentation.",
                metrics, script);
        setContent(page, "Overview loaded.");
    }

    private void showSortingBenchmark() {
        StackPane chartHost = new StackPane();
        TableView<BenchmarkRow> table = benchmarkTable();
        table.setPrefHeight(220);

        ToggleGroup viewGroup = new ToggleGroup();
        RadioButton allAlgorithms = new RadioButton("All algorithms");
        allAlgorithms.setToggleGroup(viewGroup);
        allAlgorithms.setSelected(true);
        RadioButton fastOnly = new RadioButton("Fast algorithms only");
        fastOnly.setToggleGroup(viewGroup);

        HBox controls = new HBox(14, allAlgorithms, fastOnly);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("path-controls");

        TextArea conclusion = new TextArea();
        conclusion.setEditable(false);
        conclusion.setWrapText(true);
        conclusion.setPrefRowCount(4);
        conclusion.getStyleClass().add("script-box");

        Runnable refresh = () -> {
            boolean includeBubble = allAlgorithms.isSelected();
            chartHost.getChildren().setAll(buildSortingBenchmarkPanel(includeBubble));
            table.getItems().setAll(filteredBenchmarkRows(includeBubble));
            conclusion.setText(buildSortingConclusion(includeBubble));
        };
        allAlgorithms.setOnAction(event -> refresh.run());
        fastOnly.setOnAction(event -> refresh.run());
        refresh.run();

        VBox processDemo = buildSortingProcessDemo();

        VBox page = page("Task A: Sorting Benchmark",
                "Grouped bars show average runtime in milliseconds; exact values are listed below.",
                controls, chartHost, conclusion, processDemo, table);
        setContent(page, "Sorting benchmark ready.");
    }

    private Pane buildSortingBenchmarkPanel(boolean includeBubble) {
        String[] datasets = {"Dataset A", "Dataset B", "Dataset C"};
        String[] algorithms = includeBubble
                ? new String[]{"Bubble Sort", "Quick Sort", "Merge Sort", "Heap Sort"}
                : new String[]{"Quick Sort", "Merge Sort", "Heap Sort"};
        String[] colors = includeBubble
                ? new String[]{"#f25a2a", "#f59e0b", "#4caf50", "#3aa6c5"}
                : new String[]{"#f59e0b", "#4caf50", "#3aa6c5"};
        double width = 920;
        double height = 430;
        double left = 82;
        double top = 42;
        double chartWidth = 760;
        double chartHeight = 250;
        double baseY = top + chartHeight;

        double maxValue = 0;
        for (BenchmarkRow row : benchmarkRows) {
            if (contains(algorithms, row.algorithm)) {
                maxValue = Math.max(maxValue, row.averageMs);
            }
        }
        double axisMax = Math.max(0.1, Math.ceil(maxValue * 1.18 * 1000.0) / 1000.0);

        Pane pane = new Pane();
        pane.setMinSize(width, height);
        pane.setPrefSize(width, height);
        pane.getStyleClass().add("custom-chart");

        addChartLabel(pane,
                includeBubble ? "Sorting Algorithm Runtime Comparison" : "Fast Sorting Algorithms Comparison",
                width / 2 - 170, 8, "chart-title-label");
        addChartLabel(pane, "Average runtime (ms)", 8, top + 92, "axis-title-label rotated-axis-label");

        Line xAxis = new Line(left, baseY, left + chartWidth, baseY);
        xAxis.getStyleClass().add("chart-axis");
        Line yAxis = new Line(left, top, left, baseY);
        yAxis.getStyleClass().add("chart-axis");
        pane.getChildren().addAll(xAxis, yAxis);

        for (int tick = 0; tick <= 5; tick++) {
            double value = axisMax * tick / 5.0;
            double y = baseY - value / axisMax * chartHeight;
            Line grid = new Line(left, y, left + chartWidth, y);
            grid.getStyleClass().add("chart-grid-line");
            addChartLabel(pane, formatRuntime(value), 28, y - 8, "axis-tick-label");
            pane.getChildren().add(grid);
        }

        double groupWidth = chartWidth / datasets.length;
        double barWidth = 34;
        double barGap = 10;
        double totalGroupBars = algorithms.length * barWidth + (algorithms.length - 1) * barGap;

        for (int datasetIndex = 0; datasetIndex < datasets.length; datasetIndex++) {
            double groupLeft = left + datasetIndex * groupWidth + (groupWidth - totalGroupBars) / 2.0;
            for (int algorithmIndex = 0; algorithmIndex < algorithms.length; algorithmIndex++) {
                BenchmarkRow row = findBenchmarkRow(datasets[datasetIndex], algorithms[algorithmIndex]);
                if (row == null) {
                    continue;
                }

                double barHeight = Math.max(2, row.averageMs / axisMax * chartHeight);
                double x = groupLeft + algorithmIndex * (barWidth + barGap);
                double y = baseY - barHeight;
                Rectangle bar = new Rectangle(x, y, barWidth, barHeight);
                bar.setStyle("-fx-fill: " + colors[algorithmIndex] + ";");
                bar.getStyleClass().add("custom-bar");
                pane.getChildren().add(bar);

                if (barHeight > 26) {
                    addChartLabel(pane, formatRuntime(row.averageMs), x - 5, y - 20, "bar-value-label");
                }
            }
            addChartLabel(pane, datasets[datasetIndex],
                    left + datasetIndex * groupWidth + groupWidth / 2.0 - 36,
                    baseY + 22, "dataset-label");
        }

        double legendY = height - 70;
        double legendX = 162;
        for (int i = 0; i < algorithms.length; i++) {
            double x = legendX + i * 172;
            Rectangle swatch = new Rectangle(x, legendY, 16, 16);
            swatch.setStyle("-fx-fill: " + colors[i] + ";");
            addChartLabel(pane, algorithms[i], x + 24, legendY - 1, "legend-text-label");
            pane.getChildren().add(swatch);
        }

        addChartLabel(pane, "Dataset", left + chartWidth / 2.0 - 28, baseY + 54, "axis-title-label");
        return pane;
    }

    private VBox buildSortingProcessDemo() {
        Label title = new Label("Sorting Process Demo");
        title.getStyleClass().add("panel-title");
        Label subtitle = new Label("Every thin line is one real candidate score from the selected dataset. Higher priority should move left.");
        subtitle.getStyleClass().add("comparison-subtitle");
        subtitle.setWrapText(true);

        ComboBox<String> datasetBox = new ComboBox<>();
        datasetBox.getItems().addAll("Dataset A", "Dataset B", "Dataset C");
        datasetBox.getSelectionModel().selectFirst();
        datasetBox.getStyleClass().add("case-selector");

        ComboBox<String> algorithmBox = new ComboBox<>();
        algorithmBox.getItems().addAll("Bubble Sort", "Quick Sort", "Merge Sort", "Heap Sort");
        algorithmBox.getSelectionModel().selectFirst();
        algorithmBox.getStyleClass().add("case-selector");

        Button play = new Button("Play Sorting");
        play.getStyleClass().add("primary-button");
        Button reset = new Button("Reset");
        reset.getStyleClass().add("case-button");

        FlowPane controls = new FlowPane(12, 10,
                labelledControl("Dataset sample", datasetBox),
                labelledControl("Algorithm", algorithmBox),
                play,
                reset);
        controls.setAlignment(Pos.CENTER_LEFT);

        Pane bars = new Pane();
        bars.getStyleClass().add("sort-process-pane");
        bars.setMinSize(760, 320);
        bars.setPrefSize(900, 320);

        Label step = new Label();
        step.getStyleClass().add("process-step-label");
        step.setWrapText(true);

        Runnable renderInitial = () -> {
            List<SortFrame> frames = buildSortFrames(datasetBox.getValue(), algorithmBox.getValue());
            renderSortFrame(bars, step, frames.get(0), 0, frames.size());
        };
        datasetBox.setOnAction(event -> renderInitial.run());
        algorithmBox.setOnAction(event -> renderInitial.run());
        reset.setOnAction(event -> {
            stopRouteAnimation();
            renderInitial.run();
        });
        play.setOnAction(event -> playSortingDemo(bars, step, datasetBox.getValue(), algorithmBox.getValue()));
        renderInitial.run();

        VBox panel = new VBox(10, title, subtitle, controls, bars, step);
        panel.getStyleClass().add("process-panel");
        return panel;
    }

    private void playSortingDemo(Pane bars, Label step, String dataset, String algorithm) {
        stopRouteAnimation();
        List<SortFrame> frames = buildSortFrames(dataset, algorithm);
        processAnimation = new Timeline();
        for (int i = 0; i < frames.size(); i++) {
            final int index = i;
            processAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(index * 18L), event ->
                    renderSortFrame(bars, step, frames.get(index), index, frames.size())));
        }
        processAnimation.play();
    }

    private List<SortFrame> buildSortFrames(String dataset, String algorithm) {
        int[] values = demoPriorityValues(dataset);
        SortTrace trace = new SortTrace(sortFrameInterval(algorithm, values.length));
        trace.addAlways(values, -1, -1, values.length,
                "Initial data from " + dataset + ": every thin line is one candidate priority score.");
        if ("Quick Sort".equals(algorithm)) {
            quickTrace(values.clone(), 0, values.length - 1, trace);
        } else if ("Merge Sort".equals(algorithm)) {
            mergeTrace(values.clone(), 0, values.length - 1, trace);
        } else if ("Heap Sort".equals(algorithm)) {
            heapTrace(values.clone(), trace);
        } else {
            bubbleTrace(values.clone(), trace);
        }
        SortFrame last = trace.lastFrame();
        trace.addAlways(last.values, -1, -1, 0,
                algorithm + " complete: bars are ordered from high priority to low priority.");
        return trace.frames();
    }

    private int sortFrameInterval(String algorithm, int valueCount) {
        if ("Bubble Sort".equals(algorithm)) {
            return Math.max(1, valueCount * 3);
        }
        if ("Quick Sort".equals(algorithm)) {
            return Math.max(1, valueCount / 2);
        }
        if ("Merge Sort".equals(algorithm)) {
            return Math.max(1, valueCount / 12);
        }
        return Math.max(1, valueCount / 8);
    }

    private int[] demoPriorityValues(String dataset) {
        int datasetIndex = Math.max(0, Math.min(2, dataset.charAt(dataset.length() - 1) - 'A'));
        List<Location> locations = originalDatasets.get(datasetIndex);
        int sampleSize = locations.size();
        int[] values = new int[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            values[i] = locations.get(i).getPriorityScore();
        }
        return values;
    }

    private void bubbleTrace(int[] values, SortTrace trace) {
        int n = values.length;
        for (int pass = 0; pass < n - 1; pass++) {
            boolean swapped = false;
            for (int i = 0; i < n - 1 - pass; i++) {
                trace.add(values, i, i + 1, n - pass,
                        "Bubble: compare adjacent bars " + (i + 1) + " and " + (i + 2) + ".");
                if (values[i] < values[i + 1]) {
                    swap(values, i, i + 1);
                    swapped = true;
                    trace.add(values, i, i + 1, n - pass,
                            "Swap because the right bar has higher priority.");
                }
            }
            trace.add(values, -1, -1, n - pass - 1,
                    "End of pass " + (pass + 1) + ": the lowest remaining priority is fixed on the right.");
            if (!swapped) {
                break;
            }
        }
    }

    private void quickTrace(int[] values, int low, int high, SortTrace trace) {
        if (low >= high) {
            return;
        }
        int pivot = values[high];
        int i = low - 1;
        trace.add(values, low, high, values.length,
                "Quick: choose pivot " + pivot + " for positions " + (low + 1) + "-" + (high + 1) + ".");
        for (int j = low; j < high; j++) {
            trace.add(values, j, high, values.length,
                    "Compare " + values[j] + " with pivot " + pivot + ".");
            if (values[j] >= pivot) {
                i++;
                swap(values, i, j);
                trace.add(values, i, j, values.length,
                        "Move higher-priority value to the left partition.");
            }
        }
        swap(values, i + 1, high);
        int pivotIndex = i + 1;
        trace.add(values, pivotIndex, pivotIndex, values.length,
                "Pivot placed at position " + (pivotIndex + 1) + ". Recurse on both sides.");
        quickTrace(values, low, pivotIndex - 1, trace);
        quickTrace(values, pivotIndex + 1, high, trace);
    }

    private void mergeTrace(int[] values, int left, int right, SortTrace trace) {
        if (left >= right) {
            return;
        }
        int mid = left + (right - left) / 2;
        mergeTrace(values, left, mid, trace);
        mergeTrace(values, mid + 1, right, trace);
        int[] merged = new int[right - left + 1];
        int i = left;
        int j = mid + 1;
        int k = 0;
        while (i <= mid && j <= right) {
            trace.add(values, i, j, values.length,
                    "Merge: compare front values from the two sorted halves.");
            if (values[i] >= values[j]) {
                merged[k++] = values[i++];
            } else {
                merged[k++] = values[j++];
            }
        }
        while (i <= mid) {
            merged[k++] = values[i++];
        }
        while (j <= right) {
            merged[k++] = values[j++];
        }
        for (int offset = 0; offset < merged.length; offset++) {
            values[left + offset] = merged[offset];
            trace.add(values, left + offset, -1, values.length,
                    "Write merged value back into position " + (left + offset + 1) + ".");
        }
    }

    private void heapTrace(int[] values, SortTrace trace) {
        int n = values.length;
        for (int i = n / 2 - 1; i >= 0; i--) {
            minHeapify(values, n, i, trace);
        }
        trace.add(values, 0, -1, n,
                "Heap: build a min-heap so the lowest priority can be moved to the sorted suffix.");
        for (int end = n - 1; end > 0; end--) {
            swap(values, 0, end);
            trace.add(values, 0, end, end,
                    "Move the lowest remaining priority to the right side.");
            minHeapify(values, end, 0, trace);
        }
    }

    private void minHeapify(int[] values, int heapSize, int root, SortTrace trace) {
        int smallest = root;
        int left = 2 * root + 1;
        int right = 2 * root + 2;
        if (left < heapSize && values[left] < values[smallest]) {
            smallest = left;
        }
        if (right < heapSize && values[right] < values[smallest]) {
            smallest = right;
        }
        trace.add(values, root, smallest, heapSize,
                "Heapify: compare parent with children inside the heap.");
        if (smallest != root) {
            swap(values, root, smallest);
            trace.add(values, root, smallest, heapSize,
                    "Swap to restore the heap property.");
            minHeapify(values, heapSize, smallest, trace);
        }
    }

    private void renderSortFrame(Pane pane, Label step, SortFrame frame, int index, int total) {
        pane.getChildren().clear();
        double width = pane.getWidth() > 0 ? pane.getWidth() : pane.getPrefWidth();
        double left = 24;
        double top = 24;
        double baseY = 260;
        double chartHeight = 218;
        double plotWidth = Math.max(1, width - left * 2);
        double stepX = frame.values.length <= 1 ? plotWidth : plotWidth / (frame.values.length - 1);
        double strokeWidth = Math.max(0.6, Math.min(2.4, plotWidth / Math.max(1, frame.values.length) * 0.82));
        int min = frame.values[0];
        int max = frame.values[0];
        for (int value : frame.values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        int range = Math.max(1, max - min);

        Line base = new Line(left - 10, baseY, width - left + 10, baseY);
        base.getStyleClass().add("chart-axis");
        pane.getChildren().add(base);

        for (int i = 0; i < frame.values.length; i++) {
            double normal = (frame.values[i] - min) / (double) range;
            double barHeight = 8 + normal * chartHeight;
            double x = left + i * stepX;
            double y = baseY - barHeight;
            Line bar = new Line(x, baseY, x, y);
            bar.setStrokeWidth(strokeWidth);
            bar.getStyleClass().add("sort-demo-bar");
            if (i == frame.highlightA || i == frame.highlightB) {
                bar.getStyleClass().add("sort-demo-highlight");
            }
            if (i >= frame.sortedStart) {
                bar.getStyleClass().add("sort-demo-sorted");
            }
            pane.getChildren().add(bar);
        }

        step.setText("Step " + (index + 1) + "/" + total + ": " + frame.note);
    }

    private void showTopTargets() {
        TableView<TopRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<TopRow, String> dataset = new TableColumn<>("Dataset");
        dataset.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().dataset));
        TableColumn<TopRow, Number> rank = new TableColumn<>("Rank");
        rank.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().rank));
        TableColumn<TopRow, String> location = new TableColumn<>("Location ID");
        location.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().locationId));
        TableColumn<TopRow, Number> priority = new TableColumn<>("Priority Score");
        priority.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().priorityScore));
        table.getColumns().add(dataset);
        table.getColumns().add(rank);
        table.getColumns().add(location);
        table.getColumns().add(priority);

        for (int i = 0; i < allTop10.size(); i++) {
            List<Location> top = allTop10.get(i);
            for (int j = 0; j < top.size(); j++) {
                Location locationRow = top.get(j);
                table.getItems().add(new TopRow("Dataset " + (char) ('A' + i),
                        j + 1, locationRow.getLocationId(), locationRow.getPriorityScore()));
            }
        }

        VBox page = page("Task A Output: Top 10 Candidate Locations",
                "These ranked targets are reused to define the required shortest-path cases.",
                table);
        setContent(page, "Top 10 targets ready.");
    }

    private void showPathCases() {
        VBox holder = new VBox(16);
        holder.getStyleClass().add("content-card");

        Label title = new Label("Task B: Interactive Path Explorer");
        title.getStyleClass().add("page-title");
        Label intro = new Label("Choose a required case and algorithm, then generate the route visualisation and explanation.");
        intro.getStyleClass().add("page-subtitle");
        intro.setWrapText(true);

        ComboBox<PathCase> caseSelector = new ComboBox<>();
        caseSelector.getItems().addAll(pathCases);
        caseSelector.getSelectionModel().selectFirst();
        caseSelector.getStyleClass().add("case-selector");

        ToggleGroup algorithmGroup = new ToggleGroup();
        RadioButton dijkstraOption = new RadioButton("Dijkstra (weighted cost)");
        dijkstraOption.setToggleGroup(algorithmGroup);
        dijkstraOption.setSelected(true);
        RadioButton floydDpOption = new RadioButton("Floyd + DP (weighted cost)");
        floydDpOption.setToggleGroup(algorithmGroup);
        RadioButton bfsOption = new RadioButton("BFS (edge count)");
        bfsOption.setToggleGroup(algorithmGroup);

        Button showRoute = new Button("Show Route");
        showRoute.getStyleClass().add("primary-button");
        Button animateRoute = new Button("Animate Route");
        animateRoute.getStyleClass().add("case-button");
        Button animateSearch = new Button("Animate Search Process");
        animateSearch.getStyleClass().add("case-button");

        FlowPane controls = new FlowPane(12, 10,
                labelledControl("Case", caseSelector),
                dijkstraOption,
                floydDpOption,
                bfsOption,
                showRoute,
                animateRoute,
                animateSearch);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("path-controls");

        VBox details = new VBox(14);
        details.setFillWidth(true);

        showRoute.setOnAction(event -> renderRoute(details, buildRoute(
                caseSelector.getValue(),
                selectedPathAlgorithm(algorithmGroup)), false));
        animateRoute.setOnAction(event -> renderRoute(details, buildRoute(
                caseSelector.getValue(),
                selectedPathAlgorithm(algorithmGroup)), true));
        animateSearch.setOnAction(event -> renderSearchProcess(details, buildRoute(
                caseSelector.getValue(),
                selectedPathAlgorithm(algorithmGroup))));

        holder.getChildren().addAll(title, intro, controls, details);
        renderRoute(details, buildRoute(pathCases.get(0), "Dijkstra"), false);
        setContent(holder, "Path visualisation ready.");
    }

    private VBox labelledControl(String labelText, javafx.scene.Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("control-label");
        VBox box = new VBox(4, label, control);
        box.getStyleClass().add("control-stack");
        return box;
    }

    private void renderRoute(VBox details, RouteView route, boolean animate) {
        stopRouteAnimation();
        details.getChildren().clear();

        Label summary = new Label(route.summary());
        summary.getStyleClass().add("summary-label");
        summary.setWrapText(true);

        ScrollPane diagramScroll = new ScrollPane(buildPathDiagram(route, animate));
        diagramScroll.setFitToWidth(true);
        diagramScroll.setPannable(true);
        diagramScroll.getStyleClass().add("path-diagram-scroll");

        TextArea notes = new TextArea(route.notes());
        notes.setEditable(false);
        notes.setWrapText(true);
        notes.setPrefRowCount(7);
        notes.getStyleClass().add("script-box");

        details.getChildren().addAll(summary, diagramScroll, notes);
    }

    private void renderSearchProcess(VBox details, RouteView route) {
        stopRouteAnimation();
        details.getChildren().clear();

        Label summary = new Label(route.caseName + " using " + route.algorithm
                + ": search process from " + route.start + " to " + firstSearchTarget(route));
        summary.getStyleClass().add("summary-label");
        summary.setWrapText(true);

        VBox process = buildSearchProcessPanel(route);
        details.getChildren().addAll(summary, process);
    }

    private String selectedPathAlgorithm(ToggleGroup algorithmGroup) {
        RadioButton selected = (RadioButton) algorithmGroup.getSelectedToggle();
        return selected == null ? "Dijkstra" : selected.getText();
    }

    private RouteView buildRoute(PathCase pathCase, String algorithm) {
        if (algorithm.startsWith("Dijkstra")) {
            return new RouteView(pathCase.caseName, "Dijkstra", "total weighted cost",
                    pathCase.start, pathCase.end, pathCase.viaNodes, pathCase.reachable,
                    pathCase.path, pathCase.legCosts, pathCase.totalCost,
                    buildEdgeLabels(pathCase.path, true));
        }

        if (algorithm.startsWith("Floyd")) {
            Graph.RoutePlanResult result = graph.shortestPathVisitingFloyd(
                    pathCase.start, pathCase.viaNodes, pathCase.end);
            if (!result.isReachable()) {
                return new RouteView(pathCase.caseName, "Floyd + DP", "total weighted cost",
                        pathCase.start, pathCase.end, pathCase.viaNodes, false,
                        Collections.emptyList(), Collections.emptyList(), -1,
                        Collections.emptyList());
            }
            return new RouteView(pathCase.caseName, "Floyd + DP", "total weighted cost",
                    pathCase.start, pathCase.end, result.visitOrder, true,
                    result.path, result.legCosts, result.totalCost,
                    buildEdgeLabels(result.path, true));
        }

        List<String> targets = new ArrayList<>(pathCase.viaNodes);
        targets.add(pathCase.end);

        List<String> fullPath = new ArrayList<>();
        List<Integer> legCounts = new ArrayList<>();
        int totalEdges = 0;
        String current = pathCase.start;
        fullPath.add(current);

        for (String target : targets) {
            Graph.DijkstraResult leg = graph.shortestPathUnweighted(current, target);
            if (!leg.isReachable()) {
                return new RouteView(pathCase.caseName, "BFS", "edge count",
                        pathCase.start, pathCase.end, pathCase.viaNodes, false,
                        Collections.emptyList(), Collections.emptyList(), -1,
                        Collections.emptyList());
            }
            for (int i = 1; i < leg.path.size(); i++) {
                fullPath.add(leg.path.get(i));
            }
            legCounts.add(leg.totalCost);
            totalEdges += leg.totalCost;
            current = target;
        }

        return new RouteView(pathCase.caseName, "BFS", "edge count",
                pathCase.start, pathCase.end, pathCase.viaNodes, true,
                fullPath, legCounts, totalEdges,
                buildEdgeLabels(fullPath, false));
    }

    private VBox buildSearchProcessPanel(RouteView route) {
        if (route.algorithm.startsWith("Floyd")) {
            VBox panel = new VBox(10);
            panel.getStyleClass().add("process-panel");
            Label title = new Label("Floyd + DP Process");
            title.getStyleClass().add("panel-title");
            TextArea notes = new TextArea(
                    "Floyd-Warshall is an all-pairs dynamic programming process:\n"
                            + "1. Start with a distance matrix from the weighted graph.\n"
                            + "2. For each possible intermediate node k, test whether i -> k -> j is cheaper than i -> j.\n"
                            + "3. After the matrix is built, DP chooses the cheapest order for required via nodes.\n"
                            + "4. The final route is reconstructed from the stored next-hop matrix.");
            notes.setEditable(false);
            notes.setWrapText(true);
            notes.setPrefRowCount(6);
            notes.getStyleClass().add("script-box");
            panel.getChildren().addAll(title, notes);
            return panel;
        }

        List<SearchFrame> frames = route.algorithm.startsWith("BFS")
                ? buildBfsFrames(route.start, firstSearchTarget(route))
                : buildDijkstraFrames(route.start, firstSearchTarget(route));

        Label title = new Label(route.algorithm + " Exploration Process");
        title.getStyleClass().add("panel-title");
        Label hint = new Label(route.algorithm.startsWith("BFS")
                ? "BFS uses a queue: first discovered, first explored."
                : "Dijkstra uses a priority queue: the lowest known weighted cost is explored first.");
        hint.getStyleClass().add("comparison-subtitle");
        hint.setWrapText(true);

        Pane pane = new Pane();
        pane.getStyleClass().add("search-process-pane");
        pane.setMinSize(760, 300);
        pane.setPrefSize(760, 300);
        Label step = new Label();
        step.getStyleClass().add("process-step-label");
        step.setWrapText(true);

        renderSearchFrame(pane, step, frames.get(0), 0, frames.size());
        processAnimation = new Timeline();
        for (int i = 0; i < frames.size(); i++) {
            final int index = i;
            processAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(index * 520L), event ->
                    renderSearchFrame(pane, step, frames.get(index), index, frames.size())));
        }
        processAnimation.play();

        VBox panel = new VBox(10, title, hint, pane, step);
        panel.getStyleClass().add("process-panel");
        return panel;
    }

    private String firstSearchTarget(RouteView route) {
        return route.viaNodes.isEmpty() ? route.end : route.viaNodes.get(0);
    }

    private List<SearchFrame> buildDijkstraFrames(String start, String target) {
        List<SearchFrame> frames = new ArrayList<>();
        Map<String, Integer> dist = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<NodeCost> queue = new PriorityQueue<>();
        dist.put(start, 0);
        queue.offer(new NodeCost(start, 0));
        frames.add(new SearchFrame(start, List.of(start + " (0)"), Collections.emptyList(),
                "Initialise distance(" + start + ") = 0."));

        while (!queue.isEmpty() && frames.size() < 34) {
            NodeCost current = queue.poll();
            if (visited.contains(current.node) || current.cost > dist.getOrDefault(current.node, Integer.MAX_VALUE)) {
                continue;
            }
            visited.add(current.node);
            List<Edge> neighbors = graph.neighbors(current.node);
            neighbors.sort((a, b) -> a.getTo().compareTo(b.getTo()));
            for (Edge edge : neighbors) {
                int candidate = current.cost + edge.getWeight();
                if (candidate < dist.getOrDefault(edge.getTo(), Integer.MAX_VALUE)) {
                    dist.put(edge.getTo(), candidate);
                    queue.offer(new NodeCost(edge.getTo(), candidate));
                }
            }
            frames.add(new SearchFrame(current.node, priorityQueuePreview(queue),
                    sortedStrings(visited),
                    "Settle " + current.node + " at cost " + current.cost
                            + "; relax its outgoing weighted edges."));
            if (current.node.equals(target)) {
                break;
            }
        }
        return frames;
    }

    private List<SearchFrame> buildBfsFrames(String start, String target) {
        List<SearchFrame> frames = new ArrayList<>();
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.offer(start);
        visited.add(start);
        frames.add(new SearchFrame(start, new ArrayList<>(queue), sortedStrings(visited),
                "Start with " + start + " in the queue."));

        while (!queue.isEmpty() && frames.size() < 34) {
            String current = queue.poll();
            List<Edge> neighbors = graph.neighbors(current);
            neighbors.sort((a, b) -> a.getTo().compareTo(b.getTo()));
            for (Edge edge : neighbors) {
                if (visited.add(edge.getTo())) {
                    queue.offer(edge.getTo());
                }
            }
            frames.add(new SearchFrame(current, new ArrayList<>(queue), sortedStrings(visited),
                    "Visit " + current + "; enqueue all newly discovered neighbours."));
            if (current.equals(target)) {
                break;
            }
        }
        return frames;
    }

    private List<String> priorityQueuePreview(PriorityQueue<NodeCost> queue) {
        List<NodeCost> nodes = new ArrayList<>(queue);
        nodes.sort(null);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(10, nodes.size()); i++) {
            NodeCost node = nodes.get(i);
            labels.add(node.node + " (" + node.cost + ")");
        }
        return labels;
    }

    private List<String> sortedStrings(Set<String> values) {
        List<String> result = new ArrayList<>(values);
        Collections.sort(result);
        if (result.size() > 14) {
            return new ArrayList<>(result.subList(0, 14));
        }
        return result;
    }

    private void renderSearchFrame(Pane pane, Label step, SearchFrame frame, int index, int total) {
        pane.getChildren().clear();
        addSearchColumn(pane, "Current", List.of(frame.current), 26, 28, "search-current-chip", 146);
        addSearchColumn(pane, "Frontier", frame.frontier, 210, 28, "search-frontier-chip", 214);
        addSearchColumn(pane, "Visited", frame.visited, 480, 28, "search-visited-chip", 214);
        step.setText("Step " + (index + 1) + "/" + total + ": " + frame.note);
    }

    private void addSearchColumn(Pane pane, String title, List<String> values,
                                 double x, double y, String chipClass, double chipWidth) {
        Label heading = new Label(title);
        heading.getStyleClass().add("search-column-title");
        heading.setLayoutX(x);
        heading.setLayoutY(y);
        pane.getChildren().add(heading);

        double chipY = y + 34;
        int limit = Math.min(values.size(), 8);
        for (int i = 0; i < limit; i++) {
            Label chip = new Label(values.get(i));
            chip.getStyleClass().addAll("search-chip", chipClass);
            chip.setMinWidth(chipWidth);
            chip.setMaxWidth(chipWidth);
            chip.setLayoutX(x);
            chip.setLayoutY(chipY + i * 26);
            pane.getChildren().add(chip);
        }
        if (values.size() > limit) {
            Label more = new Label("+" + (values.size() - limit) + " more");
            more.getStyleClass().add("search-more-label");
            more.setLayoutX(x);
            more.setLayoutY(chipY + limit * 26);
            pane.getChildren().add(more);
        }
    }

    private List<String> buildEdgeLabels(List<String> path, boolean weighted) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            if (weighted) {
                labels.add(String.valueOf(graph.edgeWeight(path.get(i), path.get(i + 1))));
            } else {
                labels.add("1 edge");
            }
        }
        return labels;
    }

    private Pane buildPathDiagram(RouteView route, boolean animate) {
        int nodeCount = Math.max(1, route.path.size());
        int nodesPerRow = 7;
        int visibleColumns = Math.min(nodesPerRow, nodeCount);
        int rows = Math.max(1, (nodeCount + nodesPerRow - 1) / nodesPerRow);
        double left = 76;
        double top = 74;
        double columnStep = 120;
        double rowStep = 116;
        double radius = 22;
        double width = Math.max(760, left * 2 + (visibleColumns - 1) * columnStep);
        double height = top * 2 + (rows - 1) * rowStep + 72;

        Pane diagram = new Pane();
        diagram.setMinSize(width, height);
        diagram.setPrefSize(width, height);
        diagram.getStyleClass().add("path-diagram");

        if (!route.reachable || route.path.isEmpty()) {
            Label empty = new Label("No reachable path found.");
            empty.getStyleClass().add("empty-path-label");
            empty.setLayoutX(28);
            empty.setLayoutY(28);
            diagram.getChildren().add(empty);
            return diagram;
        }

        for (int i = 0; i < route.path.size() - 1; i++) {
            double[] from = pathPoint(i, nodesPerRow, left, top, columnStep, rowStep);
            double[] to = pathPoint(i + 1, nodesPerRow, left, top, columnStep, rowStep);
            addArrow(diagram, from[0], from[1], to[0], to[1], radius, i + 1);
            addEdgeLabel(diagram, route.edgeLabels.get(i), from[0], from[1], to[0], to[1], i + 1);
        }

        for (int i = 0; i < route.path.size(); i++) {
            double[] point = pathPoint(i, nodesPerRow, left, top, columnStep, rowStep);
            addPathNode(diagram, route, i, point[0], point[1], radius, i);
        }

        addLegend(diagram, 28, height - 44);
        if (animate) {
            animateDiagram(diagram, route.path.size());
        }
        return diagram;
    }

    private static double[] pathPoint(int index, int nodesPerRow, double left, double top,
                                      double columnStep, double rowStep) {
        int row = index / nodesPerRow;
        int column = index % nodesPerRow;
        if (row % 2 == 1) {
            column = nodesPerRow - 1 - column;
        }
        return new double[]{left + column * columnStep, top + row * rowStep};
    }

    private void addArrow(Pane diagram, double startX, double startY,
                          double endX, double endY, double radius, int stepIndex) {
        double angle = Math.atan2(endY - startY, endX - startX);
        double lineStartX = startX + Math.cos(angle) * (radius + 5);
        double lineStartY = startY + Math.sin(angle) * (radius + 5);
        double lineEndX = endX - Math.cos(angle) * (radius + 10);
        double lineEndY = endY - Math.sin(angle) * (radius + 10);

        Line line = new Line(lineStartX, lineStartY, lineEndX, lineEndY);
        line.getStyleClass().add("diagram-edge");
        addStepClass(line, stepIndex);

        double arrowLength = 12;
        double arrowWidth = 7;
        double tipX = endX - Math.cos(angle) * (radius + 2);
        double tipY = endY - Math.sin(angle) * (radius + 2);
        double baseX = tipX - Math.cos(angle) * arrowLength;
        double baseY = tipY - Math.sin(angle) * arrowLength;
        double normalX = -Math.sin(angle);
        double normalY = Math.cos(angle);

        Polygon arrowHead = new Polygon(
                tipX, tipY,
                baseX + normalX * arrowWidth, baseY + normalY * arrowWidth,
                baseX - normalX * arrowWidth, baseY - normalY * arrowWidth
        );
        arrowHead.getStyleClass().add("diagram-arrow");
        addStepClass(arrowHead, stepIndex);
        diagram.getChildren().addAll(line, arrowHead);
    }

    private void addEdgeLabel(Pane diagram, String text,
                              double startX, double startY, double endX, double endY,
                              int stepIndex) {
        Label label = new Label(text);
        label.getStyleClass().add("edge-weight-label");
        addStepClass(label, stepIndex);
        double midX = (startX + endX) / 2.0;
        double midY = (startY + endY) / 2.0;
        label.setLayoutX(midX - 18);
        label.setLayoutY(midY - 18);
        diagram.getChildren().add(label);
    }

    private void animateDiagram(Pane diagram, int stepCount) {
        routeAnimation = new SequentialTransition();
        for (javafx.scene.Node node : diagram.getChildren()) {
            if (isAnimatableRouteNode(node)) {
                node.setOpacity(0);
            }
        }

        for (int step = 0; step < stepCount; step++) {
            ParallelTransition group = new ParallelTransition();
            String stepClass = stepClass(step);
            for (javafx.scene.Node node : diagram.getChildren()) {
                if (node.getStyleClass().contains(stepClass)) {
                    FadeTransition fade = new FadeTransition(Duration.millis(170), node);
                    fade.setFromValue(0);
                    fade.setToValue(1);
                    group.getChildren().add(fade);
                }
            }
            routeAnimation.getChildren().add(group);
        }
        routeAnimation.play();
    }

    private void addStepClass(javafx.scene.Node node, int stepIndex) {
        node.getStyleClass().add(stepClass(stepIndex));
    }

    private String stepClass(int stepIndex) {
        return "route-step-" + stepIndex;
    }

    private boolean isAnimatableRouteNode(javafx.scene.Node node) {
        return node.getStyleClass().contains("diagram-edge")
                || node.getStyleClass().contains("diagram-arrow")
                || node.getStyleClass().contains("edge-weight-label")
                || node.getStyleClass().contains("diagram-node")
                || node.getStyleClass().contains("diagram-step")
                || node.getStyleClass().contains("diagram-node-label");
    }

    private void addPathNode(Pane diagram, RouteView route, int index,
                             double x, double y, double radius, int stepIndex) {
        String nodeId = route.path.get(index);
        Circle circle = new Circle(x, y, radius);
        circle.getStyleClass().add("diagram-node");
        addStepClass(circle, stepIndex);
        if (index == 0) {
            circle.getStyleClass().add("diagram-start");
        } else if (index == route.path.size() - 1) {
            circle.getStyleClass().add("diagram-end");
        } else if (route.viaNodes.contains(nodeId)) {
            circle.getStyleClass().add("diagram-via");
        }

        Label step = new Label(String.valueOf(index + 1));
        step.getStyleClass().add("diagram-step");
        addStepClass(step, stepIndex);
        if (index != 0
                && index != route.path.size() - 1
                && !route.viaNodes.contains(nodeId)) {
            step.getStyleClass().add("diagram-step-dark");
        }
        step.setMinSize(24, 18);
        step.setAlignment(Pos.CENTER);
        step.setLayoutX(x - 12);
        step.setLayoutY(y - 9);

        Label label = new Label(nodeId);
        label.getStyleClass().add("diagram-node-label");
        addStepClass(label, stepIndex);
        label.setMinWidth(70);
        label.setAlignment(Pos.CENTER);
        label.setLayoutX(x - 35);
        label.setLayoutY(y + radius + 7);

        diagram.getChildren().addAll(circle, step, label);
    }

    private void addLegend(Pane diagram, double x, double y) {
        addLegendItem(diagram, x, y, "legend-start", "Start");
        addLegendItem(diagram, x + 128, y, "legend-via", "Required via");
        addLegendItem(diagram, x + 304, y, "legend-end", "Destination");
        addLegendItem(diagram, x + 476, y, "legend-normal", "Intermediate");
    }

    private void addLegendItem(Pane diagram, double x, double y, String styleClass, String text) {
        Circle dot = new Circle(x + 8, y + 8, 8);
        dot.getStyleClass().addAll("legend-dot", styleClass);
        Label label = new Label(text);
        label.getStyleClass().add("legend-label");
        label.setLayoutX(x + 22);
        label.setLayoutY(y - 1);
        diagram.getChildren().addAll(dot, label);
    }

    private void showBfsComparison() {
        HBox comparison = buildBfsComparisonPanel();
        TextArea explanation = new TextArea(
                "Dijkstra result:\n"
                        + pathLine(bfsComparison.weighted.path) + "\n"
                        + "Total weight = " + bfsComparison.weighted.totalCost + "\n\n"
                        + "Floyd + DP result:\n"
                        + pathLine(bfsComparison.floyd.path) + "\n"
                        + "Total weight = " + bfsComparison.floyd.totalCost + "\n\n"
                        + "BFS result:\n"
                        + pathLine(bfsComparison.unweighted.path) + "\n"
                        + "Edge count = " + bfsComparison.unweighted.totalCost + "\n\n"
                        + "Key point: Dijkstra and Floyd + DP minimise total weighted cost, while BFS minimises "
                        + "number of edges. The BFS metric is a different unit, so its shorter hop count can still "
                        + "produce a heavier route.");
        explanation.setEditable(false);
        explanation.setWrapText(true);
        explanation.getStyleClass().add("script-box");

        VBox page = page("Extension: Dijkstra vs Floyd + DP vs BFS",
                "The three methods are shown together; weighted cost and edge count are intentionally separated.",
                comparison, explanation);
        setContent(page, "Path comparison ready.");
    }

    private HBox buildBfsComparisonPanel() {
        VBox dijkstra = comparisonMetricPanel(
                "Dijkstra",
                "Optimises total weighted cost",
                "Total weight",
                String.valueOf(bfsComparison.weighted.totalCost),
                pathLine(bfsComparison.weighted.path),
                bfsComparison.weighted.path,
                "dijkstra-panel"
        );
        VBox floyd = comparisonMetricPanel(
                "Floyd + DP",
                "Uses all-pairs shortest paths",
                "Total weight",
                String.valueOf(bfsComparison.floyd.totalCost),
                pathLine(bfsComparison.floyd.path),
                bfsComparison.floyd.path,
                "floyd-panel"
        );
        int bfsActualWeight = graph.pathWeight(bfsComparison.unweighted.path);
        VBox bfs = comparisonMetricPanel(
                "BFS",
                "Optimises number of edges only",
                "Edge count, actual weight " + bfsActualWeight,
                String.valueOf(bfsComparison.unweighted.totalCost),
                pathLine(bfsComparison.unweighted.path),
                bfsComparison.unweighted.path,
                "bfs-panel"
        );
        HBox box = new HBox(14, dijkstra, floyd, bfs);
        box.getStyleClass().add("comparison-row");
        HBox.setHgrow(dijkstra, Priority.ALWAYS);
        HBox.setHgrow(floyd, Priority.ALWAYS);
        HBox.setHgrow(bfs, Priority.ALWAYS);
        return box;
    }

    private VBox comparisonMetricPanel(String titleText, String subtitleText,
                                       String metricLabel, String metricValue,
                                       String path, List<String> nodes, String styleClass) {
        Label title = new Label(titleText);
        title.getStyleClass().add("comparison-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("comparison-subtitle");
        subtitle.setWrapText(true);
        Label metricName = new Label(metricLabel);
        metricName.getStyleClass().add("comparison-metric-name");
        Label metric = new Label(metricValue);
        metric.getStyleClass().add("comparison-metric-value");
        Label pathLabel = new Label(path);
        pathLabel.getStyleClass().add("comparison-path");
        pathLabel.setWrapText(true);
        ScrollPane miniPath = new ScrollPane(buildMiniPathStrip(nodes));
        miniPath.setFitToWidth(true);
        miniPath.getStyleClass().add("mini-path-scroll");

        VBox panel = new VBox(8, title, subtitle, metricName, metric, miniPath, pathLabel);
        panel.getStyleClass().addAll("comparison-panel", styleClass);
        panel.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private FlowPane buildMiniPathStrip(List<String> nodes) {
        FlowPane strip = new FlowPane(6, 6);
        strip.getStyleClass().add("mini-path-strip");
        for (int i = 0; i < nodes.size(); i++) {
            Label node = new Label(nodes.get(i));
            node.getStyleClass().add("mini-path-node");
            strip.getChildren().add(node);
            if (i < nodes.size() - 1) {
                Label arrow = new Label("->");
                arrow.getStyleClass().add("mini-path-arrow");
                strip.getChildren().add(arrow);
            }
        }
        return strip;
    }

    private TableView<BenchmarkRow> benchmarkTable() {
        TableView<BenchmarkRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<BenchmarkRow, String> dataset = new TableColumn<>("Dataset");
        dataset.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().dataset));
        TableColumn<BenchmarkRow, String> algorithm = new TableColumn<>("Algorithm");
        algorithm.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().algorithm));
        TableColumn<BenchmarkRow, Number> average = new TableColumn<>("Avg ms");
        average.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().averageMs));
        TableColumn<BenchmarkRow, Number> min = new TableColumn<>("Min ms");
        min.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().minMs));
        TableColumn<BenchmarkRow, Number> max = new TableColumn<>("Max ms");
        max.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().maxMs));
        table.getColumns().add(dataset);
        table.getColumns().add(algorithm);
        table.getColumns().add(average);
        table.getColumns().add(min);
        table.getColumns().add(max);
        return table;
    }

    private BenchmarkRow findBenchmarkRow(String dataset, String algorithm) {
        for (BenchmarkRow row : benchmarkRows) {
            if (row.dataset.equals(dataset) && row.algorithm.equals(algorithm)) {
                return row;
            }
        }
        return null;
    }

    private List<BenchmarkRow> filteredBenchmarkRows(boolean includeBubble) {
        List<BenchmarkRow> rows = new ArrayList<>();
        for (BenchmarkRow row : benchmarkRows) {
            if (includeBubble || !row.algorithm.equals("Bubble Sort")) {
                rows.add(row);
            }
        }
        return rows;
    }

    private String buildSortingConclusion(boolean includeBubble) {
        StringBuilder builder = new StringBuilder();
        builder.append(includeBubble ? "All-algorithm view" : "Fast-algorithm view")
                .append(":\n");
        String[] datasets = {"Dataset A", "Dataset B", "Dataset C"};
        for (String dataset : datasets) {
            BenchmarkRow fastest = null;
            BenchmarkRow slowest = null;
            for (BenchmarkRow row : benchmarkRows) {
                if (!row.dataset.equals(dataset)) {
                    continue;
                }
                if (!includeBubble && row.algorithm.equals("Bubble Sort")) {
                    continue;
                }
                if (fastest == null || row.averageMs < fastest.averageMs) {
                    fastest = row;
                }
                if (slowest == null || row.averageMs > slowest.averageMs) {
                    slowest = row;
                }
            }
            if (fastest != null && slowest != null) {
                builder.append(dataset)
                        .append(": fastest = ").append(fastest.algorithm)
                        .append(" (").append(formatRuntime(fastest.averageMs)).append(" ms), slowest = ")
                        .append(slowest.algorithm).append(" (")
                        .append(formatRuntime(slowest.averageMs)).append(" ms).\n");
            }
        }
        builder.append(includeBubble
                ? "Bubble Sort is kept as a baseline; switch views to inspect the practical differences between Quick, Merge, and Heap Sort."
                : "With Bubble Sort hidden, the chart focuses on differences among the scalable algorithms.");
        return builder.toString();
    }

    private void addChartLabel(Pane pane, String text, double x, double y, String styleClasses) {
        Label label = new Label(text);
        for (String styleClass : styleClasses.split(" ")) {
            if (!styleClass.isBlank()) {
                label.getStyleClass().add(styleClass);
            }
        }
        if (label.getStyleClass().contains("rotated-axis-label")) {
            label.setRotate(-90);
        }
        label.setLayoutX(x);
        label.setLayoutY(y);
        pane.getChildren().add(label);
    }

    private static String formatRuntime(double value) {
        if (value >= 10) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.format(Locale.US, "%.3f", value);
    }

    private static void swap(int[] values, int i, int j) {
        int temp = values[i];
        values[i] = values[j];
        values[j] = temp;
    }

    private static boolean contains(String[] values, String target) {
        for (String value : values) {
            if (value.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private VBox page(String titleText, String subtitleText, javafx.scene.Node... nodes) {
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);

        VBox box = new VBox(16, title, subtitle);
        box.getStyleClass().add("content-card");
        box.getChildren().addAll(nodes);
        return box;
    }

    private VBox metric(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("metric-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("metric-value");
        valueNode.setWrapText(true);
        VBox box = new VBox(4, labelNode, valueNode);
        box.getStyleClass().add("metric-card");
        return box;
    }

    private ScrollPane centeredScroll(javafx.scene.Node node) {
        ScrollPane scroll = new ScrollPane(node);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    private void setContent(javafx.scene.Node node, String message) {
        content.getChildren().setAll(centeredScroll(node));
        status.setText(message + " Data: " + dataDir.toAbsolutePath().normalize());
    }

    private void stopRouteAnimation() {
        if (routeAnimation != null) {
            routeAnimation.stop();
            routeAnimation = null;
        }
        if (processAnimation != null) {
            processAnimation.stop();
            processAnimation = null;
        }
    }

    private void regenerateArtifacts() {
        status.setText("Regenerating CSV and SVG artefacts...");
        Thread worker = new Thread(() -> {
            try {
                Main.main(new String[]{dataDir.toString()});
                Platform.runLater(() -> status.setText("Artifacts regenerated in " + OUTPUT_DIR.toAbsolutePath().normalize()));
            } catch (Exception e) {
                Platform.runLater(() -> status.setText("Artifact regeneration failed: " + e.getMessage()));
            }
        }, "artifact-regenerator");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadResults() throws IOException {
        String[] files = {"candidates_A.csv", "candidates_B.csv", "candidates_C.csv"};
        String[] labels = {"Dataset A", "Dataset B", "Dataset C"};
        Sorter[] sorters = {new BubbleSort(), new QuickSort(), new MergeSort(), new HeapSort()};

        allTop10 = new ArrayList<>();
        benchmarkRows = new ArrayList<>();
        originalDatasets = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            List<Location> original = CSVReader.readLocations(dataDir.resolve(files[i]));
            originalDatasets.add(deepCopy(original));

            List<Location> ranked = deepCopy(original);
            new QuickSort().sort(ranked);
            allTop10.add(new ArrayList<>(ranked.subList(0, Math.min(TOP_N, ranked.size()))));

            for (Sorter sorter : sorters) {
                TimingStats stats = new TimingStats();
                for (int run = 0; run < MEASURED_RUNS; run++) {
                    List<Location> copy = deepCopy(original);
                    long start = System.nanoTime();
                    sorter.sort(copy);
                    stats.add(System.nanoTime() - start);
                }
                benchmarkRows.add(new BenchmarkRow(labels[i], sorter.getName(),
                        stats.averageMs(), stats.minMs(), stats.maxMs()));
            }
        }

        graph = new Graph();
        List<Edge> edges = CSVReader.readEdges(dataDir.resolve("paths.csv"));
        graph.addEdges(edges);
        buildPathResults();
    }

    private void buildPathResults() {
        Location a1 = allTop10.get(0).get(0);
        Location a10 = allTop10.get(0).get(9);
        Location b1 = allTop10.get(1).get(0);
        Location b5 = allTop10.get(1).get(4);
        Location c1 = allTop10.get(2).get(0);
        Location c5 = allTop10.get(2).get(4);

        pathCases = new ArrayList<>();
        pathCases.add(runCase("Case 1", a1.getLocationId(), Collections.emptyList(), a1.getLocationId()));
        pathCases.add(runCase("Case 2", a1.getLocationId(), Collections.emptyList(), a10.getLocationId()));
        pathCases.add(runCase("Case 3", a1.getLocationId(), List.of(b5.getLocationId()), b1.getLocationId()));
        pathCases.add(runCase("Case 4", a1.getLocationId(),
                List.of(b5.getLocationId(), c5.getLocationId()), c1.getLocationId()));

        Graph.DijkstraResult weighted = graph.shortestPath(a1.getLocationId(), a10.getLocationId());
        Graph.DijkstraResult floyd = graph.shortestPathFloyd(a1.getLocationId(), a10.getLocationId());
        Graph.DijkstraResult unweighted = graph.shortestPathUnweighted(a1.getLocationId(), a10.getLocationId());
        bfsComparison = new BfsComparison(a1.getLocationId(), a10.getLocationId(), weighted, floyd, unweighted);
    }

    private PathCase runCase(String name, String start, List<String> viaNodes, String end) {
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
                return new PathCase(name, start, end, viaNodes, false,
                        Collections.emptyList(), Collections.emptyList(), -1);
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

        return new PathCase(name, start, end, viaNodes, true, fullPath, legCosts, totalCost);
    }

    private static List<Location> deepCopy(List<Location> original) {
        List<Location> copy = new ArrayList<>(original.size());
        for (Location location : original) {
            copy.add(new Location(location.getLocationId(), location.getPriorityScore()));
        }
        return copy;
    }

    private static Path resolveDataDir(List<String> args) throws IOException {
        List<Path> candidates = new ArrayList<>();
        if (!args.isEmpty()) {
            candidates.add(Paths.get(args.get(0)));
        }

        Collections.addAll(candidates,
                Paths.get("resource/Group Project Datasets"),
                Paths.get("../resource/Group Project Datasets"),
                Paths.get("../../resource/Group Project Datasets"));

        List<Path> tried = new ArrayList<>();
        for (Path candidate : candidates) {
            tried.add(candidate.toAbsolutePath().normalize());
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Could not find Group Project Datasets. Tried:\n"
                + String.join("\n", tried.stream().map(Path::toString).toList()));
    }

    private static String pathLine(List<String> path) {
        return path.isEmpty() ? "No reachable path" : String.join(" -> ", path);
    }

    private void showStartupError(Stage stage, Exception e) {
        TextArea error = new TextArea(e.getMessage());
        error.setEditable(false);
        error.setWrapText(true);
        Scene scene = new Scene(new BorderPane(error), 720, 320);
        stage.setScene(scene);
        stage.setTitle("Dashboard startup error");
        stage.show();
    }

    private static class BenchmarkRow {
        final String dataset;
        final String algorithm;
        final double averageMs;
        final double minMs;
        final double maxMs;

        BenchmarkRow(String dataset, String algorithm, double averageMs, double minMs, double maxMs) {
            this.dataset = dataset;
            this.algorithm = algorithm;
            this.averageMs = round(averageMs);
            this.minMs = round(minMs);
            this.maxMs = round(maxMs);
        }
    }

    private static class TopRow {
        final String dataset;
        final int rank;
        final String locationId;
        final int priorityScore;

        TopRow(String dataset, int rank, String locationId, int priorityScore) {
            this.dataset = dataset;
            this.rank = rank;
            this.locationId = locationId;
            this.priorityScore = priorityScore;
        }
    }

    private static class PathCase {
        final String caseName;
        final String start;
        final String end;
        final List<String> viaNodes;
        final boolean reachable;
        final List<String> path;
        final List<Integer> legCosts;
        final int totalCost;

        PathCase(String caseName, String start, String end, List<String> viaNodes,
                 boolean reachable, List<String> path, List<Integer> legCosts, int totalCost) {
            this.caseName = caseName;
            this.start = start;
            this.end = end;
            this.viaNodes = new ArrayList<>(viaNodes);
            this.reachable = reachable;
            this.path = new ArrayList<>(path);
            this.legCosts = new ArrayList<>(legCosts);
            this.totalCost = totalCost;
        }

        String summary() {
            String via = viaNodes.isEmpty() ? "direct" : "via " + String.join(", ", viaNodes);
            String cost = reachable ? "total cost " + totalCost : "unreachable";
            return caseName + ": " + start + " to " + end + " (" + via + "), " + cost;
        }

        String notes() {
            return "Path: " + pathLine(path) + "\n"
                    + "Leg costs: " + (legCosts.isEmpty() ? "none" : joinIntegers(legCosts)) + "\n"
                    + "Total cost: " + totalCost;
        }

        @Override
        public String toString() {
            return caseName + ": " + start + " -> " + end;
        }
    }

    private static class RouteView {
        final String caseName;
        final String algorithm;
        final String metricName;
        final String start;
        final String end;
        final List<String> viaNodes;
        final boolean reachable;
        final List<String> path;
        final List<Integer> legCosts;
        final int totalCost;
        final List<String> edgeLabels;

        RouteView(String caseName, String algorithm, String metricName,
                  String start, String end, List<String> viaNodes,
                  boolean reachable, List<String> path,
                  List<Integer> legCosts, int totalCost, List<String> edgeLabels) {
            this.caseName = caseName;
            this.algorithm = algorithm;
            this.metricName = metricName;
            this.start = start;
            this.end = end;
            this.viaNodes = new ArrayList<>(viaNodes);
            this.reachable = reachable;
            this.path = new ArrayList<>(path);
            this.legCosts = new ArrayList<>(legCosts);
            this.totalCost = totalCost;
            this.edgeLabels = new ArrayList<>(edgeLabels);
        }

        String summary() {
            String via = viaNodes.isEmpty() ? "direct route" : "must visit " + String.join(", ", viaNodes);
            String result = reachable ? metricName + " = " + totalCost : "unreachable";
            return caseName + " using " + algorithm + ": " + start + " to " + end
                    + " (" + via + "), " + result;
        }

        String notes() {
            StringBuilder builder = new StringBuilder();
            builder.append("Algorithm: ").append(algorithm).append('\n');
            if (algorithm.equals("Floyd + DP")) {
                builder.append("Method: Floyd-Warshall precomputes all-pairs weighted shortest paths; ")
                        .append("dynamic programming selects the cheapest via-node order.\n");
            }
            builder.append("From: ").append(start).append('\n');
            builder.append("To: ").append(end).append('\n');
            builder.append(algorithm.equals("Floyd + DP") ? "DP visit order: " : "Required via nodes: ")
                    .append(viaNodes.isEmpty() ? "none" : String.join(", ", viaNodes))
                    .append('\n');
            builder.append("Path details:\n");
            if (path.isEmpty()) {
                builder.append("No reachable path found.\n");
            } else {
                for (int i = 0; i < path.size(); i++) {
                    builder.append(i + 1).append(". ").append(path.get(i)).append('\n');
                }
            }
            builder.append("Leg ").append(metricName).append("s: ")
                    .append(legCosts.isEmpty() ? "none" : joinIntegers(legCosts))
                    .append('\n');
            builder.append("Total ").append(metricName).append(": ").append(totalCost);
            return builder.toString();
        }
    }

    private static class BfsComparison {
        final String start;
        final String end;
        final Graph.DijkstraResult weighted;
        final Graph.DijkstraResult floyd;
        final Graph.DijkstraResult unweighted;

        BfsComparison(String start, String end,
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

    private static class SortFrame {
        final int[] values;
        final int highlightA;
        final int highlightB;
        final int sortedStart;
        final String note;

        SortFrame(int[] values, int highlightA, int highlightB, int sortedStart, String note) {
            this.values = values.clone();
            this.highlightA = highlightA;
            this.highlightB = highlightB;
            this.sortedStart = sortedStart;
            this.note = note;
        }
    }

    private static class SortTrace {
        private final List<SortFrame> frames = new ArrayList<>();
        private final int interval;
        private int operations;
        private SortFrame latest;

        SortTrace(int interval) {
            this.interval = Math.max(1, interval);
        }

        void add(int[] values, int highlightA, int highlightB, int sortedStart, String note) {
            operations++;
            latest = new SortFrame(values, highlightA, highlightB, sortedStart, note);
            if (frames.size() < SORT_MAX_FRAMES && operations % interval == 0) {
                frames.add(latest);
            }
        }

        void addAlways(int[] values, int highlightA, int highlightB, int sortedStart, String note) {
            latest = new SortFrame(values, highlightA, highlightB, sortedStart, note);
            if (frames.isEmpty() || frames.size() < SORT_MAX_FRAMES) {
                frames.add(latest);
            } else {
                frames.set(frames.size() - 1, latest);
            }
        }

        SortFrame lastFrame() {
            return latest == null ? frames.get(frames.size() - 1) : latest;
        }

        List<SortFrame> frames() {
            return frames;
        }
    }

    private static class SearchFrame {
        final String current;
        final List<String> frontier;
        final List<String> visited;
        final String note;

        SearchFrame(String current, List<String> frontier, List<String> visited, String note) {
            this.current = current;
            this.frontier = new ArrayList<>(frontier);
            this.visited = new ArrayList<>(visited);
            this.note = note;
        }
    }

    private static class NodeCost implements Comparable<NodeCost> {
        final String node;
        final int cost;

        NodeCost(String node, int cost) {
            this.node = node;
            this.cost = cost;
        }

        @Override
        public int compareTo(NodeCost other) {
            int costCompare = Integer.compare(this.cost, other.cost);
            if (costCompare != 0) {
                return costCompare;
            }
            return this.node.compareTo(other.node);
        }
    }

    private static String joinIntegers(List<Integer> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(" + ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static double round(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.3f", value));
    }
}
