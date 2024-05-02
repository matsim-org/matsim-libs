/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.vsp.ev;

import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.vehicles.Vehicle;

public final class UrbanEVUtils {

	public static final String PLAN_CHARGING_DURING_ACTS_ATTRIBUTE_NAME = "planChargingDuringActivities";

	/**
	 * Checks whether {@code electricVehicleSpecification} should be charged during the drivers' activities. <br>
	 * If this is the case, [@code {@link UrbanEVTripsPlanner} will pre-plan charging according to the driver's selected plan. <br>
	 * <p>
	 * Checks for the corresponding attribute with key={@code PLAN_CHARGING_DURING_ACTS_ATTRIBUTE_NAME} in the MATSim input vehicle.
	 * <b>If the attribute was not provided, true is returned!</b>
	 * This means that by default, <i>all</i> electricVehicleSpecifications are planned to get charged during activities and not during trips
	 * (planning for the latter is done by {@code org.matsim.contrib.ev.routing.EVNetworkRoutingModule}.
	 * </p>
	 * @param electricVehicleSpecification
	 * @return
	 */
	public static boolean isChargingDuringActivities(ElectricVehicleSpecification electricVehicleSpecification) {
		Object attribute = electricVehicleSpecification.getMatsimVehicle().getAttributes().getAttribute(PLAN_CHARGING_DURING_ACTS_ATTRIBUTE_NAME);
		return attribute == null ? true : (Boolean) attribute;
	}

	/**
	 * see description for {@code setChargingDuringActivities(Vehicle vehicle, boolean value)}
	 * @param electricVehicleSpecification
	 * @param value
	 */
	public static void setChargingDuringActivities(ElectricVehicleSpecification electricVehicleSpecification, boolean value) {
		setChargingDuringActivities(electricVehicleSpecification.getMatsimVehicle(), value);
	}

	/**
	 * defines whether the vehicle shall be included for planning recharging during the driver's activities (with {@code UrbanEVTripsPlanner}, or not. <br>
	 * In the latter case, recharging may be planned to take place during a trip, if the vehicle is taken for a trip with a mode that is
	 * routed by {@code org.matsim.contrib.ev.routing.EVNetworkRoutingModule}.
	 * @param vehicle
	 * @param value
	 */
	public static void setChargingDuringActivities(Vehicle vehicle, boolean value) {
		vehicle.getAttributes().putAttribute(PLAN_CHARGING_DURING_ACTS_ATTRIBUTE_NAME, value);
	}

}
