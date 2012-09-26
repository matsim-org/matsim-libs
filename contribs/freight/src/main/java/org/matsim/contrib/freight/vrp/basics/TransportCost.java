package org.matsim.contrib.freight.vrp.basics;

public interface TransportCost {

	public double getTransportCost(String fromId, String toId,
			double departureTime, Driver driver, Vehicle vehicle);

}
