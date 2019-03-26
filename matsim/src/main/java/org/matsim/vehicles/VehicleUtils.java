/* *********************************************************************** *
 * project: matsim
 * VehicleUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.vehicles;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;


/**
 * @author nagel
 *
 */
public class VehicleUtils {
	private static final Logger log = Logger.getLogger( VehicleUtils.class ) ;

	private static final VehicleType DEFAULT_VEHICLE_TYPE = VehicleUtils.getFactory().createVehicleType(Id.create("defaultVehicleType", VehicleType.class));

	// should remain under the hood --> should remain private
	private static final String DOOR_OPERATION_MODE = "doorOperationMode" ;
	private static final String EGRESSTIME = "egressTimeInSecondsPerPerson";
	private static final String ACCESSTIME = "accessTimeInSecondsPerPerson";
	private static final String GASCONSUMPTION = "gasConsumptionLitersPerMeter";
	private static final String FREIGHT_CAPACITY_UNITS = "freightCapacityInUnits";

	static {
		VehicleCapacityImpl capacity = new VehicleCapacityImpl();
		capacity.setSeats(4);
		DEFAULT_VEHICLE_TYPE.setCapacity(capacity);
	}

	public static VehiclesFactory getFactory() {
		return new VehiclesFactoryImpl();
	}

	public static Vehicles createVehiclesContainer() {
		return new VehiclesImpl();
	}

	public static VehicleType getDefaultVehicleType() {
		return DEFAULT_VEHICLE_TYPE;
	}

	public static VehicleType.DoorOperationMode getDoorOperationMode( VehicleType vehicleType ){
		return (VehicleType.DoorOperationMode) vehicleType.getAttributes().getAttribute( DOOR_OPERATION_MODE );
	}

	public static void setDoorOperationMode( VehicleType vehicleType, VehicleType.DoorOperationMode mode ){
		vehicleType.getAttributes().putAttribute( DOOR_OPERATION_MODE, mode ) ;
	}

	public static double getEgressTime(VehicleType vehicleType) {
		return (Double) vehicleType.getAttributes().getAttribute(EGRESSTIME);
	}

	public static void setEgressTime(VehicleType vehicleType, double egressTime) {
		vehicleType.getAttributes().putAttribute(EGRESSTIME, egressTime);
	}

	public static double getAccessTime(VehicleType vehicleType) {
		final Object attribute = vehicleType.getAttributes().getAttribute( ACCESSTIME );
		if ( attribute==null ) {
			return 1.0 ; // this was the default value in V1; could also return Double-null instead.
		} else {
			return (double) attribute ;
		}
	}

	public static void setAccessTime(VehicleType vehicleType, double accessTime) {
		vehicleType.getAttributes().putAttribute(ACCESSTIME, accessTime);
	}

	public static double getGasConsumption(VehicleType vehicleType) {
		return (Double) vehicleType.getAttributes().getAttribute(GASCONSUMPTION);
	}

	public static void setGasConsumption(VehicleType vehicleType, double literPerMeter) {
		vehicleType.getAttributes().putAttribute(GASCONSUMPTION, literPerMeter);
	}

	public static double getFreightCapacityUnits (VehicleType vehicleType) {
		return (Double) vehicleType.getAttributes().getAttribute(FREIGHT_CAPACITY_UNITS);
	}

	public static void setFreightCapacityUnits(VehicleType vehicleType, double units) {
		vehicleType.getAttributes().putAttribute(FREIGHT_CAPACITY_UNITS, units);
	}


	//TODO: acessTime: einbinden

	//TODO: egressTime: einbinden

	//TODO: gasConsumption -> literPerMeter: , einbinden

	//TODO: FreightCapacity -> units: einbinden
}
