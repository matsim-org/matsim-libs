package org.matsim.contrib.carsharing.manager;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public interface CSPersonVehicle {
	
	public void addNewPersonInfo(Id<Person> personId);
	public Map<Id<Link>, CSVehicle> getVehicleLocationForType(Id<Person> personId, String type);
	public boolean addVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type);
	public boolean removeVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type);
	public void addOriginForTW(Id<Person> personId, Link link, CSVehicle vehicle);
	public void reset();

}
