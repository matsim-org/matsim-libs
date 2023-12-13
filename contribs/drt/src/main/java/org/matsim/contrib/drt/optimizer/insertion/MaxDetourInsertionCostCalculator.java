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
		if (violatesDetour(insertion, insertion.vehicleEntry, drtRequest, detourTimeInfo)) {
			return INFEASIBLE_SOLUTION_COST;
		}
		return delegate.calculate(drtRequest, insertion, detourTimeInfo);
	}

	private boolean violatesDetour(InsertionGenerator.Insertion insertion, VehicleEntry vEntry, DrtRequest drtRequest, InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo) {
		// Check if the max travel time constraint for the newly inserted request is violated
		double rideDuration = detourTimeInfo.dropoffDetourInfo.arrivalTime - detourTimeInfo.pickupDetourInfo.departureTime;
		return drtRequest.getMaxRideDuration() < rideDuration;
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
