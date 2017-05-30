package org.matsim.contrib.carsharing.manager.demand;

import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public interface VehicleChoiceAgent {

	CSVehicle chooseVehicle(Set<CSVehicle> vehicleOptions, Link startLink);

}
