package se.kth.assertgroup.core.analysis.trace;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import se.kth.assertgroup.core.analysis.trace.models.LineCoverage;
import se.kth.assertgroup.core.analysis.trace.utils.JacocoHelper;
import se.kth.assertgroup.core.analysis.trace.utils.SpoonHelper;
import se.kth.assertgroup.core.analysis.trace.models.LineMapping;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Given the original and patched mvn projects, a {@link TraceAnalyzer} generates the execution trace diff.
 */
public class TraceAnalyzer {
    private static final String FINAL_REPORT_TEMPLATE_PATH = "trace/final_report.html";
    private static final String FINAL_REPORT_PATH = "target/trace/final_report.html";
    private static final String FINAL_REPORT_ORIGINAL_COL_ID = "original-trace";
    private static final String FINAL_REPORT_PATCHED_COL_ID = "patched-trace";
    private static final String FINAL_REPORT_CODE_KEYWORD = "{code}";
    private static final String FINAL_REPORT_COLOR_KEYWORD = "{color}";
    private static final String TAB_HTML_CHAR = "&emsp;";
    private static final String TAB_CHAR = "\\\t";
    private static final String TAB_SPACE = "    ";
    private static final String FINAL_REPORT_LINE_TEMPLATE = "<span style=\"background-color: "
            + FINAL_REPORT_COLOR_KEYWORD + "\">" + FINAL_REPORT_CODE_KEYWORD + "</span>";

    private File originalMvnDir, patchedMvnDir;

    public TraceAnalyzer(File originalMvnDir, File patchedMvnDir) {
        this.originalMvnDir = originalMvnDir;
        this.patchedMvnDir = patchedMvnDir;
    }

    public void generatedTraceDiff(File outputDir, Path modifiedFilePath) throws Exception {
        LineMapping lineMapping =
                SpoonHelper.getDiff(originalMvnDir.toPath().resolve(modifiedFilePath).toFile(),
                        patchedMvnDir.toPath().resolve(modifiedFilePath).toFile());

        LineCoverage originalCoverage = JacocoHelper.getPerLineCoverage(originalMvnDir, modifiedFilePath);
        LineCoverage patchedCoverage = JacocoHelper.getPerLineCoverage(patchedMvnDir, modifiedFilePath);

        generateTraceDiff(outputDir, modifiedFilePath, lineMapping, originalCoverage, patchedCoverage);
    }

    private void generateTraceDiff
            (
                    File outputDir,
                    Path modifiedFilePath,
                    LineMapping lineMapping,
                    LineCoverage originalCoverage,
                    LineCoverage patchedCoverage
            ) throws IOException, URISyntaxException {
        List<String> originalLines =
                FileUtils.readLines(originalMvnDir.toPath().resolve(modifiedFilePath).toFile(), "UTF-8"),
                patchedLines =
                        FileUtils.readLines(patchedMvnDir.toPath().resolve(modifiedFilePath).toFile(), "UTF-8");

        Document reportDoc =
                Jsoup.parse(new File(TraceAnalyzer.class.getClassLoader().getResource(FINAL_REPORT_TEMPLATE_PATH).toURI()), "UTF-8");

        Element originalCol = reportDoc.getElementById(FINAL_REPORT_ORIGINAL_COL_ID),
                patchedCol = reportDoc.getElementById(FINAL_REPORT_PATCHED_COL_ID);

        fillReportColumn(lineMapping.getSrcToDst(), lineMapping.getSrcNewLines(), originalCoverage, patchedCoverage,
                originalLines, originalCol);
        fillReportColumn(lineMapping.getDstToSrc(), lineMapping.getDstNewLines(), patchedCoverage, originalCoverage,
                patchedLines, patchedCol);

        File finalReportFile = new File(Path.of(outputDir.getPath(), FINAL_REPORT_PATH).toString());
        finalReportFile.getParentFile().mkdirs();
        finalReportFile.createNewFile();
        String finalHtml = trimCodeString(reportDoc.toString());
        FileUtils.writeStringToFile(finalReportFile, finalHtml, "UTF-8");
    }

    private String trimCodeString(String originalStr) {
        String[] parts = originalStr.split("<pre>");
        String result = "";
        for(int i = 0; i < parts.length - 1; i++){
            result += parts[i].trim() + "<pre>";
        }
        result += parts[parts.length - 1].trim();
        return result;
    }

    private void fillReportColumn
            (
                    Map<Integer, Integer> lineNumberMapping,
                    Set<Integer> newLines,
                    LineCoverage sourceCoverage,
                    LineCoverage targetCoverage,
                    List<String> lines, Element colElem
            ) {
        for(int i = 0; i < lines.size(); i++){
            String reportLine = FINAL_REPORT_LINE_TEMPLATE.replace(FINAL_REPORT_CODE_KEYWORD, lines.get(i));
//            reportLine = reportLine.replaceAll(TAB_CHAR, TAB_HTML_CHAR);
//            reportLine = reportLine.replaceAll(TAB_SPACE, TAB_HTML_CHAR);
            int sourceLineNumber = i + 1;

            String backgroundColor = "white";
            if(lineNumberMapping.containsKey(sourceLineNumber)){
                int targetLineNumber = lineNumberMapping.get(sourceLineNumber);
                backgroundColor = sourceCoverage.get(sourceLineNumber) == targetCoverage.get(targetLineNumber) ?
                        (sourceCoverage.get(sourceLineNumber) == LineCoverage.CoverageStatus.COVERED ? "green" : "white")
                        : sourceCoverage.get(sourceLineNumber) == LineCoverage.CoverageStatus.COVERED ? "cyan" : "red";
            } else if(newLines.contains(sourceLineNumber)){
                backgroundColor = "yellow";
            }
            reportLine = reportLine.replace(FINAL_REPORT_COLOR_KEYWORD, backgroundColor);

            colElem.append(reportLine);
            colElem.append("<br>");
        }
    }

    public static void main(String[] args) throws Exception {
//        File sample1 = new File("/home/khaes/phd/projects/explanation/code/tmp/jtar");
//        File sample2 = new File("/home/khaes/phd/projects/explanation/code/tmp/jtar2");
//        new TraceAnalyzer(sample1, sample2).generatedTraceDiff(sample2, Path.of("src/main/java/org/kamranzafar/jtar/TarHeader.java"));

        File sample1 = new File("/home/khaes/phd/projects/explanation/code/tmp/TestProj");
        File sample2 = new File("/home/khaes/phd/projects/explanation/code/tmp/TestProj2");
        new TraceAnalyzer(sample1, sample2).generatedTraceDiff(sample2, Path.of("src/main/java/NumberAnalyzer.java"));

    }
}
