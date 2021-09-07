package se.kth.assertgroup.core.analysis.trace;

import java.io.File;

public class Main {
    public static void main(String args[]) throws Exception {
        String originalPath = args[0], patchedPath = args[1], outputDir = args[2], targetFiles = args[3],
                reportConfigStr = args[4];

        new TraceAnalyzer(new File(originalPath), new File(patchedPath))
                .generatedTraceDiff(new File(outputDir), targetFiles, reportConfigStr);
    }
}
