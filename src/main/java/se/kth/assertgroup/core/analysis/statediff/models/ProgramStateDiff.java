package se.kth.assertgroup.core.analysis.statediff.models;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class ProgramStateDiff {
    private Pair<Integer, String> firstOnlyOriginalState, firstOnlyPatchedState;
    private Map<Integer, String> onlyOriginalReturnPerLine, onlyPatchedReturnPerLine;

    public ProgramStateDiff(){
        onlyOriginalReturnPerLine = new HashMap<>();
        onlyPatchedReturnPerLine = new HashMap<>();
    }

    public Map<Integer, String> getOnlyPatchedReturnPerLine() {
        return onlyPatchedReturnPerLine;
    }

    public void setOnlyPatchedReturnPerLine(Map<Integer, String> onlyPatchedReturnPerLine) {
        this.onlyPatchedReturnPerLine = onlyPatchedReturnPerLine;
    }

    public Map<Integer, String> getOnlyOriginalReturnPerLine() {
        return onlyOriginalReturnPerLine;
    }

    public void setOnlyOriginalReturnPerLine(Map<Integer, String> onlyOriginalReturnPerLine) {
        this.onlyOriginalReturnPerLine = onlyOriginalReturnPerLine;
    }

    public Pair<Integer, String> getFirstOnlyPatchedState() {
        return firstOnlyPatchedState;
    }

    public void setFirstOnlyPatchedState(Pair<Integer, String> firstOnlyPatchedState) {
        this.firstOnlyPatchedState = firstOnlyPatchedState;
    }

    public Pair<Integer, String> getFirstOnlyOriginalState() {
        return firstOnlyOriginalState;
    }

    public void setFirstOnlyOriginalState(Pair<Integer, String> firstOnlyOriginalState) {
        this.firstOnlyOriginalState = firstOnlyOriginalState;
    }
}
