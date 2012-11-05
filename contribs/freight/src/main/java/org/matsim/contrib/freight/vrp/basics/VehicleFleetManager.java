package org.matsim.contrib.freight.vrp.basics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class VehicleFleetManager {
	
	public static VehicleFleetManager createDefaultFleetManager() {
		return new VehicleFleetManager(Collections.EMPTY_LIST);
	}
	
	static class TypeContainer {
		
		private String type;

		private LinkedList<Vehicle> vehicleList;
		
		public TypeContainer(String type) {
			super();
			this.type = type;
			vehicleList = new LinkedList<Vehicle>();
		}
		
		void add(Vehicle vehicle){
			if(vehicleList.contains(vehicle)){
				throw new IllegalStateException("cannot add vehicle twice " + vehicle.getId());
			}
			vehicleList.add(vehicle);
		}
		
		void remove(Vehicle vehicle){
			vehicleList.remove(vehicle);
		}

		public Vehicle getVehicle() {
			return vehicleList.getFirst();
		}

		public boolean isEmpty() {
			return vehicleList.isEmpty();
		}
		
	}
	
	private static Logger logger = Logger.getLogger(VehicleFleetManager.class);
	
	private Collection<Vehicle> vehicles;
	
	private Set<Vehicle> lockedVehicles;

	private Map<String,TypeContainer> typeMapOfAvailableVehicles;
	
	public VehicleFleetManager(Collection<Vehicle> vehicles) {
		super();
		this.vehicles = vehicles;
		this.lockedVehicles = new HashSet<Vehicle>();
		makeMap();
	}
	
	public VehicleFleetManager(Collection<Vehicle> vehicles, Collection<Vehicle> lockedVehicles) {
		this.vehicles = vehicles;
		makeMap();
		this.lockedVehicles = new HashSet<Vehicle>();
		for(Vehicle v : lockedVehicles){
			lock(v);
		}
	}

	private void makeMap() {
		typeMapOfAvailableVehicles = new HashMap<String, TypeContainer>();
		for(Vehicle v : vehicles){
			addVehicle(v);
		}
	}

	private void addVehicle(Vehicle v) {
		String typeId = v.getType().typeId;
		if(!typeMapOfAvailableVehicles.containsKey(typeId)){
			typeMapOfAvailableVehicles.put(typeId, new TypeContainer(typeId));
		}
		typeMapOfAvailableVehicles.get(typeId).add(v);
	}
	
	private void removeVehicle(Vehicle v){
		if(typeMapOfAvailableVehicles.containsKey(v.getType().typeId)){
			typeMapOfAvailableVehicles.get(v.getType().typeId).remove(v);
		}
	}

	public Vehicle getEmptyVehicle(String typeId){
		Vehicle v = null;
		if(typeMapOfAvailableVehicles.containsKey(typeId)){
			v = typeMapOfAvailableVehicles.get(typeId).getVehicle();
		}
		return v;
	}
	
	public Collection<String> getAvailableVehicleTypes(){
		List<String> types = new ArrayList<String>();
		for(String typeId : typeMapOfAvailableVehicles.keySet()){
			if(!typeMapOfAvailableVehicles.get(typeId).isEmpty()){
				types.add(typeId);
			}
		}
		return types;
	}
	
	public void lock(Vehicle vehicle){
		boolean locked = lockedVehicles.add(vehicle);
		removeVehicle(vehicle);
		if(!locked){
			throw new IllegalStateException("cannot lock vehicle twice " + vehicle.getId());
		}
	}
	
	public void unlock(Vehicle vehicle){
		if(vehicle == null) return;
		lockedVehicles.remove(vehicle);
		addVehicle(vehicle);
	}

	public Collection<String> getAvailableVehicleTypes(String withoutThisType) {
		List<String> types = new ArrayList<String>();
		for(String typeId : typeMapOfAvailableVehicles.keySet()){
			if(typeId.equals(withoutThisType)){
				continue;
			}
			if(!typeMapOfAvailableVehicles.get(typeId).isEmpty()){
				types.add(typeId);
			}
		}
		return types;
	}

	public boolean isLocked(Vehicle vehicle) {
		return lockedVehicles.contains(vehicle);
	}

}
