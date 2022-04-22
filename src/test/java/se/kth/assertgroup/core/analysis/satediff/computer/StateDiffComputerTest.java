package se.kth.assertgroup.core.analysis.satediff.computer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StateDiffComputerTest {

    @Test
    void computeStateDiff_diffIsGenerated(@TempDir Path tempDir) {
        Path simpleSahabDirectory = Paths.get("src/test/resources/sahab_reports/simple");
        File leftBreakpoint = simpleSahabDirectory.resolve("breakpoint/left.json").toFile(),
                rightBreakpoint = simpleSahabDirectory.resolve("breakpoint/right.json").toFile(),
                leftReturn = simpleSahabDirectory.resolve("return/right.json").toFile(),
                rightReturn = simpleSahabDirectory.resolve("return/right.json").toFile();

        for(int i = 1464; i <= 1482; i++)
            System.out.println(i + "," + (i - 3));
    }
}
