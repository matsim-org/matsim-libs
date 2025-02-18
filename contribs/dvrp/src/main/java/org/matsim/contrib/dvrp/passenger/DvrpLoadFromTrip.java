package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * This interface is to be bound per DRT mode. It allows to construct the
 * {@link DvrpLoad} object representing the occupancy consumed for a trip.
 * 
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface DvrpLoadFromTrip {
	DvrpLoad getLoad(Person person, Attributes tripAttributes);
}
