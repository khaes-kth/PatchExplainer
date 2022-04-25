package se.kth.assertgroup.core.analysis.satediff.computer;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.kth.assertgroup.core.analysis.statediff.computer.StateDiffComputer;
import se.kth.assertgroup.core.analysis.statediff.models.ProgramStateDiff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class StateDiffComputerTest {

    // breakpoint from https://github.com/khaes-kth/drr-execdiff/commit/649e9234549d2c74f1279499011da87638c2d718
    @Test
    void computeStateDiff_simple_diffIsGenerated(@TempDir Path tempDir) throws IOException, ParseException {
        Path simpleSahabDirectory = Paths.get("src/test/resources/sahab_reports/simple");
        File leftBreakpoint = simpleSahabDirectory.resolve("breakpoint/left.json").toFile(),
                rightBreakpoint = simpleSahabDirectory.resolve("breakpoint/right.json").toFile(),
                leftReturn = simpleSahabDirectory.resolve("return/right.json").toFile(),
                rightReturn = simpleSahabDirectory.resolve("return/right.json").toFile(),
                lineMapping = simpleSahabDirectory.resolve("project_data/line_mapping.csv").toFile(),
                left_line_to_var = simpleSahabDirectory.resolve("project_data/left_line_to_var.csv").toFile(),
                rightLineToVar = simpleSahabDirectory.resolve("project_data/right_line_to_var.csv").toFile();

        StateDiffComputer sdc = new StateDiffComputer(
                leftBreakpoint,
                rightBreakpoint,
                leftReturn,
                rightReturn,
                lineMapping,
                left_line_to_var,
                rightLineToVar);

        ProgramStateDiff stateDiff = sdc.computeProgramStateDiff();

        assertNotNull(stateDiff.getFirstOnlyOriginalState());
        assertNull(stateDiff.getFirstOnlyPatchedState());
    }

    // breakpoint from: https://github.com/khaes-kth/drr-execdiff/commit/4513e8f2ce6436f8bf3c3fbf403b11b3deb4d157, depth=1
    @Test
    void computeStateDiff_complex_diffIsGenerated(@TempDir Path tempDir) throws IOException, ParseException {
        Path simpleSahabDirectory = Paths.get("src/test/resources/sahab_reports/complex");
        File leftBreakpoint = simpleSahabDirectory.resolve("breakpoint/left.json").toFile(),
                rightBreakpoint = simpleSahabDirectory.resolve("breakpoint/right.json").toFile(),
                leftReturn = simpleSahabDirectory.resolve("return/right.json").toFile(),
                rightReturn = simpleSahabDirectory.resolve("return/right.json").toFile(),
                lineMapping = simpleSahabDirectory.resolve("project_data/line_mapping.csv").toFile(),
                left_line_to_var = simpleSahabDirectory.resolve("project_data/left_line_to_var.csv").toFile(),
                rightLineToVar = simpleSahabDirectory.resolve("project_data/right_line_to_var.csv").toFile();

        StateDiffComputer sdc = new StateDiffComputer(
                leftBreakpoint,
                rightBreakpoint,
                leftReturn,
                rightReturn,
                lineMapping,
                left_line_to_var,
                rightLineToVar);

        ProgramStateDiff stateDiff = sdc.computeProgramStateDiff();

        assertNull(stateDiff.getFirstOnlyOriginalState());
        assertNull(stateDiff.getFirstOnlyPatchedState());
    }
}
