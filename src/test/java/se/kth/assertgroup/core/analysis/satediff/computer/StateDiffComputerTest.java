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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StateDiffComputerTest<R> {

    // breakpoint from https://github.com/khaes-kth/drr-execdiff/commit/649e9234549d2c74f1279499011da87638c2d718
    @Test
    void computeStateDiff_simple_diffIsGenerated() throws IOException, ParseException {
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

        assertNotNull(stateDiff.getFirstOnlyOriginalState().getValue().toString().equals("id=\"TestDTZ1\""));
        assertNull(stateDiff.getFirstOnlyPatchedState());
    }

    // breakpoint from: https://github.com/khaes-kth/drr-execdiff/commit/1c04679173a46faa59e73f68def33f60843f8beb, depth=1
    @Test
    void computeStateDiff_complex_diffIsGenerated() throws IOException, ParseException {
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

    // breakpoint from: https://github.com/khaes-kth/drr-execdiff/commit/8b5b580751d1c08eb848e389ec3e7e235eea62d8, depth=1
    // only a part of breakpoint data is stored in right.json
    @Test
    void computeStateDiff_simple_diffIsGenerated_two() throws IOException, ParseException {
        Path simpleSahabDirectory = Paths.get("src/test/resources/sahab_reports/simple_two");
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

        assertEquals(stateDiff.getFirstOnlyOriginalState().getValue(), "id=\"TestDTZ1\"");
        assertEquals(stateDiff.getFirstOnlyPatchedState().getValue(), "next.iWallOffset=3600000");
    }
}
