package se.kth.assertgroup.core.analysis.statediff.models;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SrcLineVars {
    private Map<Integer, Set<String>> lineVars;

    private Launcher launcher = new Launcher();
    private CtModel model;

    public SrcLineVars(File src){
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.addInputResource(src.getAbsolutePath());
        launcher.buildModel();
        model = launcher.getModel();
        computeLineVars();
    }

    private void computeLineVars(){
        lineVars = new HashMap<>();

        model.filterChildren(new TypeFilter<>(CtVariableRead.class)).forEach(obj -> {
            CtVariableRead var = (CtVariableRead) obj;
            int line = var.getPosition().getLine();
            if(!lineVars.containsKey(line))
                lineVars.put(line, new HashSet<>());
            lineVars.get(line).add(var.getVariable().getSimpleName());
        });
    }
}
