package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;

public class TourPlan {
	
	private Collection<VehicleRoute> vehicleRoutes;
	
	private double score;

	public TourPlan(Collection<VehicleRoute> vehicleRoutes) {
		super();
		this.vehicleRoutes = vehicleRoutes;
	}

	public Collection<VehicleRoute> getVehicleRoutes() {
		return vehicleRoutes;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
	
	

}
