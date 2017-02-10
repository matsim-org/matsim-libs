package playground.clruch.utils;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

public class VrpPathUtils {
    public static double getDistance(VrpPathWithTravelData vrpPathWithTravelData) {
        double distance = 0;
        for (Link link : vrpPathWithTravelData)
            distance += link.getLength();
        return distance;
    }

    public static String toString(VrpPathWithTravelData vrpPathWithTravelData) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Link link : vrpPathWithTravelData) {
            stringBuilder.append(link.getId().toString() + '\t');
            stringBuilder.append(link.getFromNode().getId().toString() + '\t');
            stringBuilder.append(link.getToNode().getId().toString() + '\n');
        }
        return stringBuilder.toString().trim();
    }

    public static boolean isConsistent(VrpPathWithTravelData vrpPathWithTravelData) {
        boolean status = true;
        Iterator<Link> iterator = vrpPathWithTravelData.iterator();
        Node node = iterator.next().getToNode();
        while (iterator.hasNext()) {
            Link link = iterator.next();
            status &= node.getId().equals(link.getFromNode().getId());
            node = link.getToNode();
        }
        return status;
    }

}
