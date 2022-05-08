package se.kth.assertgroup.core.analysis.statediff.ui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import se.kth.assertgroup.core.analysis.statediff.computer.StateDiffComputer;
import se.kth.assertgroup.core.analysis.statediff.models.ProgramStateDiff;
import se.kth.assertgroup.core.analysis.statediff.models.SelectedTest;
import se.kth.assertgroup.core.analysis.statediff.models.SrcLineVars;
import se.kth.assertgroup.core.analysis.statediff.utils.ExecDiffHelper;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public class StateDiffUIManipulator {
    private static final Logger logger = Logger.getLogger(StateDiffUIManipulator.class.getName());
//    private static final File STATE_DIFF_WIDGET_TEMPLATE = new File("src/main/resources/state_diff/state_diff_widget.html");
    private static final File STATE_DIFF_WIDGET_TEMPLATE =
        new File("/home/khaes/phd/projects/explanation/code/Explainer/src/main/resources/state_diff/state_diff_widget.html");

    public void addStateDiffToExecDiffUI
            (
                    File leftReport,
                    File rightReport,
                    File srcFile,
                    File dstFile,
                    File ghFullDiff,
                    String testName,
                    String testLink
            ) throws Exception {
        Pair<Map<Integer, Integer>, Map<Integer, Integer>> mappings = ExecDiffHelper.getMappingFromExecDiff(ghFullDiff);

        SrcLineVars srcLineVars = new SrcLineVars(srcFile), dstLineVars = new SrcLineVars(dstFile);

        logger.info("Line mappings and vars computed.");

        StateDiffComputer sdc = new StateDiffComputer(leftReport, rightReport,
                mappings.getLeft(), mappings.getRight(), srcLineVars.getLineVars(), dstLineVars.getLineVars());

        ProgramStateDiff psd = sdc.computeProgramStateDiff();

        logger.info(psd.toString());

        addStateDiffToExecDiffUI(psd, ghFullDiff, new SelectedTest(testName, testLink));
    }

    private void addStateDiffToExecDiffUI
            (
                    ProgramStateDiff stateDiff,
                    File ghFullDiff,
                    SelectedTest test
            )
            throws Exception {
        if(stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarVal(),
                    stateDiff.getFirstOriginalUniqueStateSummary().getFirstUniqueVarValLine(),
                    "variable", true, ghFullDiff, test);
        }

        if(stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarVal(),
                    stateDiff.getFirstPatchedUniqueStateSummary().getFirstUniqueVarValLine(),
                    "variable", false, ghFullDiff, test);
        }

        if(stateDiff.getOriginalUniqueReturn().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getOriginalUniqueReturn().getFirstUniqueVarVal(),
                    stateDiff.getOriginalUniqueReturn().getFirstUniqueVarValLine(),
                    "return", true, ghFullDiff, test);
        }

        if(stateDiff.getPatchedUniqueReturn().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getPatchedUniqueReturn().getFirstUniqueVarVal(),
                    stateDiff.getPatchedUniqueReturn().getFirstUniqueVarValLine(),
                    "return", false, ghFullDiff, test);
        }
    }

    private void addStateDiffToExecDiffUI
            (
                    String diffStr,
                    Integer diffLine,
                    String diffType,
                    boolean occursInOriginal,
                    File ghFullDiff,
                    SelectedTest test
            ) throws Exception {
        String stateDiffHtml = FileUtils.readFileToString(STATE_DIFF_WIDGET_TEMPLATE, "UTF-8");
        stateDiffHtml = stateDiffHtml.replace("{{line-num}}", diffLine.toString())
                .replace("{{diff-type}}", diffType)
                .replace("{{test-link}}", test.getTestLink())
                .replace("{{test-name}}", test.getTestName())
                .replace("{{unique-state}}", diffStr)
                .replace("{{unique-state-version}}", occursInOriginal ? "original" : "patched");
        ExecDiffHelper.addLineInfoAfter(diffLine, stateDiffHtml, ghFullDiff, occursInOriginal);
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
                new File("/home/khaes/phd/projects/explanation/code/tmp/output/sahab-reports/75093e2c7b15373ef75dfeb8ab2e9ce16562fce5/left.json"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/output/sahab-reports/75093e2c7b15373ef75dfeb8ab2e9ce16562fce5/right.json"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/old-src/MonthDay.java"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/new-src/MonthDay.java"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/output/gh_full_patch1-Time-14-Arja-plausible.html"),
                "org.joda.time.TestMonthDay_Basics::testPlusMonths_int_negativeFromLeap",
                "http://example.com");
    }
}
