package se.kth.assertgroup.core.analysis.statediff.models;

import java.util.HashMap;
import java.util.Map;

public class ProgramStateDiff {
    private UniqueStateInfo firstOriginalUniqueStateInfo, firstPatchedUniqueStateInfo;
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

    public UniqueStateInfo getFirstOriginalUniqueStateInfo() {
        return firstOriginalUniqueStateInfo;
    }

    public void setFirstOriginalUniqueStateInfo(UniqueStateInfo firstOriginalUniqueStateInfo) {
        this.firstOriginalUniqueStateInfo = firstOriginalUniqueStateInfo;
    }

    public UniqueStateInfo getFirstPatchedUniqueStateInfo() {
        return firstPatchedUniqueStateInfo;
    }

    public void setFirstPatchedUniqueStateInfo(UniqueStateInfo firstPatchedUniqueStateInfo) {
        this.firstPatchedUniqueStateInfo = firstPatchedUniqueStateInfo;
    }

    public static class UniqueStateInfo {
        private Integer firstUniqueStateLine, firstUniqueVarValLine, firstUniqueStateHash;
        private String firstUniqueVarVal;

        public Integer getFirstUniqueStateLine() {
            return firstUniqueStateLine;
        }

        public void setFirstUniqueStateLine(Integer firstUniqueStateLine) {
            this.firstUniqueStateLine = firstUniqueStateLine;
        }

        public Integer getFirstUniqueVarValLine() {
            return firstUniqueVarValLine;
        }

        public void setFirstUniqueVarValLine(Integer firstUniqueVarValLine) {
            this.firstUniqueVarValLine = firstUniqueVarValLine;
        }

        public Integer getFirstUniqueStateHash() {
            return firstUniqueStateHash;
        }

        public void setFirstUniqueStateHash(Integer firstUniqueStateHash) {
            this.firstUniqueStateHash = firstUniqueStateHash;
        }

        public String getFirstUniqueVarVal() {
            return firstUniqueVarVal;
        }

        public void setFirstUniqueVarVal(String firstUniqueVarVal) {
            this.firstUniqueVarVal = firstUniqueVarVal;
        }
    }
}
