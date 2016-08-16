package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface CarsharingStation {
	
	public String getStationId();
	
	//public int getNumberOfVehicles(String type);

	//public ArrayList<StationBasedVehicle> getVehicles(String type);
	
	//public void removeCar(String type, StationBasedVehicle vehicle);
	
	//public void addCar(String type, StationBasedVehicle vehicle);

	public Id<Link> getLinkId();
}
