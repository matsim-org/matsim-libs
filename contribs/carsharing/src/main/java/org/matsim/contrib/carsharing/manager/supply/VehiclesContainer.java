package org.matsim.contrib.carsharing.manager.supply;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
/** 
 * @author balac
 */
public interface VehiclesContainer {
	
	public boolean reserveVehicle(CSVehicle vehicle);
	public void parkVehicle(CSVehicle vehicle, Link link);
	public Link getVehicleLocation(CSVehicle vehicle);
	public CSVehicle findClosestAvailableVehicle(Link startLink, String typeOfVehicle, double searchDistance);
	public Link findClosestAvailableParkingLocation(Link destinationLink, double searchDistance);
	public void reserveParking(Link destinationLink);

}
