package playground.polettif.boescpa.analysis.trips;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.List;

/**
 * Provides utility methods for the work with links.
 *
 * @author boescpa
 */
public class TripUtils {

    /**
     * Calculates the travel distance for a given trip.
     *
     * If no path is provided (path == null), the Manhattan distance between the start and the end link is returned.
     * If the trip wasn't finished (endLink == null), the path length is not calculated (return 0).
     *
     * @param path		of the trip
     * @param network	of the simulation
     * @param startLink	of the trip
     * @param endLink	of the trip
     * @return	path length of the trip [m]
     */
    public static double calcTravelDistance(List<Id<Link>> path, Network network, Id startLink, Id endLink) {
        // If the trip wasn't finished (endLink == null), the path length is not calculated.
        if (endLink == null) {
            return 0;
        }

        double travelDistance = 0;
        if (path != null && path.size() > 0) {
            // if a path was recorded, use the actual path for travel-distance calculation
            for (Id linkId : path) {
                travelDistance += network.getLinks().get(linkId).getLength();
            }
        } else {
            // if no path available, use euclidean distance as estimation for travel-distance
            Coord coordsStartLink = network.getLinks().get(startLink).getCoord();
            Coord coordsEndLink = network.getLinks().get(endLink).getCoord();
            travelDistance += (int) Math.sqrt(
                    ((coordsEndLink.getX() - coordsStartLink.getX())*(coordsEndLink.getX() - coordsStartLink.getX()))
                            + ((coordsEndLink.getY() - coordsStartLink.getY())*(coordsEndLink.getY() - coordsStartLink.getY())));
            // and scale it with a factor to account for non-euclidean detours on "real" path (<-> Manhattan distance)
            travelDistance *= Math.sqrt(2);
        }
        return travelDistance;
    }

    /**
     * Calculates the travel time for a given trip.
     *
     * @param startTime	of the trip [sec]
     * @param endTime	of the trip [sec]
     * @return	total travel time of the trip [sec]
     */
    public static double calcTravelTime(Double startTime, Double endTime) {
        return endTime - startTime;
    }

}
