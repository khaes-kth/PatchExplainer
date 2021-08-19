package se.kth.assertgroup.core.analysis.trace.models;

import java.util.HashMap;
import java.util.Map;

public class LineCoverage extends HashMap<Integer, LineCoverage.CoverageStatus> {
    public enum CoverageStatus{
        COVERED,
        UNCOVERED
    }
}
