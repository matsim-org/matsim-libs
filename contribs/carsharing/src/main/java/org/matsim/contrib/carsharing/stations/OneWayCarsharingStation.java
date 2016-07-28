package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;


public class OneWayCarsharingStation extends AbstractCarSharingStation {

	private Map<String, Integer> vehiclesPerType = new HashMap<String, Integer>();
	private Map<String, ArrayList<StationBasedVehicle>> vehicleIDsPerType = new HashMap<String, ArrayList<StationBasedVehicle>>();
	private String stationId;
	private int avaialbleParkingSpots;


	public Map<String, ArrayList<StationBasedVehicle>> getVehicleIDsPerType() {
		return vehicleIDsPerType;
	}

	public OneWayCarsharingStation(String stationId, Link link, Map<String, Integer> vehiclesPerType,
			Map<String, ArrayList<StationBasedVehicle>> vehicleIDsPerType, int availableParkingSpots) {
		super(link) ;
		this.stationId = stationId;
		this.vehiclesPerType = vehiclesPerType;
		this.vehicleIDsPerType = vehicleIDsPerType;
		this.setAvaialbleParkingSpots(availableParkingSpots);

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
	
}