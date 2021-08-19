package se.kth.assertgroup.core.analysis.trace.utils;

import com.github.gumtreediff.matchers.Mapping;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import se.kth.assertgroup.core.analysis.trace.models.LineMapping;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

public class SpoonHelper {
    public static LineMapping getDiff(File source, File target) throws Exception {
        LineMapping res = new LineMapping();


        Diff diff = new AstComparator().compare(source, target);


        Iterator<Mapping> it = diff.getMappingsComp().iterator();
        while (it.hasNext()) {
            Mapping mapping = it.next();

            if (mapping.first != null && mapping.second != null
                    && (mapping.first.getChildren().isEmpty() || mapping.second.getChildren().isEmpty())) {

                CtElement srcElem = (CtElement) mapping.first.getMetadata("spoon_object"),
                        dstElem = (CtElement) mapping.second.getMetadata("spoon_object");

                if (!srcElem.getPosition().isValidPosition() || !dstElem.getPosition().isValidPosition())
                    continue;

                int srcLine = srcElem.getPosition().getLine(), dstLine = dstElem.getPosition().getLine();
                res.addMapping(srcLine, dstLine);
            }
        }


        diff.getRootOperations().forEach(op -> removeChangedLinesFromMapping(res, op));


        return res;
    }

    private static void removeChangedLinesFromMapping(LineMapping mapping, Operation op){
        if(op instanceof InsertOperation){
            removeNodeFromMapping(op.getDstNode(), mapping.getDstToSrc());
        } else if(op instanceof DeleteOperation){
            removeNodeFromMapping(op.getSrcNode(), mapping.getSrcToDst());
        } else if(op instanceof MoveOperation || op instanceof UpdateOperation){
            removeNodeFromMapping(op.getSrcNode(), mapping.getSrcToDst());
            removeNodeFromMapping(op.getDstNode(), mapping.getDstToSrc());
        }
    }

    private static void removeNodeFromMapping(CtElement dstNode, Map<Integer, Integer> mapping) {
        IntStream.rangeClosed(dstNode.getPosition().getLine(), dstNode.getPosition().getEndLine())
                .forEach(x -> mapping.remove(x));
    }
}
