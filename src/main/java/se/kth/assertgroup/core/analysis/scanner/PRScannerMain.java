package se.kth.assertgroup.core.analysis.scanner;

import org.apache.commons.io.FileUtils;
import se.kth.assertgroup.core.analysis.scanner.githubapi.code_changes.GithubAPIPullRequestAdapter;
import se.kth.assertgroup.core.analysis.scanner.githubapi.code_changes.models.SelectedPullRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PRScannerMain {
    public static void main(String[] args) throws IOException {
        long intervalStart = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30;
        List<SelectedPullRequest> selectedPRs =
                GithubAPIPullRequestAdapter.getInstance().getSingleLinePRs(intervalStart, 1000);

        FileUtils.writeLines(new File("files/SelectedPRs.txt"), selectedPRs);
    }
}