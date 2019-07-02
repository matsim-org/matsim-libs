package org.matsim.contrib.roadpricing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Interface to let base toll amounts be in/deflated by a certain factor. This
 * may be based on the individual, vehicle, specific link, or the time of day,
 * or combinations thereof.
 *
 * @author nagel, jwjoubert
 */
public interface TollFactor {

	public double getTollFactor(Id<Person> personId, Id<Vehicle> vehicleId, Id<Link> linkId, double time);

}