package org.matsim.contrib.ev.strategic.access;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * This interface decides whether a person has access to a specific charger.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargerAccess {
	boolean hasAccess(Person person, Charger charger);

	boolean hasAccess(Person person, ChargerSpecification charger);
}
