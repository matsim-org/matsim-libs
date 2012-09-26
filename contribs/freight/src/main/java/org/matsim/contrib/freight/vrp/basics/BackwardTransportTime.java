package org.matsim.contrib.freight.vrp.basics;

public interface BackwardTransportTime {

	public double getBackwardTransportTime(String fromId, String toId,
			double arrivalTime, Driver driver, Vehicle vehicle);

}
