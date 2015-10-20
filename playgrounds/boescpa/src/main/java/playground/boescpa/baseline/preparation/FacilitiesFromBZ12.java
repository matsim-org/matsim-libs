package playground.boescpa.baseline.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import playground.boescpa.lib.obj.CSVReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        final boolean publicFacilities = false;

        final ActivityFacilitiesFactoryImpl factory = new ActivityFacilitiesFactoryImpl();
        final ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
        int idNumber = 0;

        CSVReader reader = new CSVReader(pathToCSV);
        reader.skipLine(); // skip header
        String[] newLine = reader.readLine(); // first line
        while (newLine != null) {

            // EMPFTE;METER_X;METER_Y;NOGA_CD_2008_6;KATEGORIE;NOGA_TAG;CAPACITY;OPEN_FROM;OPEN_TO;METER_X_GERUNDET;METER_Y_GERUNDET
            String idString;
            String desc;
            if (!publicFacilities) {
                //idString = newLine[4];
                desc = newLine[4]; //5];
            } else {
                //idString = activityType;
                desc = null;
            }
            idString = String.format("%06d", ++idNumber); //idString + "_" + String.format("%06d", ++idNumber);
            Double capacity = Double.parseDouble(newLine[6]);
            Double openFrom = Double.parseDouble(newLine[7]);
            Double openTill = Double.parseDouble(newLine[8]);
            Double xCoord = Double.parseDouble(newLine[9]);
            Double yCoord = Double.parseDouble(newLine[10]);

            // new facility
            ActivityFacilityImpl newFacility = (ActivityFacilityImpl) factory.createActivityFacility(
                    Id.create(idString, ActivityFacility.class),
                    new Coord(xCoord,yCoord));
            newFacility.setDesc(desc);
            // new activity
            ActivityOption newActivity = factory.createActivityOption(activityType);
            newActivity.setCapacity(capacity);
            newActivity.addOpeningTime(new OpeningTimeImpl(openFrom, openTill));

            // add new facility and activity
            newFacility.addActivityOption(newActivity);
            facilities.addActivityFacility(newFacility);

            newLine = reader.readLine(); // next line
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

    /*private class TempFacility {
        Coord coords;
        List<ActivityOption> activities;
        Map<String, Double> descs;

        TempFacility(double xCoord, double yCoord) {
            this.coords = new Coord(xCoord, yCoord);
            activities = new ArrayList<>();
            descs = new HashMap<>();
        }
    }*/
}
