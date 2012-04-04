package org.matsim.contrib.freight.vrp.basics;

public class VehicleRoute {
	
	private Tour tour;
	
	private Vehicle vehicle;

	public VehicleRoute(Tour tour, Vehicle vehicle) {
		super();
		this.tour = tour;
		this.vehicle = vehicle;
	}

	public Tour getTour() {
		return tour;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}
	
	

}
