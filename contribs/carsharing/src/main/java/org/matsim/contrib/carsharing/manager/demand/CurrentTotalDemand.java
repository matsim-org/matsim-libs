package org.matsim.contrib.carsharing.manager.demand;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public interface CurrentTotalDemand {

	boolean hasVehicleOnLink(Id<Person> personId, Link link, String type);

	CSVehicle getVehicleOnLink(Id<Person> personId, Link link, String type);

	boolean addVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type);

	boolean removeVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type);

	void reset();

}