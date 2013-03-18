package playground.toronto.transitfares.fareclasser;

import org.matsim.api.core.v01.population.Person;

public interface FarePerceiver {
	
	public double getFarePerception(Person p);
	
}
