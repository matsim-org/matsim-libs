package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;

public class VehicleRoutingProblemSolutionImpl implements VehicleRoutingProblemSolution{

	private Collection<VehicleRoute> routes;
	
	private double totalCost;
	
	public VehicleRoutingProblemSolutionImpl(Collection<VehicleRoute> routes, double totalCost) {
		super();
		this.routes = routes;
		this.totalCost = totalCost;
	}

	@Override
	public Collection<VehicleRoute> getRoutes() {
		return routes;
	}

	@Override
	public double getTotalCost() {
		return totalCost;
	}

}
