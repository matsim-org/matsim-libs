package ch.sbb.matsim.routing.pt.raptor;

import java.util.function.Supplier;

/**
 * @author mrieser / Simunto
 */
public interface RaptorTransferCostCalculator {

	double calcTransferCost(Supplier<Transfer> transfer, RaptorParameters raptorParams, int totalTravelTime, int totalTransferCount, double existingTransferCosts, double currentTime);

}
