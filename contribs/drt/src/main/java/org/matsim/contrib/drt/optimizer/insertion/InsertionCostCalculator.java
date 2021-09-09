package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.passenger.DrtRequest;

public interface InsertionCostCalculator<D> {

	double INFEASIBLE_SOLUTION_COST = Double.POSITIVE_INFINITY;

	double calculate(DrtRequest drtRequest, InsertionWithDetourData<D> insertion);

	class DetourTimeInfo {
		// expected departure time for the new request
		public final double departureTime;
		// expected arrival time for the new request
		public final double arrivalTime;
		// time delay of each stop placed after the pickup insertion point
		public final double pickupTimeLoss;
		// ADDITIONAL time delay of each stop placed after the dropoff insertion point
		public final double dropoffTimeLoss;

		public DetourTimeInfo(double departureTime, double arrivalTime, double pickupTimeLoss, double dropoffTimeLoss) {
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
			this.pickupTimeLoss = pickupTimeLoss;
			this.dropoffTimeLoss = dropoffTimeLoss;
		}

		// TOTAL time delay of each stop placed after the dropoff insertion point
		// (this is the amount of extra time the vehicle will operate if this insertion is applied)
		public double getTotalTimeLoss() {
			return pickupTimeLoss + dropoffTimeLoss;
		}
	}
}
