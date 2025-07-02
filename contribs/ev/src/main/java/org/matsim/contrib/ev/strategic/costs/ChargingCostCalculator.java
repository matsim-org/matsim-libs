package org.matsim.contrib.ev.strategic.costs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * This interface calculates the cost for charging.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargingCostCalculator {
	double calculateChargingCost(Id<Person> personId, Id<Charger> charger, double duration,
			double energy);
}
