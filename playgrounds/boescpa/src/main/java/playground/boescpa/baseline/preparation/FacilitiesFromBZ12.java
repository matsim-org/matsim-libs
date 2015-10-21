package playground.boescpa.baseline.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import playground.boescpa.lib.obj.CSVReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates facilities from BZ12.
 *
 * @author boescpa
 */
public class FacilitiesFromBZ12 {

    public static void main(final String[] args) {
        final String pathToCSV = args[0];
        final String activityType = args[1];
        final String pathToOutputFacilities = args[2];

        final boolean publicFacilities = true;

        final ActivityFacilitiesFactoryImpl factory = new ActivityFacilitiesFactoryImpl();
        final Map<String, TempFacility> tempFacilities = new HashMap<>();
        int idNumber = 0;

        CSVReader reader = new CSVReader(pathToCSV);
        reader.skipLine(); // skip header
        String[] newLine = reader.readLine(); // first line
        while (newLine != null) {

            // EMPFTE;METER_X;METER_Y;NOGA_CD_2008_6;KATEGORIE;NOGA_TAG;CAPACITY;OPEN_FROM;OPEN_TO;METER_X_GERUNDET;METER_Y_GERUNDET
            String desc;
            if (!publicFacilities) {
                desc = newLine[4];
            } else {
                desc = null;
            }
            double capacity = Double.parseDouble(newLine[6]);
            double openFrom = Double.parseDouble(newLine[7]);
            double openTill = Double.parseDouble(newLine[8]);
            double xCoord = Double.parseDouble(newLine[9]);
            double yCoord = Double.parseDouble(newLine[10]);

            // new facility
            TempFacility tempFacility;
            String coordKey = xCoord + "_" + yCoord;
            if (tempFacilities.containsKey(coordKey)) {
                tempFacility = tempFacilities.get(coordKey);
            } else {
                tempFacility = new TempFacility(xCoord, yCoord);
                tempFacilities.put(coordKey, tempFacility);
            }
            tempFacility.capacity += capacity;
            tempFacility.openFrom = tempFacility.openFrom < openFrom ? tempFacility.openFrom : openFrom;
            tempFacility.openTill = tempFacility.openTill > openTill ? tempFacility.openTill : openTill;
            if (desc != null) {
                if (!tempFacility.descs.keySet().contains(desc)) {
                    tempFacility.descs.put(desc, 0l);
                }
                tempFacility.descs.put(desc, tempFacility.descs.get(desc) + (long) capacity);
            }

            newLine = reader.readLine(); // next line
        }

        // Create actual facilities:
        final ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
        for (TempFacility tempFacility : tempFacilities.values()) {
            ActivityFacilityImpl newFacility = (ActivityFacilityImpl) factory.createActivityFacility(
                    Id.create(String.format("%06d", ++idNumber), ActivityFacility.class),
                    tempFacility.coords);
            // Create description
            String finalDesc = "";
            for (String desc : tempFacility.descs.keySet()) {
                finalDesc = finalDesc + desc + " " + tempFacility.descs.get(desc) + "; ";
            }
            if (finalDesc.length() > 1) {
                finalDesc = finalDesc.substring(0, finalDesc.lastIndexOf(";"));
                newFacility.setDesc(finalDesc);
            }
            // Create activities
            ActivityOption newActivity = factory.createActivityOption(activityType);
            newActivity.setCapacity(tempFacility.capacity);
            newActivity.addOpeningTime(new OpeningTimeImpl(tempFacility.openFrom, tempFacility.openTill));
            newFacility.addActivityOption(newActivity);
            // add new facility and activity
            facilities.addActivityFacility(newFacility);
        }

        // Write facilities
        FacilitiesWriter facilitiesWriter = new FacilitiesWriter(facilities);
        facilitiesWriter.write(pathToOutputFacilities);
        testFacilities(pathToOutputFacilities);
    }

    public static void testFacilities(String pathToFile) {
        MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(
                ScenarioUtils.createScenario(
                        ConfigUtils.createConfig()));
        facilitiesReader.readFile(pathToFile);
    }

    private static class TempFacility {
        Coord coords;
        double capacity = 0;
        double openFrom = Double.MAX_VALUE;
        double openTill = Double.MIN_VALUE;
        Map<String, Long> descs;

        TempFacility(double xCoord, double yCoord) {
            this.coords = new Coord(xCoord, yCoord);
            descs = new HashMap<>();
        }
    }
}
