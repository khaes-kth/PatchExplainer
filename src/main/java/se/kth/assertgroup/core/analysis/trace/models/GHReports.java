package se.kth.assertgroup.core.analysis.trace.models;

public class GHReports {
    private String expandedReport, unexpandedReport;
    private boolean containsExecDiff;

    public GHReports(String expandedReport, String unexpandedReport, boolean containsExecDiff){
        this.expandedReport = expandedReport;
        this.unexpandedReport = unexpandedReport;
        this.containsExecDiff = containsExecDiff;
    }

    public String getExpandedReport() {
        return expandedReport;
    }

    public void setExpandedReport(String expandedReport) {
        this.expandedReport = expandedReport;
    }

    public String getUnexpandedReport() {
        return unexpandedReport;
    }

    public void setUnexpandedReport(String unexpandedReport) {
        this.unexpandedReport = unexpandedReport;
    }

    public boolean containsExecDiff() {
        return containsExecDiff;
    }

    public void setContainsExecDiff(boolean containsExecDiff) {
        this.containsExecDiff = containsExecDiff;
    }
}
