package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * This contains the capabilities/resources a carrier has/can deploy.
 * 
 * <p>If a carrier has a fixed fleet-size, this should contain all carrierVehicles that a carrier can deploy (@see CarrierVehicle).
 * If the fleet configuration is part of the planning problem and the carrier can dimension its fleet, this should contain
 * the available carrierVehicleTypes (@see CarrierVehicleType) and the available depots. If certain types are only 
 * available at certain depots, assign them to depotIds accordingly, otherwise it is assumed that every type can be 
 * deployed at every depot.
 * 
 * @author sschroeder, mzilske
 *
 */
public class CarrierCapabilities {

	/**
	 * Returns a new instance of CarrierCapabilities.
	 * 
	 * <p>This method always returns an empty CarrierCapabilities object, i.e. with no capabilities.
	 * 
	 * @return an empty capability object
	 */
	public static CarrierCapabilities newInstance(){
		return new CarrierCapabilities();
	}
	
	private Collection<CarrierVehicle> carrierVehicles = new ArrayList<CarrierVehicle>();

	private Collection<Id> depots = new ArrayList<Id>();
	
	private Collection<CarrierVehicleType> vehicleTypes = new ArrayList<CarrierVehicleType>();
	
	private Map<Id, Collection<CarrierVehicleType>> depotToTypes = new HashMap<Id, Collection<CarrierVehicleType>>();
	
	/**
	 * Returns a collection of carrierVehicles, a carrier has to its disposal.
	 * 
	 * 
	 * @return collection of carrierVehicles
	 * @see CarrierVehicle
	 */
	public Collection<CarrierVehicle> getCarrierVehicles() {
		return carrierVehicles;
	}
	
	
	
	public Map<Id, Collection<CarrierVehicleType>> getDepotToTypes() {
		return depotToTypes;
	}


	/**
	 * Returns a collection of depotIds.
	 * 
	 * <p>This collection is intended to contain the linkIds of available depots.
	 * 
	 * @return collection of ids
	 */
	public Collection<Id> getDepots() {
		return depots;
	}

	/**
	 * Returns a collection of CarrierVehicleTypes.
	 * 
	 * @return a collection of vehicleTypes
	 * @see CarrierVehicleType
	 */
	public Collection<CarrierVehicleType> getVehicleTypes() {
		return vehicleTypes;
	}

	/**
	 * Assigns a vehicleType to a certain depot.
	 * 
	 * <p>If the depotList does not contain the depotId, it is added. If the vehicleType is not in the vehicleTypeList it is added as well.
	 * 
	 * @param depotId the vehicleType is assigned to
	 * @param vehicleType to be assigned
	 *  
	 */
	public void assignTypeToDepot(Id depotId, CarrierVehicleType vehicleType){
		if(!depots.contains(depotId)) depots.add(depotId);
		if(!vehicleTypes.contains(vehicleType)) vehicleTypes.add(vehicleType);
		if(!depotToTypes.containsKey(depotId)){
			List<CarrierVehicleType> types = new ArrayList<CarrierVehicleType>();
			types.add(vehicleType);
			depotToTypes.put(depotId, types);
		}
		else depotToTypes.get(depotId).add(vehicleType);
	}
}
