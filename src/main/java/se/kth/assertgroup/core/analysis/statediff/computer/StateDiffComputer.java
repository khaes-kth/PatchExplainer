package se.kth.assertgroup.core.analysis.statediff.computer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import se.kth.assertgroup.core.analysis.statediff.models.ProgramStateDiff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StateDiffComputer {

    /* Required formatting for the following files
    lineMapping:
    x,y
    ...

    lineToVars:
    x,var1;var2;...
    ...
     */
    private File leftBreakPoint, rightBreakPoint, leftReturn, rightReturn, lineMappingFile,
            leftLineToVarsFile, rightLineToVarsFile;

    private Map<Integer, Integer> leftRightLineMapping, rightLeftLineMapping;
    private Map<Integer, String> leftLineToVars, rightLineToVars;

    public StateDiffComputer
            (
                    File leftBreakPoint,
                    File rightBreakPoint,
                    File leftReturn,
                    File rightReturn,
                    File lineMappingFile,
                    File leftLineToVarsFile,
                    File rightLineToVarsFile
            ) throws IOException {
        this.leftBreakPoint = leftBreakPoint;
        this.rightBreakPoint = rightBreakPoint;
        this.leftReturn = leftReturn;
        this.rightReturn = rightReturn;
        this.lineMappingFile = lineMappingFile;
        this.leftLineToVarsFile = leftLineToVarsFile;
        this.rightLineToVarsFile = rightLineToVarsFile;

        extractLineInfo();
    }

    private void extractLineInfo() throws IOException {
        leftRightLineMapping = new HashMap<>();
        rightLeftLineMapping = new HashMap<>();
        List<String> lines = FileUtils.readLines(lineMappingFile, "UTF-8");
        lines.forEach(l -> leftRightLineMapping.put(Integer.parseInt(l.split(",")[0]),
                Integer.parseInt(l.split(",")[1])));
        lines.forEach(l -> rightLeftLineMapping.put(Integer.parseInt(l.split(",")[1]),
                Integer.parseInt(l.split(",")[0])));

        leftLineToVars = new HashMap<>();
        FileUtils.readLines(leftLineToVarsFile, "UTF-8").forEach(l -> {
            leftLineToVars.put(Integer.parseInt(l.split(",")[0]), l.split(",")[1]);
        });

        rightLineToVars = new HashMap<>();
        FileUtils.readLines(rightLineToVarsFile, "UTF-8").forEach(l -> {
            rightLineToVars.put(Integer.parseInt(l.split(",")[0]), l.split(",")[1]);
        });
    }

    public ProgramStateDiff computeProgramStateDiff() throws IOException, ParseException {
        ProgramStateDiff programStateDiff = new ProgramStateDiff();

        programStateDiff.setFirstOnlyOriginalState(getFirstDistinctStateOnRelevantLine(leftLineToVars,
                leftRightLineMapping, leftBreakPoint, rightBreakPoint));

        programStateDiff.setFirstOnlyPatchedState(getFirstDistinctStateOnRelevantLine(rightLineToVars,
                rightLeftLineMapping, rightBreakPoint, leftBreakPoint));

        return programStateDiff;
    }

    // return first state in @hashes that does not exist in states of the corresponding opposite line
    private Pair<Integer, String> getFirstDistinctStateOnRelevantLine
    (
            Map<Integer, String> lineToVars,
            Map<Integer, Integer> lineMapping,
            File breakpointFile,
            File oppositeBreakpointFile
    ) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONArray jsonStates = (JSONArray) parser.parse(new FileReader(breakpointFile)),
                oppositeJsonStates = (JSONArray) parser.parse(new FileReader(oppositeBreakpointFile));

        List<Pair<Integer, Integer>> hashes = getHashedBreakpointStates(jsonStates),
                oppositeHashes = getHashedBreakpointStates(oppositeJsonStates);

        Map<Integer, List<Integer>> oppositeLineToStateIndices = getLineToStateIndices(oppositeHashes);

        Map<Integer, Set<Integer>> oppositeLineToStates = getLineToStates(oppositeHashes);

        int firstLineWithDistinctState = -1;
        String firstDistinctState = null;
        for (int i = 0; i < hashes.size(); i++) {
            Pair<Integer, Integer> p = hashes.get(i);
            int lineNumber = p.getKey(), stateHash = p.getValue();

            if (!lineMapping.containsKey(lineNumber))
                continue;

            int oppositeLineNumber = lineMapping.get(lineNumber);

            if (!oppositeLineToStates.containsKey(oppositeLineNumber) ||
                    oppositeLineToStates.get(oppositeLineNumber).contains(stateHash)) {
                // this stateHash is not covered in the opposite version
                firstLineWithDistinctState = lineNumber;

                firstDistinctState = identifyBreakpointDistinctState((JSONObject) jsonStates.get(i),
                        lineToVars.get(lineNumber),
                        oppositeJsonStates, oppositeLineToStateIndices.get(oppositeLineNumber));
            }
        }

        return firstDistinctState == null ? null : Pair.of(firstLineWithDistinctState, firstDistinctState);
    }

    private String identifyBreakpointDistinctState
            (
                    JSONObject jsonState,
                    String lineVars,
                    JSONArray oppositeJsonStates,
                    List<Integer> oppositeTargetStateIndices
            ) {
        JSONArray valueCollection = breakpointStateToValueCollection(jsonState);

        Set<String> distinctVarVals = extractVarVals(valueCollection, lineVars, true);

        for(int ind : oppositeTargetStateIndices){
            JSONArray oppositeValueCollection = breakpointStateToValueCollection((JSONObject) oppositeJsonStates.get(ind));
            distinctVarVals.removeAll(extractVarVals(oppositeValueCollection, lineVars, true));
        }

        String shortestDistinctVarVal = null;

        for(String varVal : distinctVarVals){
            if(shortestDistinctVarVal == null || shortestDistinctVarVal.length()> varVal.length())
                shortestDistinctVarVal = varVal;
        }

        return shortestDistinctVarVal;
    }

    private Set<String> extractVarVals(JSONArray valueCollection, String lineVars, boolean checkLineVars) {
        Set<String> varVals = new HashSet<>();

        for(int i = 0; i < valueCollection.size(); i++){
            JSONObject valueJo = (JSONObject) valueCollection.get(i);

            // a variable that is not important in this line should be ignored
            if(checkLineVars && (lineVars == null || !lineVars.contains(valueJo.get("name").toString())))
                continue;

            varVals.addAll(extractVarVals("", valueJo));
        }
        return varVals;
    }

    private Set<String> extractVarVals(String prefix, JSONObject valueJo){
        Set<String> varVals = new HashSet<>();
        if(valueJo.get("nestedTypes") == null || ((JSONArray) valueJo.get("nestedTypes")).size() == 0){
            // it's a leaf node
            varVals.add(prefix + valueJo.get("name") + "=" + valueJo.get("value").toString());
            return varVals;
        }

        prefix += valueJo.get("name").toString();
        JSONArray nestedTypes = (JSONArray) valueJo.get("nestedTypes");

        for(int i = 0; i < nestedTypes.size(); i++){
            JSONObject nestedObj = (JSONObject) nestedTypes.get(i);
            varVals.addAll(extractVarVals(prefix, valueJo));
        }

        return varVals;
    }

    private JSONArray breakpointStateToValueCollection(JSONObject state){
        return (JSONArray) ((JSONObject) ((JSONArray)
                state.get("stackFrameContexts")).get(0)).get("runtimeValueCollection");
    }

    // Gives Json indices that correspond to states of a line
    private Map<Integer, List<Integer>> getLineToStateIndices(List<Pair<Integer, Integer>> lineToHashes) {
        Map<Integer, List<Integer>> ret = new HashMap<>();

        for (int i = 0; i < lineToHashes.size(); i++) {
            int lineNumber = lineToHashes.get(i).getKey();
            if (!ret.containsKey(lineNumber))
                ret.put(lineNumber, new ArrayList<>());
            ret.get(lineNumber).add(i);
        }

        return ret;
    }

    private Map<Integer, Set<Integer>> getLineToStates(List<Pair<Integer, Integer>> lineToHashes) {
        Map<Integer, Set<Integer>> ret = new HashMap<>();

        for (Pair<Integer, Integer> p : lineToHashes) {
            int lineNumber = p.getKey(), state = p.getValue();
            if (!ret.containsKey(lineNumber))
                ret.put(lineNumber, new HashSet<>());
            ret.get(lineNumber).add(state);
        }

        return ret;
    }

    // returns pair of (lineNumber, stateHash)
    private List<Pair<Integer, Integer>> getHashedBreakpointStates(JSONArray ja) throws IOException, ParseException {
        List<Pair<Integer, Integer>> hashedStates = new ArrayList<>();

        for (int i = 0; i < ja.size(); i++) {
            JSONObject jo = (JSONObject) ja.get(i);
            hashedStates.add(stateJsonToHashedStatePair(jo));
        }

        return hashedStates;
    }

    // Left of return is the lineNumber and right is the state hash
    private Pair<Integer, Integer> stateJsonToHashedStatePair(JSONObject stateJO) {
        int lineNumber = Integer.parseInt(stateJO.get("lineNumber").toString());
        stateJO = (JSONObject) ((JSONArray) stateJO.get("stackFrameContexts")).get(0);
        return Pair.of(lineNumber,
                stateJO.get("runtimeValueCollection").toString().hashCode());
    }

    public File getRightBreakPoint() {
        return rightBreakPoint;
    }

    public void setRightBreakPoint(File rightBreakPoint) {
        this.rightBreakPoint = rightBreakPoint;
    }

    public File getLeftBreakPoint() {
        return leftBreakPoint;
    }

    public void setLeftBreakPoint(File leftBreakPoint) {
        this.leftBreakPoint = leftBreakPoint;
    }

    public File getRightReturn() {
        return rightReturn;
    }

    public void setRightReturn(File rightReturn) {
        this.rightReturn = rightReturn;
    }

    public File getLeftReturn() {
        return leftReturn;
    }

    public void setLeftReturn(File leftReturn) {
        this.leftReturn = leftReturn;
    }
}
