package se.kth.assertgroup.core.analysis.trace.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import se.kth.assertgroup.core.analysis.trace.models.GHReports;
import se.kth.assertgroup.core.analysis.trace.models.LineMapping;
import se.kth.assertgroup.core.analysis.trace.models.TraceInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GHHelper {
    private static final String GH_COMMIT_KEYWORD = "{commit}";
    private static final String GH_SLUG_KEYWORD = "{slug}";
    private static final String GH_COMMIT_URL_TEMPLATE = "https://github.com/" + GH_SLUG_KEYWORD
            + "/commit/" + GH_COMMIT_KEYWORD;
    private static final String GH_REPO_URL_TEMPLATE = "https://github.com/" + GH_SLUG_KEYWORD + ".git";
    private static final String GIT_PREVIOUS_COMMIT_REF = "HEAD~1";

    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String TEST_FILE_SUB_STR = "/test/";
    private static final long SELENIUM_LOAD_WAIT_SEC = 1000L;

    public static List<String> cloneCommitAndGetChangedSources
            (
                    String slug,
                    String commit,
                    File originalDir,
                    File patchedDir
            ) throws Exception {
        originalDir.mkdirs();
        patchedDir.mkdirs();

        String repoGitUrl = GH_REPO_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug);
        int cloneRes = PH.run(originalDir, "cloning " + slug, "git", "clone", repoGitUrl, ".");
        if (cloneRes != 0)
            throw new Exception("Cannot clone " + slug);

        int checkoutRes = PH.run(originalDir, "checkout to " + commit, "git", "checkout", commit);
        if (checkoutRes != 0)
            throw new Exception("Cannot checkout " + commit);

        FileUtils.copyDirectory(originalDir, patchedDir);

        checkoutRes = PH.run(originalDir, "checkout to " + commit, "git", "checkout",
                GIT_PREVIOUS_COMMIT_REF);
        if (checkoutRes != 0)
            throw new Exception("Cannot checkout to the previous commit of" + commit);


        List<String> modifiedSrcPaths = new ArrayList<>();
        String commitUrl = GH_COMMIT_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug).replace(GH_COMMIT_KEYWORD, commit);
        Document doc = Jsoup.connect(commitUrl).get();

        Elements changedFileElems = doc.select("a.Link--primary");
        for (Element changedFileElem : changedFileElems) {
            String filePath = changedFileElem.text();
            if (filePath != null && filePath.endsWith(JAVA_FILE_EXTENSION) && !filePath.contains(TEST_FILE_SUB_STR))
                modifiedSrcPaths.add(filePath);
        }

        return modifiedSrcPaths;
    }

    public static GHReports getGHReports
            (
                    String slug,
                    String commit,
                    List<String> modifiedFiles,
                    Map<String, Map<Integer, Integer>> originalCoverages,
                    Map<String, Map<Integer, Integer>> patchedCoverages,
                    String linkToExpanded
            ) {
        String unexpandedHTML = null, expandedHTML = null;
        GHReports.ReportSummary reportSummary = null;

        String commitUrl = GH_COMMIT_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug).replace(GH_COMMIT_KEYWORD, commit);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        WebDriver nonExpandingDriver = new ChromeDriver(options);
        WebDriver expandingDriver = new ChromeDriver(options);
        try {
            // adding trace data to both expanding and non-expanding versions
            nonExpandingDriver.get(commitUrl);
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
            addTraceData(nonExpandingDriver, modifiedFiles, originalCoverages, patchedCoverages, false);

            expandingDriver.get(commitUrl);
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
            reportSummary =
                    addTraceData(expandingDriver, modifiedFiles, originalCoverages, patchedCoverages, true);


            // adding link to expanded version in the unexpanded version
            unexpandedHTML = nonExpandingDriver.getPageSource();
            expandedHTML = expandingDriver.getPageSource();
            boolean showExpandWarning = expandedContainsExecDiff(nonExpandingDriver, expandingDriver);
            addLinkToExpandedVersion(nonExpandingDriver, expandingDriver, linkToExpanded, showExpandWarning);


            // adding exec diff summary
            addExecDiffSummary(nonExpandingDriver, expandingDriver, reportSummary);


            unexpandedHTML = nonExpandingDriver.getPageSource();
            expandedHTML = expandingDriver.getPageSource();
        } catch (Exception e) {
            throw new Exception("Could not get mappings from GH page.");
        } finally {
            nonExpandingDriver.quit();
            expandingDriver.quit();

            return new GHReports(expandedHTML, unexpandedHTML, reportSummary);
        }
    }

    private static void addExecDiffSummary
            (
                    WebDriver nonExpandingDriver,
                    WebDriver expandingDriver,
                    GHReports.ReportSummary summary
            ) {
        int redSlots = summary.getLinesWithFewerExec() > 0 ? 1 : 0,
                greenSlots = summary.getLinesWithMoreExec() > 0 ? 1 : 0,
                graySlots = summary.getLinesWithEqualExec() > 0 ? 1 : 0;

        if (summary.getLinesWithMoreExec() < summary.getLinesWithFewerExec())
            redSlots = 5 - greenSlots - graySlots;
        else if (summary.getLinesWithFewerExec() < summary.getLinesWithMoreExec())
            greenSlots = 5 - redSlots - graySlots;
        else {
            redSlots = greenSlots = 2;
            graySlots = 1;
        }

        List<String> summaryLabelClasses = new ArrayList<>();
        summaryLabelClasses.addAll(Collections.nCopies(redSlots, "<span class=\\\"diffstat-block-deleted\\\"></span>"));
        summaryLabelClasses.addAll(Collections.nCopies(greenSlots, "<span class=\\\"diffstat-block-added\\\"></span>"));
        summaryLabelClasses.addAll(Collections.nCopies(graySlots, "<span class=\\\"diffstat-block-neutral\\\"></span>"));

        String summaryHTML = ("<span class=\\\"diffstat tooltipped tooltipped-e\\\" aria-label=\\\"{more-exec} lines executed " +
                "more &amp; {fewer-exec} lines executed fewer times.\\\"><span style=\\\"margin-right: 5px\\\">EXEC-DIFF: {total-changed}</span>" +
                "{spans}</span>")
                .replace("{more-exec}", summary.getLinesWithMoreExec() + "")
                .replace("{fewer-exec}", summary.getLinesWithFewerExec() + "")
                .replace("{equal-exec}", summary.getLinesWithEqualExec() + "")
                .replace("{spans}", StringUtils.join(summaryLabelClasses, ""))
                .replace("{total-changed}",
                        (summary.getLinesWithFewerExec() + summary.getLinesWithMoreExec()) + "");

        ((JavascriptExecutor) nonExpandingDriver)
                .executeScript(("document.querySelector(\"details.js-file-header-dropdown\").parentNode.innerHTML = \"{new-html}\"")
                        .replace("{new-html}", summaryHTML));
        ((JavascriptExecutor) expandingDriver)
                .executeScript(("document.querySelector(\"details.js-file-header-dropdown\").parentNode.innerHTML = \"{new-html}\"")
                        .replace("{new-html}", summaryHTML));
    }

    // returns if there is some execution trace diff
    private static GHReports.ReportSummary addTraceData
    (
            WebDriver driver,
            List<String> modifiedFiles,
            Map<String, Map<Integer, Integer>> originalCoverages,
            Map<String, Map<Integer, Integer>> patchedCoverages,
            boolean expand
    ) throws InterruptedException {
        int linesWithMoreExec = 0, linesWithFewerExec = 0, linesWithEqualExec = 0;

        JavascriptExecutor jse = ((JavascriptExecutor) driver);


        // adding data per modified source file
        List<WebElement> diffElems = driver.findElements(By.cssSelector("div[data-path]"));
        for (WebElement diffElem : diffElems) {
            String path = diffElem.getAttribute("data-path");
            Map<Integer, Integer> originalCoverage = originalCoverages.get(path),
                    patchedCoverage = patchedCoverages.get(path);

            if (!modifiedFiles.contains(path)) { // we only modify the diff for the modified files
                continue;
            }


            if (expand) {
                // clicking on needed expandable items
                while (true) {
                    List<WebElement> expandableElems = driver.findElements(By.cssSelector("a.js-expand"));
                    if (expandableElems == null || expandableElems.isEmpty())
                        break;

                    expandableElems.get(0).click();
                    Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
                }


                // removing highlights for expanded lines
                jse.executeScript("Array.from(document.getElementsByClassName('blob-expanded'))" +
                        ".forEach(e => e.classList.remove('blob-expanded'))");
            }

            // Adding the exec info for the current file
            boolean execHeaderAdded = false;
            List<WebElement> lineElems = ((WebElement) jse.executeScript(
                    "return arguments[0].parentNode;", diffElem)).findElements(By.tagName("tr"));
            for (WebElement lineElem : lineElems) {
                List<WebElement> colElems = lineElem.findElements(By.tagName("td"));

                if (lineElem.getAttribute("class").contains("js-expandable-line")) { // its not a source line
                    jse.executeScript(("arguments[0].innerHTML += \"<td class=\\\"{classes}\\\" " +
                                    "style=\\\"text-align: center\\\">{exec-header}</td>\";" +
                                    "var lastChildInd = arguments[0].childNodes.length - 1;" +
                                    "arguments[0].childNodes[lastChildInd].after(arguments[0].childNodes[lastChildInd - 2]);")
                                    .replace("{classes}", colElems.get(0).getAttribute("class"))
                                    .replace("{exec-header}", !execHeaderAdded ? "EXEC-DIFF" : ""),
                            lineElem);
                    execHeaderAdded = true;
                    continue;
                }

                // extracting src and dst line numbers

                String srcLineNumAttr = colElems.get(0).getAttribute("data-line-number"),
                        dstLineNumAttr = colElems.get(1).getAttribute("data-line-number");

                if ((srcLineNumAttr != null && !srcLineNumAttr.matches("-?\\d+")) ||
                        (dstLineNumAttr != null && !dstLineNumAttr.matches("-?\\d+"))) {
                    jse.executeScript(("arguments[0].innerHTML += \"<td class=\\\"{classes}\\\" " +
                                    "style=\\\"text-align: center\\\">{exec-header}</td>\";" +
                                    "var lastChildInd = arguments[0].childNodes.length - 1;" +
                                    "arguments[0].childNodes[lastChildInd].after(arguments[0].childNodes[lastChildInd - 2]);")
                                    .replace("{classes}", colElems.get(0).getAttribute("class"))
                                    .replace("{exec-header}", !execHeaderAdded ? "EXEC-DIFF" : ""),
                            lineElem);
                    execHeaderAdded = true;
                    continue;
                }


                int srcLineNum = srcLineNumAttr == null ? -1 : Integer.parseInt(srcLineNumAttr),
                        dstLineNum = dstLineNumAttr == null ? -1 : Integer.parseInt(dstLineNumAttr);


                // computing exec-info
                int dstExecCnt = patchedCoverage.containsKey(dstLineNum) ? patchedCoverage.get(dstLineNum) : -1,
                        diffExecCnt = -1;
                if (srcLineNum >= 0 && dstLineNum >= 0) {
                    diffExecCnt = !patchedCoverage.containsKey(dstLineNum) || !originalCoverage.containsKey(srcLineNum)
                            ? 0 : patchedCoverage.get(dstLineNum) - originalCoverage.get(srcLineNum);
                    linesWithMoreExec += diffExecCnt > 0 ? 1 : 0;
                    linesWithEqualExec += diffExecCnt == 0 ? 1 : 0;
                    linesWithFewerExec += diffExecCnt < 0 ? 1 : 0;
                }

                String execInfo = getExecInfo(dstExecCnt, diffExecCnt, srcLineNum >= 0 && dstLineNum >= 0);


                // adding exec-info
                jse.executeScript(("arguments[0].innerHTML += \"<td no-empty-exec-info=\\\"{no-empty-exec-info}\\\" " +
                                "data-line-number=\\\"{exec-info}\\\" " +
                                "class=\\\"{classes}\\\"></td>\";" +
                                "var lastChildInd = arguments[0].childNodes.length - 1;" +
                                "arguments[0].childNodes[lastChildInd].after(arguments[0].childNodes[lastChildInd - 2]);")
                                .replace("{exec-info}", execInfo)
                                .replace("{classes}", colElems.get(1).getAttribute("class"))
                                .replace("{no-empty-exec-info}", !execInfo.isEmpty() + ""),
                        lineElem);
            }
        }

        return new GHReports.ReportSummary(linesWithMoreExec, linesWithFewerExec, linesWithEqualExec);
    }

    private static String getExecInfo(int dstExecCnt, int diffExecCnt, boolean includeDiffCnt) {
        if (dstExecCnt < 0)
            return "";

        String dstExecStr = toHumanReadableStr(dstExecCnt);
        if (!includeDiffCnt)
            return dstExecStr;

        String diffSign = diffExecCnt > 0 ? "+" : "", diffCntStr = toHumanReadableStr(diffExecCnt),
                diffFullStr = "(" + diffSign + diffCntStr + ")";
        return dstExecStr + "&nbsp;".repeat(11 - dstExecStr.length() - diffFullStr.length()) + diffFullStr;
    }

    private static String toHumanReadableStr(int num) {
        if (Math.abs(num) >= Math.pow(10, 6))
            return (int) (num / Math.pow(10, 6)) + "M";
        else if (Math.abs(num) >= Math.pow(10, 3))
            return (int)(num / Math.pow(10, 3)) + "K";
        return num + "";
    }

    private static void addLinkToExpandedVersion
            (
                    WebDriver nonExpandingDriver,
                    WebDriver expandingDriver,
                    String linkToExpanded,
                    boolean showExpandWarning
            ) {
        ((JavascriptExecutor) nonExpandingDriver).executeScript(("var a = document.createElement('a');\n" +
                "var linkText = document.createTextNode(\"See full diff\");\n" +
                "a.appendChild(linkText);\n" +
                "a.title = \"{title}\";\n" +
                "a.href = \"{link-to-full}\";\n" +
                "a.className = \"float-right ml-2\";\n" +
                "var item = document.querySelector(\"div.ml-2\"); item.parentNode.replaceChild(a, item);")
                .replace("{link-to-full}", linkToExpanded)
                .replace("{title}", "There are " + (showExpandWarning ? "" : "no")
                        + " execution trace diffs not visible on this page."));

        ((JavascriptExecutor) expandingDriver).executeScript("document.querySelector(\"div.ml-2\").remove();");
    }

    private static boolean expandedContainsExecDiff(WebDriver nonExpandingDriver, WebDriver expandingDriver) {
        return nonExpandingDriver.findElements(By.cssSelector("td[no-empty-exec-info='true']")).size() !=
                expandingDriver.findElements(By.cssSelector("td[no-empty-exec-info='true']")).size();
    }
}
