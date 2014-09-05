package org.matsim.roadpricing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public interface TollFactorI {

	public double getTollFactor(Id<Person> personId, Id<Vehicle> vehicleId, Id<Link> linkId, double time);

}