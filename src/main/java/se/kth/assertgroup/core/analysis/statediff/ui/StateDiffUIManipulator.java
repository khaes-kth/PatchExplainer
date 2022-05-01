package se.kth.assertgroup.core.analysis.statediff.ui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import se.kth.assertgroup.core.analysis.statediff.computer.StateDiffComputer;
import se.kth.assertgroup.core.analysis.statediff.models.ProgramStateDiff;
import se.kth.assertgroup.core.analysis.statediff.models.SelectedTest;
import se.kth.assertgroup.core.analysis.statediff.models.SrcLineVars;
import se.kth.assertgroup.core.analysis.statediff.utils.ExecDiffHelper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class StateDiffUIManipulator {
    private static final File STATE_DIFF_WIDGET_TEMPLATE = new File("src/main/resources/state_diff/state_diff_widget.html");

    public void addStateDiffToExecDiffUI
            (
                    File leftBreakPoint,
                    File rightBreakPoint,
                    File leftReturn,
                    File rightReturn,
                    File srcFile,
                    File dstFile,
                    File ghFullDiff,
                    String testName,
                    String testLink
            ) throws Exception {
        Pair<Map<Integer, Integer>, Map<Integer, Integer>> mappings = ExecDiffHelper.getMappingFromExecDiff(ghFullDiff);

        SrcLineVars srcLineVars = new SrcLineVars(srcFile), dstLineVars = new SrcLineVars(dstFile);

        StateDiffComputer sdc = new StateDiffComputer(leftBreakPoint, rightBreakPoint, leftReturn, rightReturn,
                mappings.getLeft(), mappings.getRight(), srcLineVars.getLineVars(), dstLineVars.getLineVars());

        ProgramStateDiff psd = sdc.computeProgramStateDiff();

//        ProgramStateDiff psd = new ProgramStateDiff();
//        psd.setFirstOnlyOriginalState(Pair.of(372, "id=\"TestDTZ1\""));
//        psd.setFirstOnlyPatchedState(Pair.of(369, "next.iWallOffset=3600000"));

        addStateDiffToExecDiffUI(psd, ghFullDiff, new SelectedTest(testName, testLink));
    }

    private void addStateDiffToExecDiffUI
            (
                    ProgramStateDiff stateDiff,
                    File ghFullDiff,
                    SelectedTest test
            )
            throws Exception {
        if(stateDiff.getFirstOriginalUniqueStateInfo().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getFirstOriginalUniqueStateInfo(), "patched", ghFullDiff, test);
        }

        if(stateDiff.getFirstPatchedUniqueStateInfo().getFirstUniqueVarVal() != null){
            addStateDiffToExecDiffUI(stateDiff.getFirstPatchedUniqueStateInfo(), "original", ghFullDiff, test);
        }
    }

    private void addStateDiffToExecDiffUI
            (
                    ProgramStateDiff.UniqueStateInfo toBeShownDiff,
                    String versionName,
                    File ghFullDiff,
                    SelectedTest test
            ) throws Exception {
        String stateDiffHtml = FileUtils.readFileToString(STATE_DIFF_WIDGET_TEMPLATE, "UTF-8");
        stateDiffHtml = stateDiffHtml.replace("{{line-num}}", toBeShownDiff.getFirstUniqueVarValLine().toString())
                .replace("{{test-link}}", test.getTestLink())
                .replace("{{test-name}}", test.getTestName())
                .replace("{{unique-state}}", toBeShownDiff.getFirstUniqueVarVal())
                .replace("{{unique-state-version}}", versionName);
        ExecDiffHelper.addLineInfoAfter(toBeShownDiff.getFirstUniqueVarValLine(), stateDiffHtml, ghFullDiff);
    }

    public static void main(String[] args) throws Exception {
        new StateDiffUIManipulator().addStateDiffToExecDiffUI(
                new File("src/test/resources/sahab_reports/simple_two/breakpoint/left.json"),
                new File("src/test/resources/sahab_reports/simple_two/breakpoint/right.json"),
                null, null,
                new File("/home/khaes/phd/projects/explanation/code/tmp/old-src/DateTimeZoneBuilder.java"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/new-src/DateTimeZoneBuilder.java"),
                new File("/home/khaes/phd/projects/explanation/code/tmp/gh_full_b2.html"),
                "org.joda.time.TestPartial_Basics::testWith_baseAndArgHaveNoRange",
                "http://example.com");
    }
}
