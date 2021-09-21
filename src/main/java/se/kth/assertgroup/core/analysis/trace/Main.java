package se.kth.assertgroup.core.analysis.trace;

import java.io.File;

public class Main {
    public static void main(String args[]) throws Exception {
        String slug = args[0], commit = args[1], originalPath = args[2], patchedPath = args[3], outputDir = args[4],
                linkToFull = args[5];

        new TraceAnalyzer()
                .generateTraceDiffsForGHCommit(slug, commit, new File(originalPath), new File(patchedPath),
                        new File(outputDir), linkToFull);
    }
}
