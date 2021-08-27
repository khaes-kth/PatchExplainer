import sys
import os
import multiprocessing as mp

rules = ["1217", "1860", "2095", "2111", "2116", "2142", "2184", "2225", "2272", "4973"]

def process_per_rule(repo, commit, rule):
    reponame = repo.split("/")[-1].split(".")[0]
    exec_id = reponame + "_" + rule
    print(f"EXEC: {exec_id}")
    os.system(f"git clone {repo} {exec_id}")
    os.chdir(f"{exec_id}")
    os.system("git checkout " + commit)
    os.chdir("..")
    os.system(f"cp -R {exec_id} {exec_id}2")
    os.system(f"java -jar sorald.jar repair --source {exec_id}2/ --rule-key {rule} 1>> output/{exec_id}_sorald.log 2>> output/{exec_id}_sorald.err")
    
    with open(f"output/{exec_id}_sorald.log", 'r') as file:
        sorald_result = file.read()
    if not "No rule violations found" in sorald_result:
        os.chdir(f"{exec_id}")
        os.system(f"timeout 1000 mvn clean eu.stamp-project:dspot-diff-test-selection:list -Dpath-dir-second-version=../{exec_id}2 -Doutput-path=../output/{exec_id}_selected_tests.csv 1>> ../output/{exec_id}.log 2>> ../output/{exec_id}.err")
        os.chdir("..")
        os.system(f"rm {exec_id}patch.diff")
    else:
        print(f"Skipping {exec_id} because of no change from sorald")
    os.system(f"rm -rf {exec_id}2")
    os.system(f"rm -rf {exec_id}")

def process(repo, commit):
    for rule in rules:
        process_per_rule(repo, commit, rule)


def main(argv):
    pool = mp.Pool()
    
    with open(argv[0]) as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]
        for i in range(1, len(lines)):
            repo = lines[i].split(",")[0]
            commit = lines[i].split(",")[1]
            pool.apply_async(process, args = (repo, commit, ))

    print(pool._processes)
    pool.close()
    pool.join()

if __name__ == "__main__":
    main(sys.argv[1:])
