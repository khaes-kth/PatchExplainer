package se.kth.assertgroup.core.analysis.statediff;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StateDiffUIHandler {
    public static void main(String[] args) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONArray ja = (JSONArray) parser.parse(new FileReader("files/sahab/tmp/breakpoint-left.json"));

        Set<Integer> left = new HashSet<>(), right = new HashSet<>();

        for(int i = 0; i < ja.size(); i++){
            JSONObject jo = (JSONObject) ja.get(i);
            jo = (JSONObject) ((JSONArray) jo.get("stackFrameContexts")).get(0);
            left.add((jo.get("location").toString() + jo.get("runtimeValueCollection").toString()).hashCode());
        }

        ja = (JSONArray) parser.parse(new FileReader("files/sahab/tmp/breakpoint-right.json"));

        for(int i = 0; i < ja.size(); i++){
            JSONObject jo = (JSONObject) ja.get(i);
            jo = (JSONObject) ((JSONArray) jo.get("stackFrameContexts")).get(0);
            right.add((jo.get("location").toString() + jo.get("runtimeValueCollection").toString()).hashCode());
        }

        left.removeAll(right);

        int unseenHash = left.iterator().next();

        ja = (JSONArray) parser.parse(new FileReader("files/sahab/tmp/breakpoint-left.json"));

        for(int i = 0; i < ja.size(); i++){
            JSONObject jo = (JSONObject) ja.get(i);
            jo = (JSONObject) ((JSONArray) jo.get("stackFrameContexts")).get(0);
            if(((jo.get("location").toString() + jo.get("runtimeValueCollection").toString()).hashCode()) == unseenHash){
                System.out.println(i + " " + unseenHash);
                System.out.println(jo);
            }
        }
    }
}
