package se.kth.assertgroup.core.analysis.trace.models;

import java.util.HashMap;
import java.util.Map;

public class LineMapping {
    private Map<Integer, Integer> srcToDst, dstToSrc;

    public LineMapping(){
        srcToDst = new HashMap<>();
        dstToSrc = new HashMap<>();
    }

    public void addMapping(int src, int dst){
        srcToDst.put(src, dst);
        dstToSrc.put(dst, src);
    }

    public Map<Integer, Integer> getSrcToDst() {
        return srcToDst;
    }

    public void setSrcToDst(Map<Integer, Integer> srcToDst) {
        this.srcToDst = srcToDst;
    }

    public Map<Integer, Integer> getDstToSrc() {
        return dstToSrc;
    }

    public void setDstToSrc(Map<Integer, Integer> dstToSrc) {
        this.dstToSrc = dstToSrc;
    }
}
