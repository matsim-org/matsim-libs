package playground.joel.helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.NavigableMap;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** Created by Joel on 18.03.2017.
 * udpated, refactored clruch */
public class CSVcreator {

    private final String DEFAULT_SEPARATOR = "; ";

    public void createCSV(NavigableMap<String, Double> map, File path, String fileName) throws Exception {

        File csv = new File(path + "/" + fileName + ".csv");
        FileWriter writer = new FileWriter(csv);

        writeLines(writer, map);

        writer.flush();
        writer.close();

        GlobalAssert.that(csv.exists() && !csv.isDirectory());
        System.out.println("exported " + fileName + ".csv");

    }

    public void createCSV(NavigableMap<String, Double> map1, NavigableMap<String, Double> map2, NavigableMap<String, Double> map3, File path, String fileName)
            throws Exception {

        File csv = new File(path + "/" + fileName + ".csv");
        FileWriter writer = new FileWriter(csv);

        writeLines(writer, map1, map2, map3);

        writer.flush();
        writer.close();

        GlobalAssert.that(csv.exists() && !csv.isDirectory());
        System.out.println("exported " + fileName + ".csv");

    }

    private void writeLines(Writer w, NavigableMap<String, Double> map) throws IOException {
        writeLines(w, map, DEFAULT_SEPARATOR, ", ");
    }

    private void writeLines(Writer w, NavigableMap<String, Double> map1, NavigableMap<String, Double> map2, NavigableMap<String, Double> map3)
            throws IOException {
        writeLines(w, map1, map2, map3, DEFAULT_SEPARATOR, ", ");
    }

    private void writeLines(Writer w, NavigableMap<String, Double> map, String separators, String customQuote) throws IOException {

        // default customQuote is empty

        if (separators == " ") {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
            sb.append(KeyMap.keyToTime(key)).append(customQuote).append(map.get(key));

            sb.append("\n");
        }

        w.append(sb.toString());

    }

    private void writeLines(Writer w, NavigableMap<String, Double> map1, NavigableMap<String, Double> map2, NavigableMap<String, Double> map3,
            String separators, String customQuote) throws IOException {

        // default customQuote is empty

        if (separators == " ") {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String key : map1.keySet()) {
            sb.append(KeyMap.keyToTime(key)).append(customQuote).append(map1.get(key)).append(customQuote).append(map2.get(key)).append(customQuote)
                    .append(map3.get(key));

            sb.append("\n");
        }

        w.append(sb.toString());

    }

}
