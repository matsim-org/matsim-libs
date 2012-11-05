package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public interface RouteAgentFactory {
	
	/**
	 * creates an agent already having a certain tour
	 * @param vehicle
	 * @param driver
	 * @param tour
	 * @return
	 */
	public RouteAgent createAgent(Vehicle vehicle, Driver driver, TourImpl tour);

	/**
	 * creates an initial agent with an empty tour
	 * @param vehicle
	 * @param driver
	 * @return
	 */
	public RouteAgent createAgent(Vehicle vehicle, Driver driver);
	
	
	/**
	 * copies an agent
	 * @param agent
	 * @return
	 */
	public RouteAgent createAgent(RouteAgent agent);

}
