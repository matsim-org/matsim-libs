package org.matsim.contrib.transEnergySim.agents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;

public interface VehicleAgent {

	Id<Person> getPersonId();
	VehicleWithBattery getVehicle();
}
