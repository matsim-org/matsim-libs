package playground.toronto.transitfares.fareclasser;

import org.matsim.api.core.v01.population.Person;

/**
 * Handles the transformation between person and their transit fare class.
 * 
 * @author pkucirek
 *
 */
public interface FareClasser {

	/**
	 * Determines which fare class a {@link Person} belongs to.
	 * 
	 * @param p The {@link Person} to consider.
	 * @return The fare class name the peron belongs to.
	 */
	public String getFareClass(Person p);
}
