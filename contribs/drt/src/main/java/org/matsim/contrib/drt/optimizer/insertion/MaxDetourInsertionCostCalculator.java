package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;

import java.util.HashMap;
import java.util.Map;

public class MaxDetourInsertionCostCalculator implements InsertionCostCalculator, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler {

	private final Map<Id<Request>, Double> ongoingRequests = new HashMap<>();

	private final InsertionCostCalculator delegate;

	public MaxDetourInsertionCostCalculator(InsertionCostCalculator delegate) {
		this.delegate = delegate;
	}

	@Override
	public double calculate(DrtRequest drtRequest, InsertionGenerator.Insertion insertion, InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo) {
		if (violatesDetour(insertion, insertion.vehicleEntry, drtRequest)) {
			return INFEASIBLE_SOLUTION_COST;
		}
		return delegate.calculate(drtRequest, insertion, detourTimeInfo);
	}

	private boolean violatesDetour(InsertionGenerator.Insertion insertion, VehicleEntry vEntry, DrtRequest drtRequest) {
		double travelTime = insertion.dropoff.newWaypoint.getArrivalTime() - insertion.pickup.newWaypoint.getDepartureTime();
		if(drtRequest.getMaxTravelTime() < travelTime) {
			return true;
		}

		final int pickupIdx = insertion.pickup.index;

		Map<Id<Request>, Double> pickUps = new HashMap<>(ongoingRequests);

		for (int s = pickupIdx; s < vEntry.stops.size(); s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);

			for (AcceptedDrtRequest pickup : stop.task.getPickupRequests().values()) {
				// consider adding stop duration?
				pickUps.put(pickup.getId(), stop.getArrivalTime());
			}

			for (AcceptedDrtRequest dropOff : stop.task.getDropoffRequests().values()) {
				double arrival = stop.getArrivalTime();
				double departure = pickUps.get(dropOff.getRequest().getId());
				double time = arrival - departure;
				if (dropOff.getMaxTravelTime() < time) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		ongoingRequests.remove(event.getRequestId());
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent event) {
		ongoingRequests.put(event.getRequestId(), event.getTime());
	}
}
