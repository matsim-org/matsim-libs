package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InfiniteVehicles implements VehicleFleetManager{

	private Set<Vehicle> vehicleTypes = new HashSet<Vehicle>();
	
	private Map<String,Vehicle> types = new HashMap<String, Vehicle>();
	
	public InfiniteVehicles(Collection<Vehicle> vehicles) {
		super();
		extractTypes(vehicles);
	}

	private void extractTypes(Collection<Vehicle> representiveVehicles) {
		for(Vehicle v : representiveVehicles){
			types.put(v.getType().typeId, v);
		}
	}

	@Override
	public Vehicle getEmptyVehicle(String typeId) {
		return types.get(typeId);
	}

	@Override
	public Collection<String> getAvailableVehicleTypes() {
		return types.keySet();
	}

	@Override
	public void lock(Vehicle vehicle) {
		
	}

	@Override
	public void unlock(Vehicle vehicle) {
		
	}

	@Override
	public Collection<String> getAvailableVehicleTypes(String withoutThisType) {
		Set<String> typeSet = new HashSet<String>(types.keySet());
		typeSet.remove(withoutThisType);
		return typeSet;
	}

	@Override
	public boolean isLocked(Vehicle vehicle) {
		return false;
	}

	@Override
	public void unlockAll() {
		
	}

}
