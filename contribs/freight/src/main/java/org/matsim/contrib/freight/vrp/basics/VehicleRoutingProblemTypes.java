package org.matsim.contrib.freight.vrp.basics;

public class VehicleRoutingProblemTypes {
	
	/**
	 * Single depot distribution problem with capacity constraints and time windows
	 * 
	 */
	public static String CVRPTW = "SingleDepotDistributionTimeWindows";
	
	/**
	 * Single depot distribution problem with capacity constraints
	 * 
	 */
	public static String CVRP = "SingleDepotDistribution";
	
	/**
	 * Single depot distribution problem with time-dependent transport costs,
	 * capacity constraints and time windows
	 */
	public static String TDCVRPTW = "TDCVRPTW";
	

}
