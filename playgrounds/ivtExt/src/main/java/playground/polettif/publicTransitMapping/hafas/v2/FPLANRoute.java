/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.hafas.v2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;

/**
 * A public transport route as read out from HAFAS FPLAN.
 *
 * @author boescpa
 */
public class FPLANRoute {

	private static Logger log = Logger.getLogger(FPLANRoute.class);

	private static TransitSchedule schedule;
	private static TransitScheduleFactory scheduleFactory;
	private static int depId = 1;

	private final int initialDelay = 60; // [s] In MATSim a pt route starts with the arrival at the first station. In HAFAS with the departure at the first station. Ergo we have to set a delay which gives some waiting time at the first station while still keeping the schedule.

	public final static String PT = "pt";

	private final String operator;
	private final String fahrtNummer;
	private String routeDescription;

	private final int numberOfDepartures;
	private final int cycleTime; // [sec]

	private List<Object[]> tmpStops = new ArrayList<>();
	private final List<TransitRouteStop> transitRouteStops = new ArrayList<>();

	private Id<VehicleType> vehicleTypeId;

	public boolean addToSchedule = true;

	public static void setSchedule(TransitSchedule schedule) {
		FPLANRoute.schedule = schedule;
		FPLANRoute.scheduleFactory = schedule.getFactory();
	}

	public FPLANRoute(String operator, String fahrtNummer, int numberOfDepartures, int cycleTime) {
		this.operator = operator;
		this.fahrtNummer = fahrtNummer;
		this.numberOfDepartures = numberOfDepartures + 1; // Number gives all occurrences of route additionally to first... => +1
		this.cycleTime = cycleTime * 60; // Cycle time is given in minutes in HAFAS -> Have to change it here...
		this.routeDescription = null;
	}

	public void setRouteDescription(String nr) {
		this.routeDescription = nr;
	}


	// First departure time:
	private int firstDepartureTime = -1; //[sec]
	public void setFirstDepartureTime(int hour, int minute) {
		if(firstDepartureTime < 0) {
			this.firstDepartureTime = (hour * 3600) + (minute * 60);
		}
	}

	// Used vehicle type, Id and mode:
	private String mode = PT;

	public void setVehicleTypeId(Id<VehicleType> typeId) {
		vehicleTypeId = typeId;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getVehicleId() {
		return vehicleTypeId + "_" + operator;
	}

	/**
	 * @param arrivalTime   Expected as seconds from midnight or zero if not available.
	 * @param departureTime Expected as seconds from midnight or zero if not available.
	 */
	public void addRouteStop(String stopFacilityId, double arrivalTime, double departureTime) {
		Object[] tmpRouteStop = new Object[3];
		tmpRouteStop[0] = stopFacilityId;
		tmpRouteStop[1] = arrivalTime;
		tmpRouteStop[2] = departureTime;

		tmpStops.add(tmpRouteStop);
	}

	/**
	 * @return A list of all departures of this route.
	 * If firstDepartureTime or usedVehicle are not set before this is called, null is returned.
	 * If vehicleType is not set, the vehicle is not in the list and entry will not be created.
	 */
	public List<Departure> getDepartures() {
		if(firstDepartureTime < 0 || getVehicleId() == null) {
			log.error("getDepartures before first departureTime and usedVehicleId set.");
			return null;
		}
		if(vehicleTypeId == null) {
			//log.warn("VehicleType not defined in vehicles list.");
			return null;
		}

		List<Departure> departures = new ArrayList<>();
		for(int i = 0; i < numberOfDepartures; i++) {
			// Departure ID
//			Id<Departure> departureId = Id.create(routeDescription + "_" + String.format("%04d", i + 1), Departure.class);
			Id<Departure> departureId = Id.create(String.format("%05d", depId++), Departure.class);
			// Departure time
			double departureTime = firstDepartureTime + (i * cycleTime) - initialDelay;
			// Departure vehicle
//			Id<Vehicle> vehicleId = Id.create(getVehicleId() + "_" + String.format("%04d", i + 1), Vehicle.class);
			Id<Vehicle> vehicleId = Id.create(getVehicleId() + "_" + departureId, Vehicle.class);
			// create and add departure
			departures.add(createDeparture(departureId, departureTime, vehicleId));
		}
		return departures;
	}

	/**
	 * @return the id of the first stop
	 */
	public String getFirstStopId() {
		return (String) tmpStops.get(0)[0];
	}

	/**
	 * @return the id of the last stop
	 */
	public String getLastStopId() {
		return (String) tmpStops.get(tmpStops.size()-1)[0];
	}

	/**
	 * @return the transit route stops of this route. Static schedule needs to be set.
	 */
	public List<TransitRouteStop> getTransitRouteStops() {
		if(schedule == null) {
			throw new RuntimeException("Schedule and stopFacilities not yet defined for FPLANRoute!");
		}

		for(Object[] t : tmpStops) {
			Id<TransitStopFacility> stopFacilityId = Id.create((String) t[0], TransitStopFacility.class);
			double arrivalTime = (double) t[1];
			double departureTime = (double) t[2];

			double arrivalDelay = 0.0;
			if(arrivalTime > 0 && firstDepartureTime > 0) {
				arrivalDelay = arrivalTime + initialDelay - firstDepartureTime;
			}
			double departureDelay = 0.0;
			if(departureTime > 0 && firstDepartureTime > 0) {
				departureDelay = departureTime + initialDelay - firstDepartureTime;
			} else if(arrivalDelay > 0) {
				departureDelay = arrivalDelay + initialDelay;
			}

			TransitStopFacility stopFacility = schedule.getFacilities().get(stopFacilityId);
			TransitRouteStop routeStop = scheduleFactory.createTransitRouteStop(stopFacility, arrivalDelay, departureDelay);
			routeStop.setAwaitDepartureTime(true); // Only *T-Lines (currently not implemented) would have this as false...
			transitRouteStops.add(routeStop);
		}

		return transitRouteStops;
	}

	private Departure createDeparture(Id<Departure> departureId, double departureTime, Id<Vehicle> vehicleId) {
		Departure departure = scheduleFactory.createDeparture(departureId, departureTime);
		departure.setVehicleId(vehicleId);
		return departure;
	}

	private static boolean isTypeOf(Class<? extends Enum> vehicleGroup, String vehicle) {
		for(Object val : vehicleGroup.getEnumConstants()) {
			if(((Enum) val).name().equals(vehicle)) {
				return true;
			}
		}
		return false;
	}

	public String getRouteDescription() {
		return routeDescription;
	}

	public String getFahrtNummer() {
		return fahrtNummer;
	}

	public String getMode() {
		return mode;
	}

	public String getOperator() {
		return operator;
	}

	public Id<VehicleType> getVehicleTypeId() {
		return vehicleTypeId;
	}
}
