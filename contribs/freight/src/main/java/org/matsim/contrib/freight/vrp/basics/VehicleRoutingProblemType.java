package org.matsim.contrib.freight.vrp.basics;

public enum VehicleRoutingProblemType {
	
	/**
	 * Single depot distribution problem with capacity constraints
	 * 
	 */
	CVRP,
	
	/**
	 * Single depot distribution problem with capacity constraints and time windows
	 * 
	 */
	CVRPTW,
	
	/**
	 * Single depot distribution problem with time-dependent transport costs,
	 * capacity constraints and time windows
	 */
	TDCVRPTW
	
}
