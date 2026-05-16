# CPT204 Group Project

This repository contains the source code and datasets for a CPT204 group
coursework project. The project evaluates several sorting algorithms and
compares shortest-path strategies on a weighted graph dataset.

## Project Overview

The project includes two main tasks:

- Sorting algorithm evaluation using Bubble Sort, Quick Sort, Merge Sort, and
  Heap Sort.
- Route planning and path comparison using Dijkstra's algorithm,
  Floyd-Warshall, dynamic programming for multi-via route optimisation, and BFS
  for unweighted comparison.

The program reads CSV datasets from the `resource/` folder, runs the algorithms,
and exports result files such as CSV tables and SVG visualisations when executed.

## Repository Structure

```text
.
├── README.md
├── resource/
│   └── Group Project Datasets/
│       ├── candidates_A.csv
│       ├── candidates_B.csv
│       ├── candidates_C.csv
│       └── paths.csv
└── src/
    ├── main/
    │   ├── Main.java
    │   ├── export/
    │   ├── graph/
    │   ├── io/
    │   ├── model/
    │   ├── sort/
    │   ├── util/
    │   └── workflow/
    ├── test/
    └── javafx/
```

## Requirements

- Java Development Kit 17 or later
- A terminal or IDE that can compile and run Java source files

The command-line version does not require Maven or Gradle.

## Compile

From the repository root, compile the command-line program:

```powershell
javac -d out (Get-ChildItem -Recurse src/main -Filter *.java).FullName
```

To also compile the test runner:

```powershell
javac -d out (Get-ChildItem -Recurse src/main,src/test -Filter *.java).FullName
```

If you are using Git Bash, WSL, macOS, or Linux, you can compile with:

```bash
javac -d out $(find src/main -name '*.java')
```

## Run

After compiling, run the main workflow:

```powershell
java -cp out Main
```

You can also pass the dataset folder explicitly:

```powershell
java -cp out Main "resource/Group Project Datasets"
```

## Run Tests

After compiling both main and test sources, run:

```powershell
java -cp out TestRunner
```

The test runner checks sorting correctness, Dijkstra shortest paths,
Floyd-Warshall paths, Floyd + DP via-order optimisation, BFS unweighted paths,
priority tie-breaking, and unreachable-path handling.

## Output Files

Running the program creates an `out/` directory locally. This folder contains
compiled `.class` files and generated coursework artifacts, including:

- `sorting_results.csv`
- `sorting_benchmark.svg`
- `sorting_benchmark_fast_algorithms.svg`
- `shortest_paths.csv`
- `case_*_dijkstra_path.svg`
- `case_*_floyd_dp_path.svg`
- `bfs_unweighted_comparison.csv`

The `out/` directory is generated automatically and does not need to be uploaded
to GitHub.

## JavaFX Dashboard

The repository includes JavaFX dashboard source files under `src/javafx/`.
These files are optional and are intended for visual presentation. To run the
dashboard from an IDE, configure a JavaFX SDK and run `Launcher` with JavaFX
module options similar to:

```text
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls
```

The GitHub version of this project only stores source code and datasets, so it
does not include a bundled Windows runtime.

## Notes

- `resource/` must stay in the repository because the program reads the CSV
  datasets from this folder.
- `out/`, `.class` files, IDE settings, logs, and bundled runtimes should not be
  committed.
- The recommended minimum GitHub upload is `src/`, `resource/`, and this
  `README.md`.

