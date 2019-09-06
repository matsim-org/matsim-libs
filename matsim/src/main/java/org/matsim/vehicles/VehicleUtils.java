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
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;


/**
 * @author nagel
 *
 */
public final class VehicleUtils {
	private static final Logger log = Logger.getLogger( VehicleUtils.class ) ;

	private static final VehicleType DEFAULT_VEHICLE_TYPE = VehicleUtils.getFactory().createVehicleType(Id.create("defaultVehicleType", VehicleType.class));
	private static final String VEHICLE_ATTRIBUTE_KEY = "vehicles";

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
	private static final String FUEL_TYPE = "fuelType";

	static {
//		VehicleCapacity capacity = new VehicleCapacity();
//		capacity.setSeats(4);
//		DEFAULT_VEHICLE_TYPE.setCapacity(capacity);
		DEFAULT_VEHICLE_TYPE.getCapacity().setSeats( 4 );
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

	/**
	 * Creates a vehicle id based on the person and the mode
	 * <p>
	 * If config.qsim().getVehicleSource() is "modeVehicleTypesFromVehiclesData", the returned id is a combination of
	 * the person's id and the supplied mode. E.g. "person1_car
	 *
	 * @param person The person which owns the vehicle
	 * @param mode   The mode this vehicle is for
	 * @return a VehicleId
	 */
	public static Id<Vehicle> createVehicleId(Person person, String mode) {
		return Id.createVehicleId(person.getId().toString() + "_" + mode);
	}

	/**
	 * Retrieves a vehicleId from the person's attributes.
	 *
	 * @return the vehicleId of the person's vehicle for the specified mode
	 * @throws RuntimeException In case no vehicleIds were set or in case no vehicleId was set for the specified mode
	 */
	public static Id<Vehicle> getVehicleId(Person person, String mode) {
		Map<String, Id<Vehicle>> vehicleIds = (Map<String, Id<Vehicle>>) person.getAttributes().getAttribute(VehicleUtils.VEHICLE_ATTRIBUTE_KEY);
		if (vehicleIds == null || !vehicleIds.containsKey(mode)) {
			throw new RuntimeException("Could not retrieve vehicle id from person: " + person.getId().toString() + " for mode: " + mode +
                    ". \nIf you are not using config.qsim().getVehicleSource() with 'defaultVehicle' or 'modeVehicleTypesFromVehiclesData' you have to provide " +
                    "a vehicle for each mode for each person. Attach a map of mode:String -> id:Id<Vehicle> with key 'vehicles' as person attribute to each person." +
                    "\n VehicleUtils.insertVehicleIdIntoAttributes does this for you."
            );
		}
		return vehicleIds.get(mode);
	}

    /**
     * Attaches a vehicle id to a person, so that the router knows which vehicle to use for which mode and person
     */
    public static void insertVehicleIdIntoAttributes(Person person, String mode, Id<Vehicle> vehicleId) {
        Object attr = person.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_KEY);
        Map<String, Id<Vehicle>> map = attr == null ? new HashMap<>() : ((Map<String, Id<Vehicle>>) attr);
        map.put(mode, vehicleId);
        person.getAttributes().putAttribute(VEHICLE_ATTRIBUTE_KEY, map);
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

	public static Double getFuelConsumption(VehicleType vehicleType) {
//		final Object attribute = vehicleType.getAttributes().getAttribute(FUELCONSUMPTION);
//		if ( attribute==null ) {
//			return Double.NaN ; // this was the default value in V1; could also return Double-null instead.
//		} else {
//			return (double) attribute;
//		}
		return getFuelConsumption(vehicleType.getEngineInformation());
	}

	public static void setFuelConsumption(VehicleType vehicleType, double literPerMeter) {
    	setFuelConsumption(vehicleType.getEngineInformation(), literPerMeter);
//		vehicleType.getAttributes().putAttribute(FUELCONSUMPTION, literPerMeter);
	}

//	//TODO: Remove here, because we now let in engineInformation as seperate field?
//	public static EngineInformation getEngineInformation( VehicleType vehicleType ){
//		EngineInformation engineInformation = vehicleType.getEngineInformation();
//		//if not stored in the "old" format, organize values from the attributes. This will be probably changed in the future, kmt mar'19
//		if (Double.isNaN(engineInformation.getFuelConsumption())){
//			engineInformation.setFuelConsumption(getFuelConsumption(vehicleType));
//		}
//		return engineInformation;
//	}
//
//	//TODO: Remove here, because we now let in engineInformation as seperate field?
//	public static void setEngineInformation( VehicleType vehicleType, EngineInformation.FuelType fuelType, double literPerMeter ){
//		vehicleType.setEngineInformation(new EngineInformation(fuelType) );
//		setHbefaTechnology(vehicleType.getEngineInformation(), fuelType.toString());
//		setFuelConsumption(vehicleType, literPerMeter);
//	}
//
//	//TODO: Remove here, because we now let in engineInformation as seperate field?
//	public static void setEngineInformation(VehicleType vehicleType, Attributes currAttributes) {
//		vehicleType.setEngineInformation(new EngineInformation() );
//		if (currAttributes == null || currAttributes.isEmpty()){
////			log.warn("No Attributes were set for EngineInformation of vehicle type " + vehicleType);
//			throw new RuntimeException("No Attributes were set for EngineInformation of vehicle type " + vehicleType);
//		} else {
//			for (String attribute : currAttributes.getAsMap().keySet()) {
//				vehicleType.getAttributes().putAttribute(attribute, currAttributes.getAttribute(attribute));
//			}
//		}
//	}

	//******** Capacity attributes ************


	@Deprecated
	public static double getFreightCapacityUnits (VehicleCapacity vehicleCapacity) {
		return (int) vehicleCapacity.getAttributes().getAttribute(FREIGHT_CAPACITY_UNITS);
	}


	@Deprecated
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

	public static String getHbefaSizeClass( EngineInformation ei ) {
		return (String) ei.getAttributes().getAttribute(HBEFA_SIZE_CLASS);
	}
	public static void setHbefaSizeClass( EngineInformation engineInformation, String hbefaSizeClass ) {
		engineInformation.getAttributes().putAttribute( HBEFA_SIZE_CLASS, hbefaSizeClass ) ;
	}

//	public static String getHbefaEmissionsConcept(VehicleType vehicleType) {
//		return getHbefaEmissionsConcept(vehicleType.getEngineInformation());
//	}

	public static String getHbefaEmissionsConcept( EngineInformation ei ) {
		return (String) ei.getAttributes().getAttribute(HBEFA_EMISSIONS_CONCEPT);
	}
	public static void setHbefaEmissionsConcept( EngineInformation engineInformation, String emissionsConcept ) {
		engineInformation.getAttributes().putAttribute( HBEFA_EMISSIONS_CONCEPT, emissionsConcept ) ;
	}

	//******** CostInformation attributes ************

	@Deprecated
	/*package*/ static double getCostsPerSecondWaiting(CostInformation costInformation) {
		return (double) costInformation.getAttributes().getAttribute(COST_PER_SECOND_WAITING);
	}


	@Deprecated
	/*package*/ static void setCostsPerSecondWaiting(CostInformation costInformation, double costsPerSecond) {
		costInformation.getAttributes().putAttribute(COST_PER_SECOND_WAITING, costsPerSecond);
	}


	@Deprecated
	/*package*/ static double getCostsPerSecondInService(CostInformation costInformation) {
		return (double) costInformation.getAttributes().getAttribute(COST_PER_SECOND_INSERVICE);
	}


	@Deprecated
	/*package*/ static void setCostsPerSecondInService(CostInformation costInformation, double costsPerSecond) {
		costInformation.getAttributes().putAttribute(COST_PER_SECOND_INSERVICE, costsPerSecond);
	}

	public static VehicleImpl createVehicle( Id<Vehicle> id , VehicleType type ){
		return new VehicleImpl( id , type );
	}

	@Deprecated
	static EngineInformation.FuelType getFuelType(EngineInformation engineInformation ){
		return (EngineInformation.FuelType) engineInformation.getAttributes().getAttribute( FUEL_TYPE );
	}

	@Deprecated
	static void setFuelType(EngineInformation engineInformation, EngineInformation.FuelType fuelType ){
		engineInformation.getAttributes().putAttribute( FUEL_TYPE,  fuelType);
	}

	@Deprecated
	static Double getFuelConsumption(EngineInformation engineInformation ){
		return (Double) engineInformation.getAttributes().getAttribute( FUELCONSUMPTION );
	}

	@Deprecated
	static void setFuelConsumption(EngineInformation engineInformation, double fuelConsumption ){
		engineInformation.getAttributes().putAttribute( FUELCONSUMPTION,  fuelConsumption);
	}

	@Deprecated
	public enum DoorOperationMode{ serial, parallel }
}
