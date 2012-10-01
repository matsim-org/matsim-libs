package playground.toronto.transitfares.fareclasser;

import org.matsim.api.core.v01.population.Person;

/**
 * Handles the transformation between person and their transit fare class.
 * 
 * @author pkucirek
 *
 */
public interface FareClasser {

	public String getFareClass(Person p);
}
