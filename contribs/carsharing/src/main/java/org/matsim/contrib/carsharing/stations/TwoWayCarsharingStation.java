package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

/** 
 * @author balac
 */
public class TwoWayCarsharingStation implements CarsharingStation {

	private Map<String, Integer> numberOfvehiclesPerType = new HashMap<String, Integer>();
	private Map<String, ArrayList<CSVehicle>> vehiclesPerType = new HashMap<String, ArrayList<CSVehicle>>();
	private String stationId;
	private Link link;

	public TwoWayCarsharingStation(String stationId, Link link, Map<String, Integer> numberOfvehiclesPerType,
			Map<String, ArrayList<CSVehicle>> vehiclesPerType) {
		this.link = link;
		this.stationId = stationId;
		this.numberOfvehiclesPerType = numberOfvehiclesPerType;
		this.vehiclesPerType = vehiclesPerType;

	}
	
	public int getNumberOfVehicles(String type) {
		if (this.numberOfvehiclesPerType.containsKey(type))
			return this.numberOfvehiclesPerType.get(type);
		else
			return 0;
	}	
	
	public ArrayList<CSVehicle> getVehicles(String type) {
		
		return this.vehiclesPerType.get(type);
	}	

	public void removeCar(String type, CSVehicle vehicle) {
		
		ArrayList<CSVehicle> currentVehicles = this.vehiclesPerType.get(type);		
		currentVehicles.remove(vehicle);
		int currentNumberOfVehicles = this.numberOfvehiclesPerType.get(type);
		currentNumberOfVehicles--;
		this.numberOfvehiclesPerType.put(type, currentNumberOfVehicles);		
	}
	
	public void addCar(String type, CSVehicle vehicle){
		
		ArrayList<CSVehicle> currentVehicles = this.vehiclesPerType.get(type);		
		currentVehicles.add(vehicle);
		int currentNumberOfVehicles = this.numberOfvehiclesPerType.get(type);
		currentNumberOfVehicles++;
		this.numberOfvehiclesPerType.put(type, currentNumberOfVehicles);	
	}
	@Override
	public String getStationId() {
		return stationId;
	}	
	@Override
	public Link getLink() {
		return link;
	}

	public Map<String, ArrayList<CSVehicle>> getVehiclesPerType() {
		return vehiclesPerType;
	}
	
	public boolean removeCar(CSVehicle vehicle) {
		String type = vehicle.getType();
		ArrayList<CSVehicle> currentVehicles = this.vehiclesPerType.get(type);		
		
		int currentNumberOfVehicles = this.numberOfvehiclesPerType.get(type);
		currentNumberOfVehicles--;
		this.numberOfvehiclesPerType.put(type, currentNumberOfVehicles);		
		return currentVehicles.remove(vehicle);
	}
}