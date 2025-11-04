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
	private final DynamicEnergyCosts dynamicCosts;

	public DefaultChargingCostCalculator(DefaultChargingCostsParameters parameters) {
		this.parameters = parameters;

		this.dynamicCosts = parameters.getDynamicCostPerEnergy_kWh() == null ? null
				: DynamicEnergyCosts.parse(parameters.getDynamicCostPerEnergy_kWh());
	}

	@Override
	public double calculateChargingCost(Id<Person> personId, Id<Charger> charger, double startTime, double duration,
			double energy) {
		double blockingDuration_min = Math.max(duration / 60.0 - parameters.getBlockingDuration_min(), 0.0);
		double energy_kWh = EvUnits.J_to_kWh(energy);

		return duration / 60.0 * parameters.getCostPerDuration_min() // duration-based
				+ (dynamicCosts == null ? 0.0 : dynamicCosts.calculate(startTime, duration, energy_kWh)) // dynamic
																											// costs
				+ blockingDuration_min * parameters.getCostPerBlockingDuration_min() // blocking duration
				+ energy_kWh * parameters.getCostPerEnergy_kWh() // energy-based cost
				+ parameters.getCostPerUse(); // cost per use
	}

	@Override
	public double calculateReservationCost(Id<Person> personId, Id<Charger> charger, double duration) {
		return parameters.getCostPerReservation();
	}
}
