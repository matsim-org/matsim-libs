package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
/** 
 * @author balac
 */

public class OneWayCarsharingStation implements CarsharingStation{

	private Map<String, Integer> numberOfvehiclesPerType = new HashMap<String, Integer>();
	private Map<String, ArrayList<CSVehicle>> vehiclesPerType = new HashMap<String, ArrayList<CSVehicle>>();
	private String stationId;
	private int avaialbleParkingSpots;
	private Link link;

	public OneWayCarsharingStation(String stationId, Link link, Map<String, Integer> numberOfvehiclesPerType,
			Map<String, ArrayList<CSVehicle>> vehiclesPerType, int availableParkingSpots) {
		this.link = link;
		this.stationId = stationId;
		this.numberOfvehiclesPerType = numberOfvehiclesPerType;
		this.vehiclesPerType = vehiclesPerType;
		this.setAvaialbleParkingSpots(availableParkingSpots);

	}
	
	public int getNumberOfVehicles(String type) {
		
		return this.numberOfvehiclesPerType.get(type);
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
	
	private void setAvaialbleParkingSpots(int i) {

		this.avaialbleParkingSpots = i;
	}

	public void reserveParkingSpot() {

		this.setAvaialbleParkingSpots(this.getAvaialbleParkingSpots() - 1);
	}	

	public void freeParkingSpot() {
		this.setAvaialbleParkingSpots(this.getAvaialbleParkingSpots() + 1);
	}

	public int getAvaialbleParkingSpots() {
		return avaialbleParkingSpots;
	}
	@Override
	public Link getLink() {
		return link;
	}

	public Map<String, ArrayList<CSVehicle>> getVehiclesPerType() {
		return vehiclesPerType;
	}

	public void removeCar(CSVehicle vehicle) {
		String type = ((StationBasedVehicle)vehicle).getVehicleType();
		ArrayList<CSVehicle> currentVehicles = this.vehiclesPerType.get(type);		
		currentVehicles.remove(vehicle);
		int currentNumberOfVehicles = this.numberOfvehiclesPerType.get(type);
		currentNumberOfVehicles--;
		this.numberOfvehiclesPerType.put(type, currentNumberOfVehicles);		
		
	}
	
}