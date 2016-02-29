package org.matsim.contrib.carsharing.stations;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;

public class FreeFloatingStation extends AbstractCarSharingStation {

	
	private int numberOfVehicles;
	private ArrayList<String> vehicleIDs = new ArrayList<String>();
	
	public FreeFloatingStation(Link link, int numberOfVehicles, ArrayList<String> vehicleIDs) {
		super(link) ;
		// is a data class; should not keep object references! kai, feb'15
		
		this.numberOfVehicles = numberOfVehicles;
		this.vehicleIDs = vehicleIDs;
	}
	
	public int getNumberOfVehicles() {
		
		return numberOfVehicles;
	}
	
	public ArrayList<String> getIDs() {
		
		return vehicleIDs;
	}
	
}
