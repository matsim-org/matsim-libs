package org.matsim.contrib.ev.strategic.access;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * This implementation gives access to every person to every charger.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AnyChargerAccess implements ChargerAccess {
	@Override
	public boolean hasAccess(Person person, Charger charger) {
		return true;
	}

	@Override
	public boolean hasAccess(Person person, ChargerSpecification charger) {
		return true;
	}
}
