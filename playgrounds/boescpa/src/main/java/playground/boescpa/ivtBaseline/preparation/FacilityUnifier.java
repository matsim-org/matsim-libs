package playground.boescpa.ivtBaseline.preparation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

import java.util.HashMap;
import java.util.Map;

import static playground.boescpa.ivtBaseline.preparation.secondaryFacilityCreation.FacilitiesFromBZ12.testFacilities;

/**
 * Takes two facility files and unites them to one.
 *
 * @author boescpa
 */
public class FacilityUnifier {

    public void setIfPublicFacilities(boolean publicFacilities) {
        this.publicFacilities = publicFacilities;
    }
    private void resetIDNumber() {
        idNumber = 0;
    }

    private boolean publicFacilities;
    private int idNumber = 0;
    private final ActivityFacilitiesFactoryImpl factory = new ActivityFacilitiesFactoryImpl();

    public static void main(final String[] args) {
        final String pathToFile_A = args[0];
        final String pathToFile_B = args[1];
        final String pathToFile_Out = args[2];
        final boolean publicFacilities = Boolean.parseBoolean(args[3]);
        final boolean allNewIds = Boolean.parseBoolean(args[4]);
        final String prefixIDs = args.length >= 6 ? args[5] : null;

        // Read facilities
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
        facilitiesReader.readFile(pathToFile_A);
        final ActivityFacilities facilities_A = scenario.getActivityFacilities();
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        facilitiesReader = new MatsimFacilitiesReader(scenario);
        facilitiesReader.readFile(pathToFile_B);
        final ActivityFacilities facilities_B = scenario.getActivityFacilities();
        ActivityFacilities facilities_Out;

        // Unite facilities
        FacilityUnifier unifier = new FacilityUnifier();
        unifier.setIfPublicFacilities(publicFacilities);
        if (allNewIds) {
            facilities_Out = unifier.uniteFacilitiesWithAllNewIDs(facilities_A, facilities_B);
        } else {
            facilities_Out = unifier.uniteFacilitiesByMergingBintoA(facilities_A, facilities_B, prefixIDs);
        }

        // Write facilities
        FacilitiesWriter facilitiesWriter = new FacilitiesWriter(facilities_Out);
        facilitiesWriter.write(pathToFile_Out);
        testFacilities(pathToFile_Out);
    }

    protected ActivityFacilities uniteFacilitiesByMergingBintoA(ActivityFacilities facilities_A, ActivityFacilities facilities_B, String prefixFacilitiesB) {
        final ActivityFacilities facilities_Out = FacilitiesUtils.createActivityFacilities();
        resetIDNumber();
        String idPrefix = (prefixFacilitiesB != null) ? prefixFacilitiesB + "_" : "";

        // copy facilities A:
        for (ActivityFacility facility : facilities_A.getFacilities().values()) {
            facilities_Out.addActivityFacility(facility);
        }
        // merge in facilities B:
        for (ActivityFacility facility : facilities_B.getFacilities().values()) {
            facilities_Out.addActivityFacility(getActivityFacility(facility, idPrefix + facility.getId().toString()));
        }

        return facilities_Out;
    }

    protected ActivityFacilities uniteFacilitiesWithAllNewIDs(ActivityFacilities facilities_A, ActivityFacilities facilities_B) {
        final ActivityFacilities facilities_Out = FacilitiesUtils.createActivityFacilities();
        resetIDNumber();

        Map<String, ActivityFacility> facilityPool = new HashMap<>();
        addFacilitiesToPool(facilityPool, facilities_A);
        addFacilitiesToPool(facilityPool, facilities_B);
        for (ActivityFacility facility : facilityPool.values()) {
            facilities_Out.addActivityFacility(getActivityFacility(facility, String.format("%06d", ++idNumber)));
        }

        return facilities_Out;
    }

    private ActivityFacilityImpl getActivityFacility(ActivityFacility facility, String id) {
        ActivityFacilityImpl newFacility = (ActivityFacilityImpl) factory.createActivityFacility(
                Id.create(id, ActivityFacility.class), facility.getCoord());
        if (!publicFacilities) {
            newFacility.setDesc(((ActivityFacilityImpl)facility).getDesc());
        }
        for (ActivityOption activity : facility.getActivityOptions().values()) {
            newFacility.addActivityOption(activity);
        }
        return newFacility;
    }

    private void addFacilitiesToPool(Map<String, ActivityFacility> facilityPool, ActivityFacilities facilities) {
        for (ActivityFacility facility : facilities.getFacilities().values()) {
            ActivityFacilityImpl facilityImpl = (ActivityFacilityImpl) facility;
            String facilityKey = getKey(facilityImpl);
            if (!facilityPool.containsKey(facilityKey)) {
                facilityPool.put(facilityKey, facilityImpl);
            } else {
                ActivityFacilityImpl poolFacility = (ActivityFacilityImpl) facilityPool.get(facilityKey);
                boolean containsMatchingActivity = false;
                for (String activity : facilityImpl.getActivityOptions().keySet()) {
                    if (poolFacility.getActivityOptions().containsKey(activity)) {
                        containsMatchingActivity = true;
                    }
                }
                if (!containsMatchingActivity) {
                    for (ActivityOption activity : facilityImpl.getActivityOptions().values()) {
                        poolFacility.addActivityOption(activity);
                    }
                    poolFacility.setDesc(poolFacility.getDesc() + ", " + facilityImpl.getDesc());
                } else {
                    facilityPool.put(facilityKey, facilityImpl);
                }

            }
        }
    }

    private String getKey(ActivityFacility facility) {
        return facility.getCoord().getX() + "_" + facility.getCoord().getY();
    }
}
