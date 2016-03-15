package playground.polettif.boescpa.analysis.trips;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides different methods to filter a trip file.
 *
 * @author boescpa
 */
public class TripFilter {

    /**
     * Returns all trips travelled with the specified purpose.
     *
     * @param trips The trips to filter.
     * @param purpose The purpose for which will be filtered.
     * @return An unmodifiable List of only the trips with the specified purpose.
     */
    public static List<Trip> purposeTripFilter(List<Trip> trips, String purpose) {
        List<Trip> filteredTrips = new LinkedList<>();
        for (Trip tempTrip : trips) {
            if (tempTrip.purpose.equals(purpose)) {
                filteredTrips.add(tempTrip.clone());
            }
        }
        return Collections.unmodifiableList(filteredTrips);
    }

    /**
     * Returns all trips travelled with the specified mode.
     *
     * @param trips The trips to filter.
     * @param mode The mode for which will be filtered.
     * @return An unmodifiable List of only the trips with the specified mode.
     */
    public static List<Trip> modalTripFilter(List<Trip> trips, String mode) {
        List<Trip> filteredTrips = new LinkedList<>();
        for (Trip tempTrip : trips) {
            if (tempTrip.mode.equals(mode)) {
                filteredTrips.add(tempTrip.clone());
            }
        }
        return Collections.unmodifiableList(filteredTrips);
    }

    /**
     * Returns all trips in the area defined by the cutter.
     *
     * @param trips The trips to filter.
     * @param cutter The spatial cutter specifying the resulting area.
     * @return An unmodifiable list of only the trips in the specified area.
     */
    public static List<Trip> spatialTripFilter(List<Trip> trips, SpatialTripCutter cutter) {
        List<Trip> filteredTrips = new LinkedList<>();
        for (Trip tempTrip : trips) {
            if (cutter.spatiallyConsideringTrip(tempTrip)) {
                filteredTrips.add(tempTrip.clone());
            }
        }
        return Collections.unmodifiableList(filteredTrips);
    }

    /**
     * Check for all found trips if they have failed. If so, remove them.
     *
     * @param trips The trips to clean.
     * @param failedAgents Into this list all ids of agents with unfinished trips will be added. May be null.
     * @return An unmodifiable list of only the finished trips.
     */
    public static List<Trip> removeUnfinishedTrips(List<Trip> trips, List<Id<Person>> failedAgents) {
        List<Trip> finishedTrips = new LinkedList<>();

        for (Trip tempTrip : trips) {
            if (!(tempTrip.endLinkId == null) && !tempTrip.purpose.equals("null") && !tempTrip.purpose.equals("stuck")) {
                finishedTrips.add(tempTrip.clone());
            } else {
                if (failedAgents != null) failedAgents.add(tempTrip.agentId);
            }
        }
        return Collections.unmodifiableList(finishedTrips);
    }

}
