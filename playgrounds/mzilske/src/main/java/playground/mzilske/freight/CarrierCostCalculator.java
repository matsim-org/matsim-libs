package playground.mzilske.freight;

import java.util.Collection;

public interface CarrierCostCalculator {
	public void run(CarrierVehicle carrierVehicle, Collection<Contract> contracts, CostMemory costMemory, Double totalCosts);
}