package org.matsim.contrib.ev.strategic.costs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * This cost calculator implementation retrieves the cost structure from the
 * configuration.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DefaultChargingCostCalculator implements ChargingCostCalculator {
	private final DefaultChargingCostsParameters parameters;

	public DefaultChargingCostCalculator(DefaultChargingCostsParameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public double calculateChargingCost(Id<Person> personId, Id<Charger> charger, double duration, double energy) {
		double blockingDuration_min = Math.max(duration / 60.0 - parameters.blockingDuration_min, 0.0);
		return duration / 60.0 * parameters.costPerDuration_min
				+ blockingDuration_min * parameters.costPerBlockingDuration_min +
				+EvUnits.J_to_kWh(energy) * parameters.costPerEnergy_kWh + parameters.costPerUse;
	}
}
