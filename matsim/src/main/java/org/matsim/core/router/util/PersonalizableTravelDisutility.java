package org.matsim.core.router.util;

import org.matsim.api.core.v01.population.Person;

public interface PersonalizableTravelDisutility extends TravelDisutility {
	
	void setPerson(Person person);

}
