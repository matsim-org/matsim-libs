package playground.johannes.synpop.data.io.flattable;

import playground.johannes.synpop.data.Attributable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by johannesillenberger on 05.04.17.
 */
public class TableWriter {

    public static final String NA_STRING = "";

    public void write(Set<? extends Attributable> element, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        /*
        Write header.
         */
        List<String> attributes = new ArrayList<>(collectAttributes(element));
        for(int i = 0; i < attributes.size() - 1; i++) {
            writer.write(attributes.get(i));
            writer.write("\t");
        }
        writer.write(attributes.get(attributes.size() - 1));
        writer.newLine();
        /*
        Write data.
         */
        for(Attributable p : element) {
            for(int i = 0; i < attributes.size() - 1; i++) {
                writer.write(getValue(p, attributes.get(i)));
                writer.write("\t");
            }
            writer.write(getValue(p, attributes.get(attributes.size() - 1)));
            writer.newLine();
        }

        writer.close();
    }

    private Set<String> collectAttributes(Set<? extends Attributable> persons) {
        Set<String> attributes = new HashSet<>();
        for(Attributable p : persons) {
            attributes.addAll(p.keys());
        }

        return attributes;
    }

    private String getValue(Attributable p, String key) {
        String value = p.getAttribute(key);
        if(value == null) value = NA_STRING;
        return value;
    }
}
