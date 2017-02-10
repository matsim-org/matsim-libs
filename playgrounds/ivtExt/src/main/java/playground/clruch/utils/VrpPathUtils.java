package playground.clruch.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

public class VrpPathUtils {
    public static double getDistance(VrpPathWithTravelData dropoffPath) {
        double distance = 0;
        for (Link link : dropoffPath)
            distance += link.getLength();
        return distance;
    }

}
