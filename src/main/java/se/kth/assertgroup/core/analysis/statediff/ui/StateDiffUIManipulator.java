package se.kth.assertgroup.core.analysis.statediff.ui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import se.kth.assertgroup.core.analysis.sharedutils.GHHelper;
import se.kth.assertgroup.core.analysis.statediff.computer.StateDiffComputer;
import se.kth.assertgroup.core.analysis.statediff.models.ProgramStateDiff;
import se.kth.assertgroup.core.analysis.statediff.models.SelectedTest;
import se.kth.assertgroup.core.analysis.models.SourceInfo;
import se.kth.assertgroup.core.analysis.statediff.utils.ExecDiffHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;

public class StateDiffUIManipulator {
    private static final Logger logger = Logger.getLogger(StateDiffUIManipulator.class.getName());
    private static File STATE_DIFF_WIDGET_TEMPLATE;
//    private static final File STATE_DIFF_WIDGET_TEMPLATE =
//        new File("/home/khaes/phd/projects/explanation/code/Explainer/src/main/resources/state_diff/state_diff_widget.html");

    static {
        try {
            STATE_DIFF_WIDGET_TEMPLATE = Files.createTempFile("", "state_diff_widget.html").toFile();
            FileUtils.copyInputStreamToFile(
                    StateDiffUIManipulator.class.getClassLoader().getResourceAsStream("state_diff_widget.html"),
                    STATE_DIFF_WIDGET_TEMPLATE);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temp widget file.");
        }
    }

    public void addStateDiffToExecDiffUI
            (
                    String slug,
                    String commit,
                    File leftReport,
                    File rightReport,
                    File srcFile,
                    File dstFile,
                    File ghFullDiff,
                    String testName,
                    String testLink,
                    String outputPath
            ) throws Exception {
        boolean isHitDataIncluded = ghFullDiff != null;

        SourceInfo srcInfo = new SourceInfo(srcFile), dstInfo = new SourceInfo(dstFile);

        if(ghFullDiff == null){
            ghFullDiff = GHHelper.getGHDiff(slug, commit, srcInfo, dstInfo);
        }

        Pair<Map<Integer, Integer>, Map<Integer, Integer>> mappings =
                ExecDiffHelper.getMappingFromExecDiff(ghFullDiff, isHitDataIncluded);


        logger.info("Line mappings and vars computed.");

        StateDiffComputer sdc = new StateDiffComputer(leftReport, rightReport,
                mappings.getLeft(), mappings.getRight(), srcInfo.getLineVars(), dstInfo.getLineVars());

        ProgramStateDiff psd = sdc.computeProgramStateDiff();

        logger.info(psd.toString());

        addStateDiffToExecDiffUI(psd, ghFullDiff, new SelectedTest(testName, testLink), isHitDataIncluded);

        File outputFile = new File(outputPath);
        outputFile.createNewFile();

        FileUtils.copyFile(ghFullDiff, outputFile);
    }

    private void addStateDiffToExecDiffUI
            (
                    ProgramStateDiff stateDiff,
                    File ghFullDiff,
                    SelectedTest test,
                    boolean isHitDataIncluded
            )
            throws Exception {
        if(stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal(),
                    stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarValLine(),
                    "variable", true, ghFullDiff, test, isHitDataIncluded);
        }

        if(stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal(),
                    stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarValLine(),
                    "variable", false, ghFullDiff, test, isHitDataIncluded);
        }

        if(stateDiff.getOriginalUniqueReturn().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getOriginalUniqueReturn().getFirstUniqueVarVal(),
                    stateDiff.getOriginalUniqueReturn().getFirstUniqueVarValLine(),
                    "return", true, ghFullDiff, test, isHitDataIncluded);
        }

        if(stateDiff.getPatchedUniqueReturn().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getPatchedUniqueReturn().getFirstUniqueVarVal(),
                    stateDiff.getPatchedUniqueReturn().getFirstUniqueVarValLine(),
                    "return", false, ghFullDiff, test, isHitDataIncluded);
        }
    }

    private void addStateDiffToExecDiffUI
            (
                    String diffStr,
                    Integer diffLine,
                    String diffType,
                    boolean occursInOriginal,
                    File ghFullDiff,
                    SelectedTest test,
                    boolean isHitDataIncluded
            ) throws Exception {
        String stateDiffHtml = FileUtils.readFileToString(STATE_DIFF_WIDGET_TEMPLATE, "UTF-8");
        stateDiffHtml = stateDiffHtml.replace("{{line-num}}", diffLine.toString())
                .replace("{{diff-type}}", diffType)
                .replace("{{test-link}}", test.getTestLink())
                .replace("{{test-name}}", test.getTestName())
                .replace("{{unique-state}}", diffStr)
                .replace("{{unique-state-version}}", occursInOriginal ? "original" : "patched");
        ExecDiffHelper.addLineInfoAfter(diffLine, stateDiffHtml, ghFullDiff, occursInOriginal, isHitDataIncluded);
    }

    public static void main(String[] args) throws Exception {
//        String leftReportPath = args[0], rightReportPath = args[1], leftSrcPath = args[2], rightSrcPath = args[3],
//                diffHtmlPath = args[4], testName = args[5], testLink = args[6];
//        new StateDiffUIManipulator().addStateDiffToExecDiffUI(
//                new File(leftReportPath),
//                new File(rightReportPath),
//                new File(leftSrcPath),
//                new File(rightSrcPath),
//                new File(diffHtmlPath),
//                testName,
//                testLink);

        new StateDiffUIManipulator().addStateDiffToExecDiffUI(
                "khaes-kth/drr-execdiff",
                "8b5b580751d1c08eb848e389ec3e7e235eea62d8",
                new File("/home/khaes/phd/projects/explanation/code/tmp/output/sahab-reports/8b5b580751d1c08eb848e389ec3e7e235eea62d8/left.json"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/output/sahab-reports/8b5b580751d1c08eb848e389ec3e7e235eea62d8/right.json"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/old-src/DateTimeZoneBuilder.java"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/new-src/DateTimeZoneBuilder.java"),
                null,
                "org.joda.time.tz.TestCompiler::testDateTimeZoneBuilder",
                "http://example.com",
                "/home/khaes/phd/projects/explanation/code/tmp/output/state_diff_patch1-Time-11-Cardumen.html");
    }
}
