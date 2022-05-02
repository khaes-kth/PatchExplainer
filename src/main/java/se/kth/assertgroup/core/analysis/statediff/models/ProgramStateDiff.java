package se.kth.assertgroup.core.analysis.statediff.models;

public class ProgramStateDiff {
    private UniqueStateSummary firstOriginalUniqueStateSummary, firstPatchedUniqueStateSummary;
    private UniqueReturnSummary originalUniqueReturn, patchedUniqueReturn;

    public UniqueStateSummary getFirstOriginalUniqueStateSummary() {
        return firstOriginalUniqueStateSummary;
    }

    public void setFirstOriginalUniqueStateSummary(UniqueStateSummary firstOriginalUniqueStateSummary) {
        this.firstOriginalUniqueStateSummary = firstOriginalUniqueStateSummary;
    }

    public UniqueStateSummary getFirstPatchedUniqueStateSummary() {
        return firstPatchedUniqueStateSummary;
    }

    public void setFirstPatchedUniqueStateSummary(UniqueStateSummary firstPatchedUniqueStateSummary) {
        this.firstPatchedUniqueStateSummary = firstPatchedUniqueStateSummary;
    }

    public UniqueReturnSummary getOriginalUniqueReturn() {
        return originalUniqueReturn;
    }

    public void setOriginalUniqueReturn(UniqueReturnSummary originalUniqueReturn) {
        this.originalUniqueReturn = originalUniqueReturn;
    }

    public UniqueReturnSummary getPatchedUniqueReturn() {
        return patchedUniqueReturn;
    }

    public void setPatchedUniqueReturn(UniqueReturnSummary patchedUniqueReturn) {
        this.patchedUniqueReturn = patchedUniqueReturn;
    }

    public static class UniqueReturnSummary {
        private Integer firstUniqueVarValLine, firstUniqueReturnLine, firstUniqueReturnHash;
        private String firstUniqueVarVal;

        public Integer getFirstUniqueVarValLine() {
            return firstUniqueVarValLine;
        }

        public void setFirstUniqueVarValLine(Integer firstUniqueVarValLine) {
            this.firstUniqueVarValLine = firstUniqueVarValLine;
        }

        public String getFirstUniqueVarVal() {
            return firstUniqueVarVal;
        }

        public void setFirstUniqueVarVal(String firstUniqueVarVal) {
            this.firstUniqueVarVal = firstUniqueVarVal;
        }

        public Integer getFirstUniqueReturnHash() {
            return firstUniqueReturnHash;
        }

        public void setFirstUniqueReturnHash(Integer firstUniqueReturnHash) {
            this.firstUniqueReturnHash = firstUniqueReturnHash;
        }

        public Integer getFirstUniqueReturnLine() {
            return firstUniqueReturnLine;
        }

        public void setFirstUniqueReturnLine(Integer firstUniqueReturnLine) {
            this.firstUniqueReturnLine = firstUniqueReturnLine;
        }
    }

    public static class UniqueStateSummary {
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
