package se.kth.assertgroup.core.analysis.trace.utils;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.kth.assertgroup.core.analysis.trace.models.LineCoverage;
import spoon.Launcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class JacocoHelper {
    private static final Logger logger = LoggerFactory.getLogger(JacocoHelper.class);

    private static final String JACOCO_MVN_XML_PATH = "trace/jacoco_plugin_mvn.xml";
    private static final String JACOCO_REPORT_DIR_PATH = "target/site/jacoco";
    private static final String JACOCO_REPORT_FILENAME_SUFFIX = ".html";
    private static final String JACOCO_REPORT_DEFAULT_DIR = "default";

    // MVN related keywords
    private static final String POM_FILENAME = "pom.xml";
    private static final String BUILD_KEYWORDS = "build";
    private static final String PLUGINS_KEYWORDS = "plugins";
    private static final String JACOCO_ID = "jacoco-maven-plugin";
    // End of MVN related keywords

    public static LineCoverage getPerLineCoverage(File projectDir, Path modifiedFilePath) throws Exception {
        addJacocoPluginToPom(projectDir);

        runMvnTest(projectDir);

        return getLineCoverage(projectDir, modifiedFilePath);
    }

    private static LineCoverage getLineCoverage(File projectDir, Path modifiedFilePath) throws IOException {
        String sourceCode =
                FileUtils.readFileToString(projectDir.toPath().resolve(modifiedFilePath).toFile(), "UTF-8");
        String className = Launcher.parseClass(sourceCode).getQualifiedName();
        Path jacocoReportHtmlPath = Path.of(projectDir.getPath(), JACOCO_REPORT_DIR_PATH,
                className.contains(".") ? className.substring(0, className.lastIndexOf(".")) : JACOCO_REPORT_DEFAULT_DIR,
                modifiedFilePath.getFileName().toString() + JACOCO_REPORT_FILENAME_SUFFIX);

        LineCoverage res = new LineCoverage();
        org.jsoup.nodes.Document doc = Jsoup.parse(jacocoReportHtmlPath.toFile(), "UTF-8");
        Elements lineSpans = doc.select("span[id~=L\\d+]");
        lineSpans.forEach(span -> {
            int lineNumber = Integer.parseInt(span.id().substring("L".length()));
            LineCoverage.CoverageStatus coverageStatus =
                    span.classNames().contains("pc") || span.classNames().contains("fc") ? LineCoverage.CoverageStatus.COVERED
                            : LineCoverage.CoverageStatus.UNCOVERED;
            res.put(lineNumber, coverageStatus);
        });

        return res;
    }

    private static void runMvnTest(File projectDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("mvn", "test");
        pb.inheritIO();
        pb.directory(projectDir);
        logger.info("Running mvn test....");
        Process p = pb.start();
        int exitVal = p.waitFor();
        if (exitVal != 0)
            throw new Exception("Could not run mvn test.");
    }

    private static void addJacocoPluginToPom(File projectDir)
            throws IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException {
        File pomFile = projectDir.toPath().resolve(POM_FILENAME).toFile();
        InputStream is = new FileInputStream(pomFile);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        is.close();


        Element buildElem = getOrCreateElemByTagName(BUILD_KEYWORDS, doc.getDocumentElement(), doc);
        Element pluginsElem = getOrCreateElemByTagName(PLUGINS_KEYWORDS, buildElem, doc);
        StreamResult pluginsStreamResult = new StreamResult(new StringWriter());
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(pluginsElem), pluginsStreamResult);
        String pluginsStr = pluginsStreamResult.getWriter().toString();
        if (pluginsStr.indexOf(JACOCO_ID) >= 0) {
            return;
        }


        URL resource = JacocoHelper.class.getClassLoader().getResource(JACOCO_MVN_XML_PATH);

        if (resource == null) {
            throw new IllegalArgumentException("Jacoco xml not found!");
        }

        String jacocoXmlStr = FileUtils.readFileToString(new File(resource.toURI()), "UTF-8");
        Document jacocoDoc = db.parse(new ByteArrayInputStream(jacocoXmlStr.getBytes("UTF-8")));
        Node jacocoNode = doc.importNode(jacocoDoc.getDocumentElement(), true);
        pluginsElem.appendChild(jacocoNode);


        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result output = new StreamResult(pomFile);
        Source input = new DOMSource(doc);

        transformer.transform(input, output);
    }

    private static Element getOrCreateElemByTagName(String tagName, Element elem, Document doc) {
        NodeList nodeLst = elem.getElementsByTagName(tagName);
        if (nodeLst.getLength() < 1) {
            elem.appendChild(doc.createElement(tagName));
            nodeLst = elem.getElementsByTagName(tagName);
        }
        return (Element) nodeLst.item(0);
    }

}
