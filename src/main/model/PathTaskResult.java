package model;

import java.util.ArrayList;
import java.util.List;

public class PathTaskResult {
    public final List<PathCaseResult> pathCaseResults;
    public final PathComparisonResult comparisonResult;

    public PathTaskResult(List<PathCaseResult> pathCaseResults,
                          PathComparisonResult comparisonResult) {
        this.pathCaseResults = new ArrayList<>(pathCaseResults);
        this.comparisonResult = comparisonResult;
    }
}
