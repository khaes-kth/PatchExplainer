package se.kth.assertgroup.core.analysis.statediff;

import picocli.CommandLine;
import se.kth.assertgroup.core.analysis.Constants;
import se.kth.assertgroup.core.analysis.statediff.ui.StateDiffUIManipulator;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = Constants.STATE_DIFF_COMMAND_NAME,
        mixinStandardHelpOptions = true,
        description = "Generate execution frequency report for a given commit.")
public class StateDiffCommand implements Callable<Integer> {
    @CommandLine.Option(
            names = {Constants.ARG_LEFT_REPORT_PATH},
            description = "The path to the left report file.")
    File leftReport;

    @CommandLine.Option(
            names = {Constants.ARG_RIGHT_REPORT_PATH},
            description = "The path to the right report file.")
    File rightReport;

    @CommandLine.Option(
            names = {Constants.ARG_LEFT_SRC_PATH},
            description = "The path to the left src file.")
    File leftSrc;

    @CommandLine.Option(
            names = {Constants.ARG_RIGHT_SRC_PATH},
            description = "The path to the right src file.")
    File rightSrc;

    @CommandLine.Option(
            names = {Constants.ARG_TRACE_DIFF_FULL_REPORT_PATH},
            description = "The path to the trace diff full report file.")
    File traceDiffFullReport;

    @CommandLine.Option(
            names = {Constants.ARG_SELECTED_TESTS},
            description = "The name of the test.")
    String testName;

    @CommandLine.Option(
            names = {Constants.ARG_TEST_LINK},
            description = "The link to the test code.")
    String testLink;

    @Override
    public Integer call() throws Exception {

        new StateDiffUIManipulator().addStateDiffToExecDiffUI(
                leftReport,
                rightReport,
                leftSrc,
                rightSrc,
                traceDiffFullReport,
                testName,
                testLink);
        return 0;
    }
}
