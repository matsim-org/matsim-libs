package playground.boescpa.ivtBaseline.preparation.secondaryFacilityCreation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import playground.boescpa.ivtBaseline.preparation.IVTConfigCreator;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class EducationFacilityCreation {

    private static final ActivityFacilitiesFactoryImpl factory = new ActivityFacilitiesFactoryImpl();

    public static void main(String[] args) {
        final String inputConfig = args[0];
        final String outputFacilities = args[1];

        // Load input facilities
        final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(inputConfig));
        new FacilitiesReaderMatsimV1(scenario).parse(scenario.getConfig().facilities().getInputFile());
        final ActivityFacilities originalFacilities = scenario.getActivityFacilities();

        // Create new facilities
        final ActivityFacilities secondaryFacilities = createSecondaryFacilities(originalFacilities);

        // Write secondary facilities
        FacilitiesWriter facilitiesWriter = new FacilitiesWriter(secondaryFacilities);
        facilitiesWriter.write(outputFacilities);
    }

    private static ActivityFacilities createSecondaryFacilities(ActivityFacilities originalFacilities) {
        final ActivityFacilities secondaryFacilities = FacilitiesUtils.createActivityFacilities();
        for (ActivityFacility activityFacility : originalFacilities.getFacilities().values()) {
            if (activityFacility.getActivityOptions().keySet().contains(IVTConfigCreator.EDUCATION)) {
                // create new coords
                Coord coord = new Coord(
                        2000000 + activityFacility.getCoord().getX(),
                        1000000 + activityFacility.getCoord().getY());
                // create new facility
                ActivityFacility newFacility = factory.createActivityFacility(
                        Id.create(activityFacility.getId(), ActivityFacility.class), coord);
                secondaryFacilities.addActivityFacility(newFacility);
                // create and add education activity
                ActivityOption originalEducationActivity = activityFacility.getActivityOptions().get(IVTConfigCreator.EDUCATION);
                ActivityOption newEducationActivity = factory.createActivityOption(IVTConfigCreator.EDUCATION);
                copyActivityVals(originalEducationActivity, newEducationActivity);
                newFacility.addActivityOption(newEducationActivity);
            }
        }
        return secondaryFacilities;
    }

    private static void copyActivityVals(ActivityOption originalActivity, ActivityOption newActivity) {
        // Capacity
        newActivity.setCapacity(originalActivity.getCapacity());

        // Opening Times
        int i = 0;
        for (OpeningTime openingTime : originalActivity.getOpeningTimes()) {
            if (i == 0 || (
                    i == 1 && originalActivity.getOpeningTimes().first().getStartTime() > openingTime.getEndTime())) {
                newActivity.addOpeningTime(new OpeningTimeImpl(
                        openingTime.getStartTime(), openingTime.getEndTime()));
                i++;
            } else {
                break;
            }
        }
    }

}
