package playground.johannes.osm.FacilityGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by NicoKuehnel on 26.09.2016.
 */
public class Mapping {

    static Map<String, String> tag2Type;

    public static void initTag2TypeFromCsv(String file) {

        tag2Type = new HashMap<String, String>();


        String line;
        String cvsSplitBy = ";";

        try (BufferedReader br = new BufferedReader(new FileReader(new File(file)))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] lineString = line.split(cvsSplitBy);

                String key = lineString[0];
                String tag = lineString[1];
                String type = lineString[2];

                tag2Type.put(key + "_" + tag, type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
