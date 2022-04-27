package se.kth.assertgroup.core.analysis.satediff.models;

import org.junit.jupiter.api.Test;
import se.kth.assertgroup.core.analysis.statediff.models.SrcLineVars;

import java.io.File;
import java.nio.file.Paths;

public class SrcLineVarsTest {

    @Test
    void computeLineVars_simple(){
        File src = Paths.get("src/test/resources/source/simple/DateTimeZoneBuilder.java").toFile();
        SrcLineVars slv = new SrcLineVars(src);
    }
}
