package org.matsim.contrib.carsharing.manager.demand;

import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

public class VehicleChoiceAgentImpl implements VehicleChoiceAgent {
	
	@Inject private CarsharingSupplyInterface carsharingSupplyContainer;	
	
	@Override
	public CSVehicle chooseVehicle(Set<CSVehicle> vehicleOptions, Link startLink, Leg leg, double currentTime, Person person) {
		
		double distance = -1.0;
		CSVehicle chosenVehicle = null;
		for (CSVehicle vehicle : vehicleOptions) {
			
			Link vehicleLocation = this.carsharingSupplyContainer.getAllVehicleLocations().get(vehicle);
			
			double distanceCurr = CoordUtils.calcEuclideanDistance(vehicleLocation.getCoord(), startLink.getCoord());
			
			if (distance == -1.0 || distanceCurr < distance) 
				chosenVehicle = vehicle;
		}
		
		return chosenVehicle;		
	}

	@Override
	public CSVehicle chooseVehicleActivityTimeIncluded(Set<CSVehicle> vehicleOptions, Link startLink, Leg leg,
			double currentTime, Person person, double durationOfNextActivity, boolean keepthecar) {
		// TODO Auto-generated method stub
		return null;
	}
}
