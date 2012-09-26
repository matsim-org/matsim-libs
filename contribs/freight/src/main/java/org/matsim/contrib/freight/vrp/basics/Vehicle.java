package org.matsim.contrib.freight.vrp.basics;

import org.matsim.contrib.freight.vrp.basics.VehicleImpl.Type;

public interface Vehicle {

	public abstract double getEarliestDeparture();

	public abstract double getLatestArrival();

	public abstract String getLocationId();

	public abstract Type getType();

	public abstract String getId();

	public abstract int getCapacity();

}