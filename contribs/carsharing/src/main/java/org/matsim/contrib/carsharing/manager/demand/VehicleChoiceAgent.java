package org.matsim.contrib.carsharing.manager.demand;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public interface VehicleChoiceAgent {

	CSVehicle chooseVehicle(List<CSVehicle> vehicleOptions, Link startLink, Leg leg, double currentTime, Person person);

	CSVehicle chooseVehicleActivityTimeIncluded(List<CSVehicle> vehicleOptions, Link startLink, Leg leg,
			double currentTime, Person person, double durationOfNextActivity, boolean keepthecar);

}
