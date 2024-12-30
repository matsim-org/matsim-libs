package org.matsim.contrib.drt.extension.insertion.constraints;

import org.matsim.contrib.drt.extension.insertion.DrtInsertionConstraint;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;

public class ExclusivityConstraint implements DrtInsertionConstraint {
	private final ExclusivityVoter voter;

	public ExclusivityConstraint(ExclusivityVoter voter) {
		this.voter = voter;
	}

	@Override
	public boolean checkInsertion(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		if (voter.isExclusive(drtRequest)) {
			return isValidInsertionForExclusiveRequest(drtRequest, insertion, detourTimeInfo);
		} else {
			return isValidInsertionForStandardRequest(drtRequest, insertion, detourTimeInfo);
		}
	}

	private boolean isValidInsertionForExclusiveRequest(DrtRequest drtRequest, Insertion insertion,
			DetourTimeInfo detourTimeInfo) {
		if (!insertion.pickup.previousWaypoint.getOutgoingOccupancy().isEmpty()) {
			// attempt to attach pickup to existing trip
			return false;
		}

		// dropoff must follow directly after pickup
		return insertion.pickup.index == insertion.dropoff.index;
	}

	private boolean isValidInsertionForStandardRequest(DrtRequest drtRequest, Insertion insertion,
			DetourTimeInfo detourTimeInfo) {
		if (insertion.pickup.index == 0) {
			if (insertion.vehicleEntry.stops.size() > 0) {
				Waypoint.Stop nextStop = insertion.vehicleEntry.stops.get(0);

				for (AcceptedDrtRequest dropoffRequest : nextStop.task.getDropoffRequests().values()) {
					if (voter.isExclusive(dropoffRequest.getRequest())) {
						// ongoing prebooking trip, we cannot insert at beginning of schedule
						return false;
					}
				}
			}
		}

		int minimumIndex = Math.max(0, insertion.pickup.index - 1);
		for (int index = minimumIndex; index < insertion.dropoff.index; index++) {
			Waypoint.Stop stop = insertion.vehicleEntry.stops.get(index);

			for (AcceptedDrtRequest pickupRequest : stop.task.getPickupRequests().values()) {
				if (voter.isExclusive(pickupRequest.getRequest())) {
					// covering a prebooked pickup
					return false;
				}
			}
		}

		return true;
	}

	static public interface ExclusivityVoter {
		boolean isExclusive(DrtRequest request);
	}
}
