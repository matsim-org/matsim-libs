/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import java.util.*;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

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

	public enum FleetSize {
		INFINITE, FINITE
	}

	public static class Builder {

		public static Builder newInstance(){ return new Builder(); }

		private final Collection<VehicleType> vehicleTypes = new ArrayList<>();

		private final Map<Id<Vehicle>, CarrierVehicle> vehicles = new LinkedHashMap<>();

		private final Set<Id<org.matsim.vehicles.VehicleType>> typeIds = new HashSet<>();

		private FleetSize fleetSize = FleetSize.FINITE;

		public Builder setFleetSize(FleetSize fleetSize){
			this.fleetSize = fleetSize;
			return this;
		}

		/**
		 * @deprecated Since the vehicle type is in the {@link CarrierVehicleTypes}
		 * container, it should not be duplicated here. It is also not written
		 * to file when writing.
		 */
		@Deprecated
		public Builder addType( VehicleType type ){
			if(!typeIds.contains(type.getId())){
				vehicleTypes.add(type);
				typeIds.add(type.getId());
			}
			return this;
		}

		public Builder addVehicle(CarrierVehicle carrierVehicle){
			vehicles.put(carrierVehicle.getId(), carrierVehicle);
			if(carrierVehicle.getType() != null) addType(carrierVehicle.getType() );
			return this;
		}

		public CarrierCapabilities build(){
			return new CarrierCapabilities(this);
		}


	}

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

	private CarrierCapabilities(){}

	private CarrierCapabilities(Builder builder){
		this.carrierVehicles = builder.vehicles;
		this.vehicleTypes = builder.vehicleTypes;
		this.fleetSize = builder.fleetSize;
	}

	private Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = new LinkedHashMap<>();

	private Collection<VehicleType> vehicleTypes = new ArrayList<>();


	/**
	 * Sets the fleetSize.
	 *
	 * <p>FleetSize can be FleetSize.INFINITE and FleetSize.FINITE. If fleetSize is FleetSize.INFINITE then the vehicles in carrierVehicles are representative vehicles.
	 * Each representative vehicle can be employed infinite times.
	 * <p>If fleetSize is FleetSize.FINITE then the vehicles in carrierVehicles are exactly the vehicles
	 * the carrier can employ.
	 *
	 * <p>By default, it is FleetSize.FINITE
	 *
	 * @see FleetSize
	 */
	private FleetSize fleetSize = FleetSize.FINITE;

	/**
	 * Returns a collection of carrierVehicles, a carrier has to its disposal.
	 * <p>
	 *
	 * @return collection of carrierVehicles
	 * @see CarrierVehicle
	 */
	public Map<Id<Vehicle>, CarrierVehicle> getCarrierVehicles() {
		return carrierVehicles;
	}



	public FleetSize getFleetSize() {
		return fleetSize;
	}



	public void setFleetSize(FleetSize fleetSize) {
		this.fleetSize = fleetSize;
	}


	/**
	 * Returns a collection of CarrierVehicleTypes.
	 *
	 * @return a collection of vehicleTypes
	 * @see VehicleType
	 */
	public Collection<VehicleType> getVehicleTypes() {
		return vehicleTypes;
	}

}
