package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;

public interface VehicleFleetManager {

	public abstract Vehicle getEmptyVehicle(String typeId);

	public abstract Collection<String> getAvailableVehicleTypes();

	public abstract void lock(Vehicle vehicle);

	public abstract void unlock(Vehicle vehicle);

	public abstract Collection<String> getAvailableVehicleTypes(
			String withoutThisType);

	public abstract boolean isLocked(Vehicle vehicle);

	public abstract void unlockAll();

}