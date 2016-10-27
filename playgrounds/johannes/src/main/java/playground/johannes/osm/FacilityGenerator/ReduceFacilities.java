package playground.johannes.osm.FacilityGenerator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by NicoKuehnel on 13.10.2016.
 */
public class ReduceFacilities {

    public static void main(String[] args) {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        FacilitiesReaderMatsimV1 reader = new FacilitiesReaderMatsimV1(scenario);
        reader.readFile(args[0]);

        int nth = Integer.parseInt(args[2]);

        List<Id<ActivityFacility>> ids2Remove = new ArrayList<>();

        if (args[3].equals("d")) {
            System.out.println(String.format("Removing every %dth facility", nth));

            int i = 0;

            for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
                if (facility.getActivityOptions().containsKey("home")) {
                    if (i % nth == 0) {
                        ids2Remove.add(facility.getId());
                    }
                }
                i++;
            }


        } else if (args[3].equals("k")) {
            System.out.println(String.format("Keeping every %dth facility", nth));

            int i = 0;

            for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
                if (facility.getActivityOptions().containsKey("home")) {
                    if (i % nth != 0) {
                        ids2Remove.add(facility.getId());
                    }
                }
                i++;
            }

        }

        System.out.println(String.format("removed %d home facilities", ids2Remove.size()));

        for (Id<ActivityFacility> id : ids2Remove) {
            scenario.getActivityFacilities().getFacilities().remove(id);
        }

        FacilitiesWriter writer = new FacilitiesWriter(scenario.getActivityFacilities());
        writer.write(args[1]);
    }
}
