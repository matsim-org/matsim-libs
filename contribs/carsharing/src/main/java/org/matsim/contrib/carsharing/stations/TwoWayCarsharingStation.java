package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;


public class TwoWayCarsharingStation extends AbstractCarSharingStation {

	private Map<String, Integer> vehiclesPerType = new HashMap<String, Integer>();
	private Map<String, ArrayList<StationBasedVehicle>> vehicleIDsPerType = new HashMap<String, ArrayList<StationBasedVehicle>>();
	private String stationId;


	public Map<String, ArrayList<StationBasedVehicle>> getVehicleIDsPerType() {
		return vehicleIDsPerType;
	}

	public TwoWayCarsharingStation(String stationId, Link link, Map<String, Integer> vehiclesPerType,
			Map<String, ArrayList<StationBasedVehicle>> vehicleIDsPerType) {
		super(link) ;
		this.stationId = stationId;
		this.vehiclesPerType = vehiclesPerType;
		this.vehicleIDsPerType = vehicleIDsPerType;

	}
	
	public int getNumberOfVehicles(String type) {
		
		return this.vehiclesPerType.get(type);
	}	
	
	public ArrayList<StationBasedVehicle> getVehicles(String type) {
		
		return this.vehicleIDsPerType.get(type);
	}	

	public void removeCar(String type, StationBasedVehicle vehicle) {
		
		ArrayList<StationBasedVehicle> currentVehicles = this.vehicleIDsPerType.get(type);		
		currentVehicles.remove(vehicle);
		int currentNumberOfVehicles = this.vehiclesPerType.get(type);
		currentNumberOfVehicles--;
		this.vehiclesPerType.put(type, currentNumberOfVehicles);		
	}
	
	public void addCar(String type, StationBasedVehicle vehicle){
		
		ArrayList<StationBasedVehicle> currentVehicles = this.vehicleIDsPerType.get(type);		
		currentVehicles.add(vehicle);
		int currentNumberOfVehicles = this.vehiclesPerType.get(type);
		currentNumberOfVehicles++;
		this.vehiclesPerType.put(type, currentNumberOfVehicles);	
	}

	public String getStationId() {
		return stationId;
	}	
	
}