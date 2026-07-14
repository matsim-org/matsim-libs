package org.matsim.core.router.util;

import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class LeastCostPathUtils {
    private LeastCostPathUtils() {
    }

    public enum NoPathBehavior {
        warning, exception, none
    }

    static public void handleNotFound(NoPathBehavior behaviour, Logger logger, Node fromNode, Node toNode,
            Person person, Vehicle vehicle) {
        String info = "";
        info += " fromNode=" + fromNode.getId();
        info += " toNode=" + toNode.getId();

        handleNotFound(behaviour, logger, info, person, vehicle);
    }

    static public void handleNotFound(NoPathBehavior behaviour, Logger logger, Link fromLink, Link toLink,
            Person person, Vehicle vehicle) {
        String info = "";
        info += " fromLink=" + fromLink.getId();
        info += " toLink=" + toLink.getId();

        handleNotFound(behaviour, logger, info, person, vehicle);
    }

    static private void handleNotFound(NoPathBehavior behaviour, Logger logger, String info,
            Person person, Vehicle vehicle) {
        if (behaviour.equals(NoPathBehavior.warning) || behaviour.equals(NoPathBehavior.exception)) {
            if (person != null) {
                info += " person=" + person.getId();
            }

            if (vehicle != null) {
                info += " vehicle=" + vehicle.getId();
            }

            logger.warn("No route was found: " + info);
            logger.warn("Some possible reasons:");
            logger.warn(
                    "  * Network is not connected.  Run NetworkUtils.cleanNetwork(Network network, Set<String> modes)s.");
            logger.warn(
                    "  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
            logger.warn(
                    "  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
            logger.warn(
                    "  * The method is used to test whether a connection exists. Consider using NoPathBehavior.none in your router.");
            logger.warn("This will now return null, but it may fail later with a null pointer exception.");
        }

        if (behaviour.equals(NoPathBehavior.exception)) {
            throw new NoLeastCostPathException();
        }
    }

    static public class NoLeastCostPathException extends RuntimeException {
        public NoLeastCostPathException() {
            super("See warnings about least-cost path that could not be calculated.");
        }
    }
}
