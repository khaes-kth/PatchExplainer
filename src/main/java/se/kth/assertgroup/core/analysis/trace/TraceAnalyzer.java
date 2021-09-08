package se.kth.assertgroup.core.analysis.trace;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import se.kth.assertgroup.core.analysis.trace.models.ReportConfig;
import se.kth.assertgroup.core.analysis.trace.utils.CloverHelper;
import se.kth.assertgroup.core.analysis.trace.utils.SpoonHelper;
import se.kth.assertgroup.core.analysis.trace.models.LineMapping;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

/**
 * Given the original and patched mvn projects, a {@link TraceAnalyzer} generates the execution trace diff.
 */
public class TraceAnalyzer {
    private static final String FINAL_REPORT_TEMPLATE_PATH = "/trace/final_report.html";
    private static final String FINAL_REPORT_DIR_PATH = "target/trace/";
    private static final String FINAL_REPORT_ORIGINAL_COL_ID = "original-trace";
    private static final String FINAL_REPORT_PATCHED_COL_ID = "patched-trace";
    private static final String FINAL_REPORT_CODE_KEYWORD = "{code}";
    private static final String FINAL_REPORT_COLOR_KEYWORD = "{color}";
    private static final String FINAL_REPORT_LINE_NUM_KEYWORD = "{line-num}";
    private static final String FINAL_REPORT_LINE_NUM_PLACEHOLDER = "            ";
    private static final String FINAL_REPORT_LINE_HIT_KEYWORD = "{line-hit}";
    private static final String FINAL_REPORT_LINE_HIT_PLACEHOLDER = "    ";
    private static final String FINAL_REPORT_LINE_TEMPLATE = FINAL_REPORT_LINE_NUM_KEYWORD + ":  "
            + "<span style=\"background-color: " + FINAL_REPORT_COLOR_KEYWORD + "\">"
            + FINAL_REPORT_LINE_HIT_KEYWORD + "  " + FINAL_REPORT_CODE_KEYWORD + "</span>";
    private static final String INPUT_STR_SEPARATOR = ";";
    private static final String FINAL_REPORT_FILE_NAME_SEPARATOR = "-";
    private static final String JAVA_SUFFIX = ".java";
    private static final String HTML_SUFFIX = ".html";
    private static final int MAX_DEMONSTRABLE_HIT = 99;

    private File originalMvnDir, patchedMvnDir;

    public TraceAnalyzer(File originalMvnDir, File patchedMvnDir) {
        this.originalMvnDir = originalMvnDir;
        this.patchedMvnDir = patchedMvnDir;
    }

    public void generatedTraceDiff(File outputDir, String modifiedFilePathsStr, String reportConfigsStr) throws Exception {
        if(modifiedFilePathsStr.isEmpty())
            return;

        List<String> modifiedFilePaths = Arrays.asList(modifiedFilePathsStr.split(INPUT_STR_SEPARATOR));
        Map<String, LineMapping> filePathToLineMapping = new HashMap<>();
        for (String path : modifiedFilePaths)
            filePathToLineMapping.put(path, SpoonHelper.getDiff(originalMvnDir.toPath().resolve(path).toFile(),
                    patchedMvnDir.toPath().resolve(path).toFile()));

        Map<String, Map<Integer, Integer>> originalCoverages =
                CloverHelper.getPerLineCoverage(originalMvnDir, modifiedFilePaths);
        Map<String, Map<Integer, Integer>> patchedCoverages =
                CloverHelper.getPerLineCoverage(patchedMvnDir, modifiedFilePaths);

        for (String path : modifiedFilePaths)
            for (String configStr : reportConfigsStr.split(INPUT_STR_SEPARATOR))
                generateTraceDiff(outputDir, Path.of(path), filePathToLineMapping.get(path),
                        originalCoverages.get(path), patchedCoverages.get(path), new ReportConfig(configStr));
    }

    private void generateTraceDiff
            (
                    File outputDir,
                    Path modifiedFilePath,
                    LineMapping lineMapping,
                    Map<Integer, Integer> originalCoverage,
                    Map<Integer, Integer> patchedCoverage,
                    ReportConfig reportConfig
            ) throws IOException, URISyntaxException {
        List<String> originalLines =
                FileUtils.readLines(originalMvnDir.toPath().resolve(modifiedFilePath).toFile(), "UTF-8"),
                patchedLines =
                        FileUtils.readLines(patchedMvnDir.toPath().resolve(modifiedFilePath).toFile(), "UTF-8");

        String finalReportTemplate = new Scanner(CloverHelper.class.getResourceAsStream(FINAL_REPORT_TEMPLATE_PATH),
                "UTF-8").useDelimiter("\\A").next();
        Document reportDoc = Jsoup.parse(finalReportTemplate);

        Element originalCol = reportDoc.getElementById(FINAL_REPORT_ORIGINAL_COL_ID),
                patchedCol = reportDoc.getElementById(FINAL_REPORT_PATCHED_COL_ID);

        fillReportColumn(lineMapping.getSrcToDst(), lineMapping.getSrcNewLines(), originalCoverage, patchedCoverage,
                originalLines, originalCol, reportConfig, true);
        fillReportColumn(lineMapping.getDstToSrc(), lineMapping.getDstNewLines(), patchedCoverage, originalCoverage,
                patchedLines, patchedCol, reportConfig, false);

        File finalReportFile = new File(Path.of(outputDir.getPath(), FINAL_REPORT_DIR_PATH,
                reportConfig.toString() + FINAL_REPORT_FILE_NAME_SEPARATOR
                        + modifiedFilePath.toString().replace(File.separator, FINAL_REPORT_FILE_NAME_SEPARATOR))
                .toString().replace(JAVA_SUFFIX, HTML_SUFFIX));
        finalReportFile.getParentFile().mkdirs();
        finalReportFile.createNewFile();
        String finalHtml = trimCodeString(reportDoc.toString());
        FileUtils.writeStringToFile(finalReportFile, finalHtml, "UTF-8");
    }

