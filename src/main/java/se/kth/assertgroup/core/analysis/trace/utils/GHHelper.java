package se.kth.assertgroup.core.analysis.trace.utils;

import org.apache.commons.io.FileUtils;
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
import org.openqa.selenium.firefox.FirefoxDriver;
import se.kth.assertgroup.core.analysis.trace.models.TraceInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GHHelper {
    private static final String GH_COMMIT_KEYWORD = "{commit}";
    private static final String GH_SLUG_KEYWORD = "{slug}";
    private static final String GH_COMMIT_URL_TEMPLATE = "https://github.com/" + GH_SLUG_KEYWORD
            + "/commit/" + GH_COMMIT_KEYWORD;
    private static final String GH_REPO_URL_TEMPLATE = "https://github.com/" + GH_SLUG_KEYWORD + ".git";
    private static final String GIT_PREVIOUS_COMMIT_REF = "HEAD~1";

    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String TEST_FILE_SUB_STR = "/test/";

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

    public static String GHReportHTMLWithTraceData
            (
                    String slug,
                    String commit,
                    Map<String, Map<Integer, Integer>> coverageDiffs
            ) throws IOException {

        String commitUrl = GH_COMMIT_URL_TEMPLATE.replace(GH_SLUG_KEYWORD, slug).replace(GH_COMMIT_KEYWORD, commit);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(commitUrl);

            // removing crossorigin attrs
            JavascriptExecutor jse = ((JavascriptExecutor) driver);
            jse.executeScript("document.querySelectorAll('link,script')" +
                    ".forEach(elem => {elem.removeAttribute('integrity'); elem.removeAttribute('crossorigin')})");


            // clicking on needed expandable
            List<WebElement> diffElems = driver.findElements(By.cssSelector("div[data-path]"));
            for (WebElement diffElem : diffElems) {
                String path = diffElem.getAttribute("data-path");
                if (!path.endsWith(".java") || path.contains("/test/")) {
                    jse.executeAsyncScript(
                            "var diffElem = document.querySelector(\"div[data-path='" + path + "']\"); " +
                                    "diffElem.parentNode.parentNode.removeChild(diffElem.parentNode);");
                    continue;
                }

                Map<Integer, Integer> coverageDiff = coverageDiffs.get(path);

                while (true) {
                    List<WebElement> expandableElems = driver.findElements(By.cssSelector("a.js-expand"));
                    boolean isClicked = false;

                    for (WebElement expandableElem : expandableElems) {
                        String dataUrl = expandableElem.getAttribute("data-url");
                        String[] urlParts = dataUrl.split("&");
                        int right = -1, lastRight = -1;
                        for (String urlPart : urlParts) {
                            if (urlPart.startsWith("right=")) {
                                String[] entryParts = urlPart.split("=");
                                right = entryParts.length == 2 ? Integer.parseInt(entryParts[1]) : 0;
                            }
                            if (urlPart.startsWith("last_right=")) {
                                String[] entryParts = urlPart.split("=");
                                lastRight = entryParts.length == 2 ? Integer.parseInt(entryParts[1]) : 0;
                            }
                        }

                        if (right < 0 || lastRight < 0) // invalid data
                            continue;

                        for (int lineNumber : coverageDiff.keySet()) {
                            if (lineNumber < right && lineNumber > lastRight) {
                                expandableElem.click();
                                isClicked = true;
                                break;
                            }
                        }

                        if (isClicked)
                            break;
                    }

                    if(!isClicked)
                        break;
                }


                jse.executeAsyncScript("Array.from(document.getElementsByClassName('blob-expanded'))" +
                        ".forEach(e => e.classList.remove('blob-expanded'))");
            }


            // Adding coverage diff info
            for (WebElement diffElem : diffElems) {
                String path = diffElem.getAttribute("data-path");
                Map<Integer, Integer> coverageDiff = coverageDiffs.get(path);
                final String covDiffJSMapStr = "";
                coverageDiff.entrySet().stream()
                        .forEach(e -> covDiffJSMapStr.concat("[" + e.getKey() + "," + e.getValue() + "]"));

                jse.executeAsyncScript("var map2 = new Map([{covDiffMap}]);\n" +
                        "document.querySelector(\"div[data-path='src/main/java/NumberAnalyzer.java']\").parentNode.querySelectorAll(\"tr\").forEach(e => {\n" +
                        "\tvar added = false;\n" +
                        "\tvar tdElem = e.getElementsByTagName(\"td\")[1];\n" +
                        "\tvar rightLineNumberStr = tdElem.getAttribute(\"data-line-number\");\n" +
                        "\tif(rightLineNumberStr != null){\n" +
                        "\t\tvar rightLineNumber = parseInt(rightLineNumberStr)\n" +
                        "\t\tvar covDiff = map2.get(parseInt(rightLineNumber));\n" +
                        "\t\tif(map2.has(rightLineNumber)){\n" +
                        "\t\t\tadded = true;\n" +
                        "\t\t\te.innerHTML += \"<td data-line-number=\\\"exec {exec-diff}\\\" style=\\\"background-color: {background-color};\\\" " +
                        "class=\\\"{classes}\\\"></td>\".replace(\"{exec-diff}\", covDiff > 0 ? \"+\" + covDiff : covDiff)" +
                        ".replace(\"{background-color}\", covDiff > 0 ? \"#90EE90\" : \"#FF7F7F\")" +
                        ".replace(\"{classes}\", tdElem.classList.toString());\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "\tif(!added){\n" +
                        "\t\tconsole.log(\"DIDDDD\");\n" +
                        "\t\te.innerHTML += \"<td class=\\\"{classes}\\\"></td>\".replace(\"{classes}\", tdElem.classList.toString());\n" +
                        "\t}\n" +
                        "});".replace("{covDiffMap}", covDiffJSMapStr));
            }

        } finally {
            driver.quit();
        }

        return driver.getPageSource();
    }
}
