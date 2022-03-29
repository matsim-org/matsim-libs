package ch.sbb.matsim.routing.pt.raptor;

import java.util.function.Supplier;

/**
 * @author mrieser / Simunto
 */
public class DefaultRaptorTransferCostCalculator implements RaptorTransferCostCalculator {
	@Override
	public double calcTransferCost(Supplier<Transfer> transfer, RaptorParameters raptorParams, int totalTravelTime, int transferCount, double existingTransferCosts, double currentTime) {
		double transferCostBase = raptorParams.getTransferPenaltyFixCostPerTransfer();
		double transferCostPerHour = raptorParams.getTransferPenaltyPerTravelTimeHour();
		double transferCostMin = raptorParams.getTransferPenaltyMinimum();
		double transferCostMax = raptorParams.getTransferPenaltyMaximum();

		return (calcSingleTransferCost(transferCostBase, transferCostPerHour, transferCostMin, transferCostMax, totalTravelTime) * transferCount) - existingTransferCosts;
	}

	private double calcSingleTransferCost(double costBase, double costPerHour, double costMin, double costMax, double travelTime) {
		double cost = costBase + costPerHour / 3600 * travelTime;
		double max = Math.max(costMin, costMax);
		double min = Math.min(costMin, costMax);
		if (cost > max) {
			cost = max;
		}
		if (cost < min) {
			cost = min;
		}
		return cost;
	}

}
