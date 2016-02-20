package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;

public final class OneWayCarsharingStation extends AbstractCarSharingStation {

	
	private int numberOfVehicles;
	private int availableParkingSpaces;
	private ArrayList<String> vehicleIDs = new ArrayList<String>();

	
	public OneWayCarsharingStation(Link link, int numberOfVehicles, ArrayList<String> vehicleIDs, int availableParkingSpaces) {
		super(link) ;
		
		this.numberOfVehicles = numberOfVehicles;
		this.availableParkingSpaces = availableParkingSpaces;
		this.vehicleIDs = vehicleIDs;

	}
	
	public int getNumberOfVehicles() {
		
		return numberOfVehicles;
	}
	
	public int getNumberOfAvailableParkingSpaces() {
		
		return this.availableParkingSpaces ;
	}
	
	public void removeCar() {
		this.numberOfVehicles--;
	}
	
	public void addCar(){
		
		this.numberOfVehicles++;
	}
	
	public void reserveParkingSpot() {
		availableParkingSpaces--;
		
	}
	
	public void freeParkingSpot() {
		availableParkingSpaces++;
		
	}
	
	public ArrayList<String> getIDs() {
		
		return vehicleIDs;
	}
	
}