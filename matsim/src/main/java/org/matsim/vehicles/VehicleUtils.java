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
import org.matsim.utils.objectattributes.attributable.Attributes;


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
	private static final String FUELCONSUMPTION = "fuelConsumptionLitersPerMeter";
	private static final String FREIGHT_CAPACITY_UNITS = "freightCapacityInUnits";
	private static final String HBEFA_VEHICLE_CATEGORY_= "HbefaVehicleCategory";
	private static final String HBEFA_TECHNOLOGY = "HbefaTechnology";
	private static final String HBEFA_SIZE_CLASS = "HbefaSizeClass";
	private static final String HBEFA_EMISSIONS_CONCEPT = "HbefaEmissionsConcept";
	private static final String COST_PER_SECOND_WAITING = "costsPerSecondWaiting";
	private static final String COST_PER_SECOND_INSERVICE = "costsPerSecondInService";

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

	//******** general VehicleType attributes ************

	public static DoorOperationMode getDoorOperationMode( VehicleType vehicleType ){
		final Object attribute = vehicleType.getAttributes().getAttribute( DOOR_OPERATION_MODE );
		if ( attribute==null ) {
			return DoorOperationMode.serial; // this was the default value in V1; could also return null instead.
		} else if (attribute instanceof DoorOperationMode ){
			return (DoorOperationMode) attribute;
		} else if (attribute instanceof String) {
			String modeString = (String) attribute;
			if ( DoorOperationMode.serial.toString().equalsIgnoreCase(modeString )) {
				return DoorOperationMode.serial;
			} else if ( DoorOperationMode.parallel.toString().equalsIgnoreCase(modeString )) {
				return DoorOperationMode.parallel;
			} else {
				throw new IllegalArgumentException("VehicleType " + vehicleType.getId().toString() + " : Door operation mode " + modeString + "is not supported");
			}
		}
		else {
			throw new RuntimeException("Type of " + attribute + "is not supported here");
		}
	}

	public static void setDoorOperationMode( VehicleType vehicleType, DoorOperationMode mode ){
		vehicleType.getAttributes().putAttribute( DOOR_OPERATION_MODE, mode ) ;
	}

	public static double getEgressTime(VehicleType vehicleType) {
		final Object attribute = vehicleType.getAttributes().getAttribute( EGRESSTIME );
		if ( attribute==null ) {
			return 1.0 ; // this was the default value in V1; could also return Double-null instead.
		} else {
			return (double) attribute;
		}
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

	public static double getFuelConsumption(VehicleType vehicleType) {
		final Object attribute = vehicleType.getAttributes().getAttribute(FUELCONSUMPTION);
		if ( attribute==null ) {
			return Double.NaN ; // this was the default value in V1; could also return Double-null instead.
		} else {
			return (double) attribute;
		}
	}

	public static void setFuelConsumption(VehicleType vehicleType, double literPerMeter) {
		vehicleType.getAttributes().putAttribute(FUELCONSUMPTION, literPerMeter);
	}

	//TODO: Remove here, because we now let in engineInformation as seperate field?
	public static EngineInformation getEngineInformation(VehicleType vehicleType){
		EngineInformation engineInformation = vehicleType.getEngineInformation();
		//if not stored in the "old" format, organize values from the attributes. This will be probably changed in the future, kmt mar'19
		if (Double.isNaN(engineInformation.getFuelConsumption())){
			engineInformation.setFuelConsumption(getFuelConsumption(vehicleType));
		}
		return engineInformation;
	}

	//TODO: Remove here, because we now let in engineInformation as seperate field?
	public static void setEngineInformation(VehicleType vehicleType, EngineInformation.FuelType fuelType, double literPerMeter){
		vehicleType.setEngineInformation(new EngineInformationImpl(fuelType));
		setHbefaTechnology(vehicleType.getEngineInformation(), fuelType.toString());
		setFuelConsumption(vehicleType, literPerMeter);
	}

	//TODO: Remove here, because we now let in engineInformation as seperate field?
	public static void setEngineInformation(VehicleType vehicleType, Attributes currAttributes) {
		vehicleType.setEngineInformation(new EngineInformationImpl());
		if (currAttributes == null || currAttributes.isEmpty()){
//			log.warn("No Attributes were set for EngineInformation of vehicle type " + vehicleType);
			throw new RuntimeException("No Attributes were set for EngineInformation of vehicle type " + vehicleType);
		} else {
			for (String attribute : currAttributes.getAsMap().keySet()) {
				vehicleType.getAttributes().putAttribute(attribute, currAttributes.getAttribute(attribute));
			}
		}
	}

	//******** EngineInformation attributes ************

	public static double getFreightCapacityUnits (VehicleCapacity vehicleCapacity) {
		return (int) vehicleCapacity.getAttributes().getAttribute(FREIGHT_CAPACITY_UNITS);
	}

	public static void setFreightCapacityUnits(VehicleCapacity vehicleCapacity, double units) {
		vehicleCapacity.getAttributes().putAttribute(FREIGHT_CAPACITY_UNITS, units);
	}

//	public static String getHbefaTechnology(VehicleType vehicleType){
////		final Object attribute = vehicleType.getAttributes().getAttribute(HBEFA_TECHNOLOGY);
////		return (String) attribute;
//		return getHbefaTechnology( vehicleType.getEngineInformation() ) ;
//	}

	//******** EngineInformation attributes ************

	public static String getHbefaTechnology( EngineInformation ei ){
		return (String) ei.getAttributes().getAttribute( HBEFA_TECHNOLOGY ) ;
	}
	public static void setHbefaTechnology( EngineInformation engineInformation, String hbefaTechnology ) {
		engineInformation.getAttributes().putAttribute( HBEFA_TECHNOLOGY, hbefaTechnology ) ;
	}

//	public static void setHbefaTechnology(VehicleType vehicleType, String hbefaTechnology){
//		vehicleType.getAttributes().putAttribute(HBEFA_TECHNOLOGY, hbefaTechnology);
//	}

//	public static String getHbefaVehicleCategory(VehicleType vehicleType) {
//		return getHbefaVehicleCategory(vehicleType.getEngineInformation()) ;
//	}

	public static String getHbefaVehicleCategory( EngineInformation ei ){
		return (String) ei.getAttributes().getAttribute( HBEFA_VEHICLE_CATEGORY_ ) ;
	}
	public static void setHbefaVehicleCategory( EngineInformation engineInformation, String hbefaVehicleCategory ) {
		engineInformation.getAttributes().putAttribute( HBEFA_VEHICLE_CATEGORY_, hbefaVehicleCategory ) ;
	}

//	public static String getHbefaSizeClass(VehicleType vehicleType) {
//		return getHbefaSizeClass(vehicleType.getEngineInformation());
//	}

	public static String getHbefaSizeClass(EngineInformation ei) {
		return (String) ei.getAttributes().getAttribute(HBEFA_SIZE_CLASS);
	}
	public static void setHbefaSizeClass( EngineInformation engineInformation, String hbefaSizeClass ) {
		engineInformation.getAttributes().putAttribute( HBEFA_SIZE_CLASS, hbefaSizeClass ) ;
	}

//	public static String getHbefaEmissionsConcept(VehicleType vehicleType) {
//		return getHbefaEmissionsConcept(vehicleType.getEngineInformation());
//	}

	public static String getHbefaEmissionsConcept(EngineInformation ei) {
		return (String) ei.getAttributes().getAttribute(HBEFA_EMISSIONS_CONCEPT);
	}
	public static void setHbefaEmissionsConcept( EngineInformation engineInformation, String emissionsConcept ) {
		engineInformation.getAttributes().putAttribute( HBEFA_EMISSIONS_CONCEPT, emissionsConcept ) ;
	}

	//******** CostInformation attributes ************
	public static double getCostsPerSecondWaiting(CostInformation costInformation) {
		return (double) costInformation.getAttributes().getAttribute(COST_PER_SECOND_WAITING);
	}

	public static void setCostsPerSecondWaiting(CostInformation costInformation, double costsPerSecond) {
		costInformation.getAttributes().putAttribute(COST_PER_SECOND_WAITING, costsPerSecond);
	}

	public static double getCostsPerSecondInService(CostInformation costInformation) {
		return (double) costInformation.getAttributes().getAttribute(COST_PER_SECOND_INSERVICE);
	}

	public static void setCostsPerSecondInService(CostInformation costInformation, double costsPerSecond) {
		costInformation.getAttributes().putAttribute(COST_PER_SECOND_INSERVICE, costsPerSecond);
	}

}
