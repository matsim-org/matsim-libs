package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;

public class VehicleRoutingProblemSolution {
	
	private Collection<VehicleRoute> routes;

	private double totalCost;

	public VehicleRoutingProblemSolution(Collection<VehicleRoute> routes, double totalCost) {
		super();
		this.routes = routes;
		this.totalCost = totalCost;
	}

	
	public Collection<VehicleRoute> getRoutes() {
		return routes;
	}


	public double getTotalCost() {
		return totalCost;
	}


	public void setCosts(double totalCost) {
		this.totalCost = totalCost;
	}

}
