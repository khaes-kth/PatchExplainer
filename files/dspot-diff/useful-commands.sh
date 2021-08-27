grep -rnl 'output/' -e 'Clover has failed to instrument'
grep -rnl 'output/' -e 'Failed to execute goal'
grep -rnl 'output/' -e 'Compilation failure'
grep -rnl 'output/' -e 'There are test failures'
grep -rnl 'output/' -e 'Could not resolve dependencies'
ll output/*_selected_tests.csv
ll output/*sorald.log | wc -l
ll output/*[0-9].log
grep -rL 'Could not resolve dependencies' $(grep -rL 'There are test failures' $(grep -rL 'Compilation failure' $(grep -rL 'Clover has failed to instrument' $(grep -rnl 'output/' -e 'Failed to execute goal'))))
