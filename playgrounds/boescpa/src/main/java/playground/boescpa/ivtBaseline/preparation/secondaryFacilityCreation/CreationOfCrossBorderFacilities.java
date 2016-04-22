package playground.boescpa.ivtBaseline.preparation.secondaryFacilityCreation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.*;
import playground.boescpa.ivtBaseline.preparation.IVTConfigCreator;
import playground.boescpa.lib.obj.CSVReader;

/**
 * Creates facilities from BZ12.
 *
 * @author boescpa
 */
public class CreationOfCrossBorderFacilities {

	public static final String BC_TAG = "BC_";

    public static void main(final String[] args) {
        final String pathToCSV = args[0];
        final String pathToOutputFacilities = args[1];

        final boolean publicFacilities = true;

        final ActivityFacilitiesFactoryImpl factory = new ActivityFacilitiesFactoryImpl();
        final ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
        final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");

        CSVReader reader = new CSVReader(pathToCSV);
        reader.skipLine(); // skip header
        String[] newLine = reader.readLine(); // first line
        while (newLine != null) {

            // ID;Xcoord;Ycoord;Desc;Home Cap;Home OF;Home OT;Work Cap;Shop Cap;Shop OF;Shop OT;Leisure Cap;Leisure OF;Leisure OT
            int id = Integer.parseInt(newLine[0]);
            double xCoord = Double.parseDouble(newLine[2]);
            double yCoord = Double.parseDouble(newLine[1]);
            final Coord coord = coordinateTransformation.transform(new Coord(xCoord, yCoord));
            String desc = "Border Crossing - " + newLine[3];
            double shopCapacity = Double.parseDouble(newLine[8]);
            double shopOpenFrom = Double.parseDouble(newLine[9]);
            double shopOpenTill = Double.parseDouble(newLine[10]);
            double leisureCapacity = Double.parseDouble(newLine[11]);
            double leisureOpenFrom = Double.parseDouble(newLine[12]);
            double leisureOpenTill = Double.parseDouble(newLine[13]);

            // new facility
            ActivityFacilityImpl newFacility = (ActivityFacilityImpl) factory.createActivityFacility(
                    Id.create(BC_TAG + id, ActivityFacility.class),
                    coord);
            if (!publicFacilities) {
                newFacility.setDesc(desc);
            }
            // new activities
            ActivityOption newShopActivity = factory.createActivityOption(IVTConfigCreator.SHOP);
            newShopActivity.setCapacity(shopCapacity);
            newShopActivity.addOpeningTime(new OpeningTimeImpl(shopOpenFrom, shopOpenTill));
            newFacility.addActivityOption(newShopActivity);
            ActivityOption newLeisureActivity = factory.createActivityOption(IVTConfigCreator.LEISURE);
            newLeisureActivity.setCapacity(leisureCapacity);
            newLeisureActivity.addOpeningTime(new OpeningTimeImpl(leisureOpenFrom, leisureOpenTill));
            newFacility.addActivityOption(newLeisureActivity);
            // add new facility and activity
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
}