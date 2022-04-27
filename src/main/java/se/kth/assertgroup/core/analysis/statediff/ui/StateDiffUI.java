package se.kth.assertgroup.core.analysis.statediff.ui;

import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;
import se.kth.assertgroup.core.analysis.statediff.computer.StateDiffComputer;
import se.kth.assertgroup.core.analysis.statediff.models.ProgramStateDiff;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StateDiffUI {
    public void addStateDiffToGHDiffUI
            (
                    File leftBreakPoint,
                    File rightBreakPoint,
                    File leftReturn,
                    File rightReturn,
                    File lineMappingFile,
                    File leftLineToVarsFile,
                    File rightLineToVarsFile,
                    File ghFullDiff
            ) throws IOException, ParseException {

//        StateDiffComputer sdc = new StateDiffComputer(leftBreakPoint, rightBreakPoint, leftReturn, rightReturn,
//                lineMappingFile, leftLineToVarsFile, rightLineToVarsFile);
//
//        ProgramStateDiff psd = sdc.computeProgramStateDiff();

        List<String> lines = FileUtils.readLines(ghFullDiff, "UTF-8");


    }

    public static void main(String[] args) throws IOException, ParseException {
        new StateDiffUI().addStateDiffToGHDiffUI(null, null, null, null,
                null, null, null,
                new File("/home/khaes/phd/projects/explanation/code/tmp/gh_full.html"));
    }
}
