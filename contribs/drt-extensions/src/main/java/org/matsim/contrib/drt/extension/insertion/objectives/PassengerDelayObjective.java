package org.matsim.contrib.drt.extension.insertion.objectives;

import org.matsim.contrib.drt.extension.insertion.DrtInsertionObjective;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
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

			totalPickupValue += vehicleEntry.stops.get(i).task.getPickupRequests().size() * remainingTimeLoss;
			totalDropoffValue += vehicleEntry.stops.get(i).task.getDropoffRequests().size() * remainingTimeLoss;
		}

		remainingTimeLoss += detourTimeInfo.dropoffDetourInfo.dropoffTimeLoss;

		for (int i = insertion.dropoff.index; i < vehicleEntry.stops.size(); i++) {
			remainingTimeLoss -= insertion.vehicleEntry.getPrecedingStayTime(i);
			remainingTimeLoss = Math.max(0.0, remainingTimeLoss);

			totalPickupValue += vehicleEntry.stops.get(i).task.getPickupRequests().size() * remainingTimeLoss;
			totalDropoffValue += vehicleEntry.stops.get(i).task.getDropoffRequests().size() * remainingTimeLoss;
		}

		return totalPickupValue * pickupWeight + totalDropoffValue * dropoffWeight;
	}
}
