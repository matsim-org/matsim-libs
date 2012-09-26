package org.matsim.contrib.freight.vrp.basics;

public interface BackwardTransportCost {

	public double getBackwardTransportCost(String fromId, String toId,
			double arrivalTime, Driver driver, Vehicle vehicle);

}
