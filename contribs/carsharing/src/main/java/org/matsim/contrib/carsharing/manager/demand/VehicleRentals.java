package org.matsim.contrib.carsharing.manager.demand;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public class VehicleRentals {
	private Id<Vehicle> vehicleId;

	private ArrayList<RentalInfo> rentals;

	public VehicleRentals(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
		this.rentals = new ArrayList<RentalInfo>();
	}

	public void setVehicleId(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}

	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public void setRentals(ArrayList<RentalInfo> rentals) {
		this.rentals = rentals;
	}

	public ArrayList<RentalInfo> getRentals() {
		return this.rentals;
	}
}
