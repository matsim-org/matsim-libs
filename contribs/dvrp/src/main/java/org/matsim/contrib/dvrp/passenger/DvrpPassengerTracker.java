package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.mobsim.dsim.NodeSingleton;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the information of all passengers across all passenger engines.
 */
@NodeSingleton
final class DvrpPassengerTracker {

	private final AtomicInteger currentRequestId = new AtomicInteger(-1);

	private final String mode;


	//accessed in doSimStep() and handleDeparture()
	final Map<Id<Request>, List<MobsimPassengerAgent>> activePassengers = new ConcurrentHashMap<>();

	// holds vehicle stop activities for requests that have not arrived at departure point yet
	final Map<Id<Request>, PassengerPickupActivity> waitingForPassenger = new ConcurrentHashMap<>();

	DvrpPassengerTracker(String mode) {
		this.mode = mode;
	}


	Id<Request> createRequestId() {
		return Id.create(mode + "_" + currentRequestId.incrementAndGet(), Request.class);
	}

}