    private String trimCodeString(String originalStr) {
        // TODO: clean it!
        String[] parts = originalStr.split("trace\">");
        String result = "";
        for (int i = 0; i < parts.length - 1; i++) {
            result += (parts[i].startsWith("\n            ") ?
                    parts[i].replaceFirst("\n            ", "") : parts[i]) + "trace\">";
        }
        result += parts[parts.length - 1].replaceFirst("\n            ", "");
        return result;
    }

    private void fillReportColumn
            (
                    Map<Integer, Integer> lineNumberMapping,
                    Set<Integer> newLines,
                    Map<Integer, Integer> sourceCoverage,
                    Map<Integer, Integer> targetCoverage,
                    List<String> lines, Element colElem,
                    ReportConfig reportConfig,
                    boolean isSrc
            ) {
        for (int i = 0; i < lines.size(); i++) {
            int sourceLineNumber = i + 1;
            int targetLineNumber = lineNumberMapping.containsKey(sourceLineNumber) ?
                    lineNumberMapping.get(sourceLineNumber) : -1;

            int srcHitCnt = sourceCoverage.containsKey(sourceLineNumber) ? sourceCoverage.get(sourceLineNumber) : -1;
            String srcHitCntStr = srcHitCnt >= 0 ? (srcHitCnt <= MAX_DEMONSTRABLE_HIT ? srcHitCnt : MAX_DEMONSTRABLE_HIT + "+") + "": "";

            String lineNumStr = !isSrc ? (sourceLineNumber + "->" + (targetLineNumber >= 0 ? targetLineNumber : "N"))
                    : sourceLineNumber + "";
            String reportLine = FINAL_REPORT_LINE_TEMPLATE.replace(FINAL_REPORT_CODE_KEYWORD, lines.get(i))
                    .replace(FINAL_REPORT_LINE_NUM_KEYWORD,
                            FINAL_REPORT_LINE_NUM_PLACEHOLDER.substring(lineNumStr.length()) + lineNumStr)
                    .replace(FINAL_REPORT_LINE_HIT_KEYWORD, reportConfig.isShowHits() ?
                            FINAL_REPORT_LINE_HIT_PLACEHOLDER.substring(srcHitCntStr.length()) + srcHitCntStr
                            : FINAL_REPORT_LINE_HIT_PLACEHOLDER);



            String backgroundColor = "white";
            if (lineNumberMapping.containsKey(sourceLineNumber) && (reportConfig.isAllColors() || !isSrc)) {
                int targetHitCnt = targetCoverage.containsKey(targetLineNumber) ? targetCoverage.get(targetLineNumber) : -1;
                backgroundColor = srcHitCnt == targetHitCnt ? "white"
                        : srcHitCnt > targetHitCnt ? "cyan" : "red";
            } else if (newLines.contains(sourceLineNumber)) {
                backgroundColor = "yellow";
                if(!isSrc)
                    reportLine = reportLine.replaceFirst("N", "U"); // change the mapped line sign from N to U
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

        File sample1 = new File("/home/khaes/phd/projects/explanation/code/tmp/swagger-dubbo2");
        File sample2 = new File("/home/khaes/phd/projects/explanation/code/tmp/swagger-dubbo");
        new TraceAnalyzer(sample1, sample2)
                .generatedTraceDiff(sample2,
                        "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/annotations/EnableDubboSwagger.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/config/DubboPropertyConfig.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/config/DubboServiceScanner.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/config/SwaggerDocCache.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/http/HttpMatch.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/http/ReferenceManager.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/DubboReaderExtension.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/NameDiscover.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/DubboReaderExtension.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/Reader.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/ReaderContext.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/reader/ReaderExtension.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/web/DubboHttpController.java;" +
                                "swagger-dubbo/src/main/java/com/deepoove/swagger/dubbo/web/SwaggerDubboController.java;",
                        "showHits-allColors;showHits;allColors; ");

    }
}
