package se.kth.assertgroup.core.analysis.statediff.ui;

import com.github.gumtreediff.matchers.Mapping;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import se.kth.assertgroup.core.analysis.sharedutils.GHHelper;
import se.kth.assertgroup.core.analysis.statediff.computer.StateDiffComputer;
import se.kth.assertgroup.core.analysis.statediff.models.ProgramStateDiff;
import se.kth.assertgroup.core.analysis.statediff.models.SelectedTest;
import se.kth.assertgroup.core.analysis.models.SourceInfo;
import se.kth.assertgroup.core.analysis.statediff.utils.ExecDiffHelper;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
        long processStartTime = new Date().getTime();
        boolean isHitDataIncluded = ghFullDiff != null;

        Map<Integer, Integer> returnSrcToDstMappings = new HashMap<>(),
                returnDstToSrcMappings = new HashMap<>();

        extractReturnMappings(srcFile, dstFile, returnSrcToDstMappings, returnDstToSrcMappings);

        SourceInfo srcInfo = new SourceInfo(srcFile), dstInfo = new SourceInfo(dstFile);

        if(ghFullDiff == null){
            ghFullDiff = GHHelper.getGHDiff(slug, commit, srcInfo, dstInfo);
        }

        Pair<Map<Integer, Integer>, Map<Integer, Integer>> lineMappings =
                ExecDiffHelper.getMappingFromExecDiff(ghFullDiff, isHitDataIncluded);

        lineMappings.getLeft().putAll(returnSrcToDstMappings);
        lineMappings.getRight().putAll(returnDstToSrcMappings);


        logger.info("Line mappings and vars computed.");

        StateDiffComputer sdc = new StateDiffComputer(leftReport, rightReport,
                lineMappings.getLeft(), lineMappings.getRight(), srcInfo.getLineVars(), dstInfo.getLineVars());

        ProgramStateDiff psd = sdc.computeProgramStateDiff();

        logger.info(psd.toString());

        long endOfDiffComputationTime = new Date().getTime();

        logger.info("For commit " + commit + " diff computation took "
                + (endOfDiffComputationTime - processStartTime) + " MILLIS");

        addStateDiffToExecDiffUI(psd, ghFullDiff, new SelectedTest(testName, testLink), isHitDataIncluded);

        long endOfUIManipulationTime = new Date().getTime();

        logger.info("For commit " + commit + " UI manipulation took "
                + (endOfUIManipulationTime - endOfDiffComputationTime) + " MILLIS");

        File outputFile = new File(outputPath);
        outputFile.createNewFile();

        FileUtils.copyFile(ghFullDiff, outputFile);
    }

    private void extractReturnMappings(File srcFile, File dstFile, Map<Integer, Integer> returnSrcToDstMappings, Map<Integer, Integer> returnDstToSrcMappings) throws Exception {
        Diff diff = new AstComparator().compare(srcFile, dstFile);
        Iterator<Mapping> mappings = diff.getMappingsComp().iterator();
        while(mappings.hasNext()){
            Mapping mapping = mappings.next();
            if(mapping.first.getMetadata("spoon_object") instanceof CtReturn
                    && mapping.second.getMetadata("spoon_object") instanceof CtReturn){
                CtReturn srcElem = (CtReturn) mapping.first.getMetadata("spoon_object"),
                        dstElem = (CtReturn) mapping.second.getMetadata("spoon_object");
                int srcLine = srcElem.getPosition().getLine(),
                        dstLine = dstElem.getPosition().getLine();

                returnSrcToDstMappings.put(srcLine, dstLine);
                returnDstToSrcMappings.put(dstLine, srcLine);
            }
        }
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
                "cbe88f725f5925bf82edf1cd665f6fa398da77ff",
                new File("/home/khaes/phd/projects/explanation/code/tmp/output/sahab-reports/cbe88f725f5925bf82edf1cd665f6fa398da77ff/left.json"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/output/sahab-reports/cbe88f725f5925bf82edf1cd665f6fa398da77ff/right.json"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/old-src/ZeroIsMaxDateTimeField.java"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/new-src/ZeroIsMaxDateTimeField.java"),
                null,
                "org.joda.time.TestPartial_Basics::testWith3",
                "http://example.com",
                "/home/khaes/phd/projects/explanation/code/tmp/output/state_diff_patch5-Time-4-Cardumen.html");
    }
}
