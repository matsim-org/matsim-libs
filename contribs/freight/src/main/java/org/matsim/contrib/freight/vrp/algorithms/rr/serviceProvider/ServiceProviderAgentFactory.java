package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public interface ServiceProviderAgentFactory {
	
	/**
	 * creates an agent already having a certain tour
	 * @param vehicle
	 * @param driver
	 * @param tour
	 * @return
	 */
	public ServiceProviderAgent createAgent(Vehicle vehicle, Driver driver, Tour tour);

	/**
	 * creates an initial agent with an empty tour
	 * @param vehicle
	 * @param driver
	 * @return
	 */
	public ServiceProviderAgent createAgent(Vehicle vehicle, Driver driver);
	
	
//	public ServiceProviderAgent createAgent(Driver driver);
	
	/**
	 * copies an agent
	 * @param agent
	 * @return
	 */
	public ServiceProviderAgent createAgent(ServiceProviderAgent agent);

}
