package org.matsim.contrib.ev.strategic.costs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * This cost calculator implementation always returns zero costs.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class NoChargingCostCalculator implements ChargingCostCalculator {
	@Override
	public double calculateChargingCost(Id<Person> personId, Id<Charger> charger, double duration, double energy) {
		return 0;
	}
}
