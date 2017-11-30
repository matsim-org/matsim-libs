package playground.lsieber.networkshapecutter;

import org.matsim.facilities.ActivityFacilities;

interface FacilitiesCutter {
    ActivityFacilities filter(ActivityFacilities facilities);
}
