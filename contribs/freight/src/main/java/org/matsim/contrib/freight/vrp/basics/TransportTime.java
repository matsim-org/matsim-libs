package org.matsim.contrib.freight.vrp.basics;

public interface TransportTime {

	public double getTransportTime(String fromId, String toId,
			double departureTime, Driver driver, Vehicle vehicle);

}
