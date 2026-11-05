package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.load.DvrpLoad;

/**
 * Helpers for tests that need an {@link InsertionGenerator.Insertion} without a full vehicle schedule.
 */
public final class InsertionTestUtils {
	private InsertionTestUtils() {
	}

	/**
	 * Creates an insertion whose pickup and dropoff waypoints carry the given request (so that
	 * {@link InsertionGenerator.Insertion#getRequest()} returns it), with no previous or next waypoints.
	 */
	public static InsertionGenerator.Insertion insertionFor(DrtRequest request, VehicleEntry vehicleEntry,
			int pickupIdx, int dropoffIdx, DvrpLoad load) {
		var pickup = new InsertionGenerator.InsertionPoint(pickupIdx, null, new Waypoint.Pickup(request), null);
		var dropoff = new InsertionGenerator.InsertionPoint(dropoffIdx, null, new Waypoint.Dropoff(request), null);
		return new InsertionGenerator.Insertion(vehicleEntry, pickup, dropoff, load);
	}

	public static InsertionGenerator.Insertion insertionFor(DrtRequest request, VehicleEntry vehicleEntry,
			DvrpLoad load) {
		return insertionFor(request, vehicleEntry, -1, -1, load);
	}
}
