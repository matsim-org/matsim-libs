package playground.boescpa.analysis.trips;

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
     * @param failedAgents Into this list all ids of agents with unfinished trips will be added.
     * @return An unmodifiable list of only the finished trips.
     */
    public static List<Trip> removeUnfinishedTrips(List<Trip> trips, List<Id<Person>> failedAgents) {
        List<Trip> finishedTrips = new LinkedList<>();

        for (Trip tempTrip : trips) {
            if (!(tempTrip.endLinkId == null) && !tempTrip.purpose.equals("null") && !tempTrip.purpose.equals("stuck")) {
                finishedTrips.add(tempTrip.clone());
            } else {
                failedAgents.add(tempTrip.agentId);
            }
        }
        return Collections.unmodifiableList(finishedTrips);
    }

}
