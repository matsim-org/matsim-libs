package org.matsim.contrib.carsharing.manager.supply;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
/** 
 * @author balac
 */
public class CompanyContainer {
	private String companyId;
	private Set<String> carsharingTypes = new TreeSet<String>();
	
	private Map<String, VehiclesContainer> carsharingContainers = new HashMap<String, VehiclesContainer>();
	
	public CompanyContainer(String companyId) {
		
		this.companyId = companyId;
	}	
	
	public void addCarsharingType(String carsharingType, VehiclesContainer vehiclesContainer) {
		
		this.carsharingContainers.put(carsharingType, vehiclesContainer);
		this.carsharingTypes.add(carsharingType);
	}
	
	public VehiclesContainer getVehicleContainer(String carsharingType) {
		
		return this.carsharingContainers.get(carsharingType);
	}
	
	public boolean reserveVehicle(CSVehicle vehicle) {
		return this.carsharingContainers.get(vehicle.getCsType()).reserveVehicle(vehicle);
	}
	
	public void parkVehicle(CSVehicle vehicle, Link link) {
		
		this.carsharingContainers.get(vehicle.getCsType()).parkVehicle(vehicle, link);
	}
	
	public void reserveParkingSlot(CarsharingStation parkingStation) {

		((OneWayCarsharingStation)parkingStation).reserveParkingSpot();		
	}

	public String getCompanyId() {
		return companyId;
	}
	
}
