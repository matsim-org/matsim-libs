package pharma;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.facilities.algorithms.WorldConnectLocations;

/**
 * Created by schroeder on 02/11/15.
 */
public class DistributionCenters {

    static class DistributionCenter extends ActivityFacilityImpl {

        protected DistributionCenter(Id<ActivityFacility> id, Coord center) {
            super(id, center, null);
        }
    }

    public static void main(String[] args) {
        //52.428254,13.528745
        ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities("distribution centers");
        ActivityFacilityImpl fac = new DistributionCenter(Id.create("dc",ActivityFacility.class),GeoUtils.transform(13.528745,52.428254));
        fac.addActivityOption(new ActivityOptionImpl("pickup"));
        facilities.addActivityFacility(fac);
        new FacilitiesWriter(facilities).write("output/dcs.xml");
    }

}
