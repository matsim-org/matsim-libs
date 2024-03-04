package org.matsim.contrib.drt.extension.insertion;

import java.util.List;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

public class ConfigurableCostCalculatorStrategy implements CostCalculationStrategy {
	private final List<DrtInsertionConstraint> constraints;
	private final DrtInsertionObjective objective;

	private final SoftInsertionParams softInsertionParams;

	public ConfigurableCostCalculatorStrategy(DrtInsertionObjective objective, List<DrtInsertionConstraint> constraints,
			@Nullable SoftInsertionParams softInsertionParams) {
		this.objective = objective;
		this.constraints = constraints;
		this.softInsertionParams = softInsertionParams;
	}

	private boolean checkHardInsertion(DrtRequest request, DetourTimeInfo detourTimeInfo) {
		return detourTimeInfo.pickupDetourInfo.departureTime <= request.getLatestStartTime()
				&& detourTimeInfo.dropoffDetourInfo.arrivalTime <= request.getLatestArrivalTime();
	}

	private double calculateSoftInsertionPenalty(DrtRequest request, DetourTimeInfo detourTimeInfo) {
		if (softInsertionParams == null) {
			return 0.0;
		} else {
			double pickupDelay = Math.max(0,
					detourTimeInfo.pickupDetourInfo.departureTime - request.getLatestStartTime());

			double dropoffDelay = Math.max(0,
					detourTimeInfo.dropoffDetourInfo.arrivalTime - request.getLatestArrivalTime());

			return softInsertionParams.pickupWeight * pickupDelay + softInsertionParams.dropoffWeight * dropoffDelay;
		}
	}

	@Override
	public double calcCost(DrtRequest request, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		if (softInsertionParams == null && !checkHardInsertion(request, detourTimeInfo)) {
			return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
		}

		for (DrtInsertionConstraint constraint : constraints) {
			if (!constraint.checkInsertion(request, insertion, detourTimeInfo)) {
				return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
			}
		}

		return objective.calculateObjective(request, insertion, detourTimeInfo)
				+ calculateSoftInsertionPenalty(request, detourTimeInfo);
	}

	static record SoftInsertionParams(double pickupWeight, double dropoffWeight) {
	}
}
