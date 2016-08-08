package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;

/** 
 * @author balac
 */
public class TwoWayCarsharingStation implements CarsharingStation {

	private Map<String, Integer> numberOfvehiclesPerType = new HashMap<String, Integer>();
	private Map<String, ArrayList<StationBasedVehicle>> vehiclesPerType = new HashMap<String, ArrayList<StationBasedVehicle>>();
	private String stationId;
	private Id<Link> linkId;

	public TwoWayCarsharingStation(String stationId, Link link, Map<String, Integer> numberOfvehiclesPerType,
			Map<String, ArrayList<StationBasedVehicle>> vehiclesPerType) {
		this.linkId = link.getId();
		this.stationId = stationId;
		this.numberOfvehiclesPerType = numberOfvehiclesPerType;
		this.vehiclesPerType = vehiclesPerType;

	}
	
	public int getNumberOfVehicles(String type) {
		
		return this.numberOfvehiclesPerType.get(type);
	}	
	
	public ArrayList<StationBasedVehicle> getVehicles(String type) {
		
		return this.vehiclesPerType.get(type);
	}	

	public void removeCar(String type, StationBasedVehicle vehicle) {
		
		ArrayList<StationBasedVehicle> currentVehicles = this.vehiclesPerType.get(type);		
		currentVehicles.remove(vehicle);
		int currentNumberOfVehicles = this.numberOfvehiclesPerType.get(type);
		currentNumberOfVehicles--;
		this.numberOfvehiclesPerType.put(type, currentNumberOfVehicles);		
	}
	
	public void addCar(String type, StationBasedVehicle vehicle){
		
		ArrayList<StationBasedVehicle> currentVehicles = this.vehiclesPerType.get(type);		
		currentVehicles.add(vehicle);
		int currentNumberOfVehicles = this.numberOfvehiclesPerType.get(type);
		currentNumberOfVehicles++;
		this.numberOfvehiclesPerType.put(type, currentNumberOfVehicles);	
	}

	public String getStationId() {
		return stationId;
	}	
	
	public Id<Link> getLinkId() {
		return linkId;
	}

	public Map<String, ArrayList<StationBasedVehicle>> getVehiclesPerType() {
		return vehiclesPerType;
	}
}