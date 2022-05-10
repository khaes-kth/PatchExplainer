import sys
import os
import multiprocessing as mp

OUTPUT_DIR='/home/khaes/phd/projects/explanation/code/tmp/output'
SAHAB_DIR='/home/khaes/phd/projects/explanation/code/tmp/collector-sahab'
DRR_DIR='/home/khaes/phd/projects/explanation/code/tmp/drr-execdiff'
MAIN_DIR='/home/khaes/phd/projects/explanation/code/tmp'

def runFreqReportGenerator(changedFile, rightCommit, slug, testName):
    os.chdir(f'{MAIN_DIR}')
    originalPath = f'{MAIN_DIR}/original/'
    patchedPath = f'{MAIN_DIR}/patched/'
    outputPath = f'{MAIN_DIR}/patched/target/trace/'
    javaCommand = f'sudo java -jar explainer-cli.jar freq --changed-file-path {changedFile} --commit {rightCommit} --full-report-link http://example.com --original-path {originalPath} --output-path {outputPath} --patched-path {patchedPath} --slug {slug} --selected-tests {testName}'

def runSahab(bugId, rightCommit, testName, changedFileName):
    os.chdir(f'{SAHAB_DIR}')
    sahabCommand = f'python3 scripts/bribe-sahab.py -p {DRR_DIR}/{bugId} --left e5d67a8162aebb7dbd5df8cdc21442ef111d2ba1 --right {rightCommit} -t {testName} -c {changedFileName} 1>> {OUTPUT_DIR}/logs/sahab_{rightCommit}.log 2>> {OUTPUT_DIR}/logs/sahab_{rightCommit}.err'
    os.system(sahabCommand)

def runStateDiffUIManipulator(rightCommit, testName, copiedGHDiffPath, oldSrcPath):
    sahabReportDir = f'{OUTPUT_DIR}/sahab-reports/{rightCommit}'
    os.chdir(f'{MAIN_DIR}')
    javaCommand = f'java -jar explainer-cli.jar sdiff --left-report-path {sahabReportDir}/left.json --right-report-path {sahabReportDir}/right.json --left-src-path {oldSrcPath} --right-src-path {newSrcPath} --trace-diff-report-path {copiedGHDiffPath} --selected-tests {testName} --test-link "http://example.com" 1>> {OUTPUT_DIR}/logs/diff_computer_{rightCommit}.log 2>> {OUTPUT_DIR}/logs/diff_computer_{rightCommit}.err'
    os.system(javaCommand)

def process(patchName, bugId, proj, bugNum):
    os.chdir(DRR_DIR)
    os.system(f'git checkout -f master')

    bugId = patchName.split('-')[1] + '-' + patchName.split('-')[2]
    proj = bugId.split('-')[0]
    bugNum = int(bugId.split('-')[1])
    
    with open(f'{DRR_DIR}/projects-metadata/{proj}-metadata.csv') as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]
        testName = lines[bugNum].split(',')[1].replace('"', "").split(';')[0]

    copiedGHDiffPath = f'{OUTPUT_DIR}/gh_full_{patchName}.html'
    os.system(f'cp ./exec-diff-reports/{patchName}/gh_full.html {copiedGHDiffPath}')
    os.system(f'git checkout -f {patchName}')
    rightCommit = os.popen(f'git log --grep="applied" --format="%H"').read().split('\n')[0]
    changedFile = os.popen(f'git diff-tree --no-commit-id --name-only -r {rightCommit}').read().split('\n')[0]
    os.system(f'cp -R ./* {MAIN_DIR}/original/')
    changedFileName = changedFile.split('/')[-1]
    os.system(f'rm {MAIN_DIR}/old-src/*')
    os.system(f'rm {MAIN_DIR}/new-src/*')
    oldSrcPath = f'{MAIN_DIR}/old-src/{changedFileName}'
    newSrcPath = f'{MAIN_DIR}/new-src/{changedFileName}'
    os.system(f'cp {changedFile} {MAIN_DIR}/new-src/{changedFileName}')
    os.system(f'git checkout -f HEAD~1')
    os.system(f'cp {changedFile} {MAIN_DIR}/old-src/{changedFileName}')
    os.system(f'cp -R ./* {MAIN_DIR}/original/')

    runFreqReportGenerator(changedFile, rightCommit, slug, testName)

    runSahab(bugId, rightCommit, testName, changedFileName)

    runStateDiffUIManipulator(rightCommit, testName, copiedGHDiffPath, oldSrcPath)

def main(argv):
    with open(argv[0]) as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]
        for line in lines:
            patchName = line.split(' ')[-1].split('/')[0]
            if not '-Time-' in patchName:
                continue
            process(patchName, bugId, proj, bugNum)

if __name__ == "__main__":
    main(sys.argv[1:])
