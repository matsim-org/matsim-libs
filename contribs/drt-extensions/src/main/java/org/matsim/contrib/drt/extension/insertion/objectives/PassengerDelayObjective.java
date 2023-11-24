package org.matsim.contrib.drt.extension.insertion.objectives;

import java.util.Collection;

import org.matsim.contrib.drt.extension.insertion.DrtInsertionObjective;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;

public class PassengerDelayObjective implements DrtInsertionObjective {
	private final double pickupWeight;
	private final double dropoffWeight;

	public PassengerDelayObjective(double pickupWeight, double dropoffWeight) {
		this.pickupWeight = pickupWeight;
		this.dropoffWeight = dropoffWeight;
	}

	@Override
	public double calculateObjective(DrtRequest request, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		VehicleEntry vehicleEntry = insertion.vehicleEntry;

		double totalPickupValue = detourTimeInfo.pickupDetourInfo.departureTime - request.getEarliestStartTime();
		double totalDropoffValue = detourTimeInfo.dropoffDetourInfo.arrivalTime - request.getEarliestStartTime();

		double remainingTimeLoss = detourTimeInfo.pickupDetourInfo.pickupTimeLoss;

		for (int i = insertion.pickup.index; i < insertion.dropoff.index; i++) {
			remainingTimeLoss -= insertion.vehicleEntry.getPrecedingStayTime(i);
			remainingTimeLoss = Math.max(0.0, remainingTimeLoss);

			totalPickupValue += getPassengers(vehicleEntry.stops.get(i).task.getPickupRequests().values())
					* remainingTimeLoss;
			totalDropoffValue += getPassengers(vehicleEntry.stops.get(i).task.getDropoffRequests().values())
					* remainingTimeLoss;
		}

		remainingTimeLoss += detourTimeInfo.dropoffDetourInfo.dropoffTimeLoss;

		for (int i = insertion.dropoff.index; i < vehicleEntry.stops.size(); i++) {
			remainingTimeLoss -= insertion.vehicleEntry.getPrecedingStayTime(i);
			remainingTimeLoss = Math.max(0.0, remainingTimeLoss);

			totalPickupValue += getPassengers(vehicleEntry.stops.get(i).task.getPickupRequests().values())
					* remainingTimeLoss;
			totalDropoffValue += getPassengers(vehicleEntry.stops.get(i).task.getDropoffRequests().values())
					* remainingTimeLoss;
		}

		return totalPickupValue * pickupWeight + totalDropoffValue * dropoffWeight;
	}

	private int getPassengers(Collection<AcceptedDrtRequest> requests) {
		return requests.stream().mapToInt(r -> r.getPassengerIds().size()).sum();
	}
}
