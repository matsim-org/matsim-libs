// code by jph
package playground.clruch.utils;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.VrpPath;

/** print out links and nodes */
public enum VrpPathUtils {
    ;
    public static double getDistance(VrpPath vrpPath) {
        double distance = 0;
        for (Link link : vrpPath)
            distance += link.getLength();
        return distance;
    }

    public static String toString(VrpPath vrpPath) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Link link : vrpPath) {
            stringBuilder.append(link.getId().toString() + '\t');
            stringBuilder.append(link.getFromNode().getId().toString() + '\t');
            stringBuilder.append(link.getToNode().getId().toString() + '\n');
        }
        return stringBuilder.toString().trim();
    }

    public static boolean isConsistent(VrpPath vrpPath) {
        boolean status = true;
        Iterator<Link> iterator = vrpPath.iterator();
        Node node = iterator.next().getToNode();
        while (iterator.hasNext()) {
            Link link = iterator.next();
            status &= node.getId().equals(link.getFromNode().getId());
            node = link.getToNode();
        }
        return status;
    }
}
