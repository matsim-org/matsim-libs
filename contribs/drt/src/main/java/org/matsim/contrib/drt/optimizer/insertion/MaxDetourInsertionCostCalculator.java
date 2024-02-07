package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * This insertion cost calculator performs additional check on the maximum ride duration, on top of the original InsertionCostCalculator
 * @author: Nico Kühnel (nkuehnel), Chengqi Lu (luchengqi7)
 *
 * The function is now implemented in the DefaultInsertionCostCalculator
 * */
@Deprecated
public class MaxDetourInsertionCostCalculator implements InsertionCostCalculator {
	private final InsertionCostCalculator delegate;

	public MaxDetourInsertionCostCalculator(InsertionCostCalculator delegate) {
		this.delegate = delegate;
	}

	@Override
	public double calculate(DrtRequest drtRequest, InsertionGenerator.Insertion insertion, InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo) {
		if (violatesDetour(drtRequest, detourTimeInfo)) {
			return INFEASIBLE_SOLUTION_COST;
		}
		return delegate.calculate(drtRequest, insertion, detourTimeInfo);
	}

	private boolean violatesDetour(DrtRequest drtRequest, InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo) {
		// Check if the max travel time constraint for the newly inserted request is violated
		double rideDuration = detourTimeInfo.dropoffDetourInfo.arrivalTime - detourTimeInfo.pickupDetourInfo.departureTime;
		return drtRequest.getMaxRideDuration() < rideDuration;
	}

}
