package org.matsim.contrib.carsharing.manager.supply;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public interface CompanyAgent {

	CSVehicle vehicleRequest(Id<Person> personId, Link locationLink, Link destinationLink, String carsharingType,
			String carType);

}
