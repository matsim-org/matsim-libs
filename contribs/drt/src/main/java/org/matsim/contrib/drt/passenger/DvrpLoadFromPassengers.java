package org.matsim.contrib.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLoad;

import java.util.Collection;

public interface DvrpLoadFromPassengers {

	DvrpVehicleLoad getLoad(Collection<Id<Person>> personIds);
}
