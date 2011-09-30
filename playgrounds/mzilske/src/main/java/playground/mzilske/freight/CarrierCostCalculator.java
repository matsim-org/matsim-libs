package playground.mzilske.freight;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

public interface CarrierCostCalculator {
	public void run(Id depotLocation, Collection<CarrierContract> contracts, Double totalCosts);
}