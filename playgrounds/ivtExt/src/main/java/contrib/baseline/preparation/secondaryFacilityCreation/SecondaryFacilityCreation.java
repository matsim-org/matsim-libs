package contrib.baseline.preparation.secondaryFacilityCreation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;

import contrib.baseline.lib.CSVReader;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class SecondaryFacilityCreation {

    private static final ActivityFacilitiesFactoryImpl factory = new ActivityFacilitiesFactoryImpl();

    public static void main(String[] args) {
        final String inputCSV = args[0];
        final String outputCSV = args[1];

        Map<String, String[]> facilities = new HashMap<>();

        CSVReader reader = new CSVReader(inputCSV);
        String[] header = reader.readLine(); // read header
        String[] newLine = reader.readLine(); // first line
        int i = 1;
        while (newLine != null) {
            // EMPFTE;METER_X;METER_Y;NOGA_CD_2008_6;KATEGORIE;NOGA_TAG;CAPACITY;OPEN_FROM;OPEN_TO;METER_X_GERUNDET;METER_Y_GERUNDET
            if (newLine[4].matches("Malls")) {
                facilities.put(""+i,newLine);
                i++;
            }
            /*String key = newLine[9] + "_" + newLine[10] + "_" + newLine[3];
            if (!facilities.keySet().contains(key)) {
                facilities.put(key, newLine);
            } else {
                String[] existingFacility = facilities.get(key);
                double capacity = Double.parseDouble(existingFacility[6]);
                capacity += Double.parseDouble(newLine[6]);
                existingFacility[6] = String.valueOf(capacity);
            }*/
            newLine = reader.readLine();
        }

        try {
            BufferedWriter writer = IOUtils.getBufferedWriter(outputCSV);
            writer.write(getString(header));
            writer.newLine();
            for (String[] facilityLine : facilities.values()) {
                String writeFacility = getString(facilityLine);
                writer.write(writeFacility);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getString(String[] csvLine) {
        String csvString = "";
        for (String facilitySpec : csvLine) {
            csvString = csvString + facilitySpec + ";";
        }
        csvString = csvString.substring(0, csvString.lastIndexOf(';'));
        return csvString;
    }

}
