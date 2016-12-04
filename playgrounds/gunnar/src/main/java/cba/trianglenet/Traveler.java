package cba.trianglenet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class Traveler {

	final Id<Person> id;

	final Link homeLocation;

	Traveler(final Id<Person> id, final Link homeLocation) {
		this.id = id;
		this.homeLocation = homeLocation;
	}
}
