package se.kth.assertgroup.core.analysis;

import picocli.CommandLine;
import se.kth.assertgroup.core.analysis.statediff.StateDiffCommand;
import se.kth.assertgroup.core.analysis.trace.ExecFreqDiffCommand;

@CommandLine.Command(
        name = Constants.EXEC_DIFF_COMMAND_NAME,
        mixinStandardHelpOptions = true,
        subcommands = {ExecFreqDiffCommand.class, StateDiffCommand.class},
        description =
                "The EXEC-DIFF command line tool for generating exec-frequency report and adding state diff info to it.",
        synopsisSubcommandLabel = "<COMMAND>")
public class ExecDiffMain {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ExecDiffMain()).execute(args);
        System.exit(exitCode);
    }
}
