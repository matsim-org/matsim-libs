package playground.boescpa.baseline.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

import static playground.boescpa.baseline.preparation.FacilitiesFromBZ12.testFacilities;

/**
 * Adds activities to existing facilities.
 *
 * @author boescpa
 */
public class ActivityAdder {

    private final ActivityFacilitiesFactoryImpl factory = new ActivityFacilitiesFactoryImpl();
    private final ActivityFacilities facilities;

    protected ActivityAdder(ActivityFacilities facilities) {
        this.facilities = facilities;
    }

    public static void main(final String[] args) {
        final String pathToFacilities = args[0];
        final String pathToOutputFacilities = args[1];

        // Read facilities and create Adder
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
        facilitiesReader.readFile(pathToFacilities);
        ActivityAdder adder = new ActivityAdder(scenario.getActivityFacilities());

        // Add activities
        adder.addActivityToFacilities("remote_work", "work", 5, 0, 24*60*60);
        adder.addActivityToFacilities("remote_work", "primary_work", 5, 0, 24*60*60);
        adder.addActivityToFacilities("escort_kids", "education", 10, 7*60*60, 18*60*60);
        adder.addActivityToFacilities("escort_kids", "leisure", 5, 7*60*60, 22*60*60);
        adder.addActivityToFacilities("escort_other", "leisure", 5, 5*60*60, 24*60*60);
        adder.addActivityToFacilities("escort_other", "shop", 5, 5*60*60, 24*60*60);
        adder.addActivityToFacilities("remote_home", "home", 1, 0, 24*60*60);

        // Write facilities
        FacilitiesWriter facilitiesWriter = new FacilitiesWriter(scenario.getActivityFacilities());
        facilitiesWriter.write(pathToOutputFacilities);
        testFacilities(pathToOutputFacilities);
    }



    protected void addActivityToFacilities(String actTypeToAdd, String existingActToAddTo, double defaultCapacity,
                                           double defaultOpenFrom, double defaultOpenTill) {
        for (ActivityFacility facility : facilities.getFacilities().values()) {
            if (!facility.getActivityOptions().containsKey(actTypeToAdd) &&
                    facility.getActivityOptions().containsKey(existingActToAddTo)) {
                ActivityOption newActivity = factory.createActivityOption(actTypeToAdd);
                newActivity.setCapacity(defaultCapacity);
                newActivity.addOpeningTime(new OpeningTimeImpl(defaultOpenFrom, defaultOpenTill));
                facility.addActivityOption(newActivity);
            }
        }
    }


}
