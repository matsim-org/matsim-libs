package org.matsim.core.router.util;

import org.matsim.api.core.v01.population.Person;

public interface PersonalizableTravelCost extends TravelCost {
	
	void setPerson(Person person);

}
