/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.ResourceType;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * Utility class for working with Railsim and its specific attributes.
 *
 * @author ikaddoura
 * @author rakow
 * @author munterfi
 */
public final class RailsimUtils {

	public static final String LINK_ATTRIBUTE_RESOURCE_ID = "railsimResourceId";
	public static final String LINK_ATTRIBUTE_CAPACITY = "railsimTrainCapacity";
	public static final String LINK_ATTRIBUTE_MINIMUM_TIME = "railsimMinimumTime";
	public static final String VEHICLE_ATTRIBUTE_ACCELERATION = "railsimAcceleration";
	public static final String VEHICLE_ATTRIBUTE_DECELERATION = "railsimDeceleration";
	public static final String RESOURCE_TYPE = "railsimResourceType";

	private RailsimUtils() {
	}

	/**
	 * Round number to precision commonly used in Railsim.
	 */
	public static double round(double d) {
		return BigDecimal.valueOf(d).setScale(3, RoundingMode.HALF_EVEN).doubleValue();
	}

	/**
	 * Return the train capacity for this link, if no link attribute is provided the default is 1.
	 */
	public static int getTrainCapacity(Link link) {
		Object attr = link.getAttributes().getAttribute(LINK_ATTRIBUTE_CAPACITY);
		return attr != null ? (int) attr : 1;
	}

	/**
	 * Sets the train capacity for the link.
	 */
	public static void setTrainCapacity(Link link, int capacity) {
		link.getAttributes().putAttribute(LINK_ATTRIBUTE_CAPACITY, capacity);
	}

	/**
	 * Return the minimum time for the switch at the end of the link (toNode); if no link attribute is provided the default is 0.
	 */
	public static double getMinimumHeadwayTime(Link link) {
		Object attr = link.getAttributes().getAttribute(LINK_ATTRIBUTE_MINIMUM_TIME);
		return attr != null ? (double) attr : 0;
	}

	/**
	 * Sets the minimum headway time after a link can be released.
	 */
	public static void setMinimumHeadwayTime(Link link, double time) {
		link.getAttributes().putAttribute(LINK_ATTRIBUTE_MINIMUM_TIME, time);
	}

	/**
	 * Resource id or null if there is none.
	 */
	public static String getResourceId(Link link) {
		return (String) link.getAttributes().getAttribute(LINK_ATTRIBUTE_RESOURCE_ID);
	}

	/**
	 * Sets the resource id for the link.
	 */
	public static void setResourceId(Link link, String resourceId) {
		link.getAttributes().putAttribute(LINK_ATTRIBUTE_RESOURCE_ID, resourceId);
	}

	/**
	 * Whether this link is an entry link applicable for re routing.
	 */
	public static boolean isEntryLink(Link link) {
		return Boolean.TRUE.equals(link.getAttributes().getAttribute("railsimEntry"));
	}

	/**
	 * Sets whether this link is an entry link applicable for re-routing.
	 */
	public static void setEntryLink(Link link, boolean isEntry) {
		link.getAttributes().putAttribute("railsimEntry", isEntry);
	}

	/**
	 * Exit link used for re routing.
	 */
	public static boolean isExitLink(Link link) {
		return Boolean.TRUE.equals(link.getAttributes().getAttribute("railsimExit"));
	}

	/**
	 * Sets whether this link is an exit link used for re-routing.
	 */
	public static void setExitLink(Link link, boolean isExit) {
		link.getAttributes().putAttribute("railsimExit", isExit);
	}

	/**
	 * Return the default deceleration time or the vehicle-specific value.
	 */
	public static double getTrainDeceleration(VehicleType vehicle, RailsimConfigGroup railsimConfigGroup) {
		double deceleration = railsimConfigGroup.decelerationDefault;
		Object attr = vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_DECELERATION);
		return attr != null ? (double) attr : deceleration;
	}

	/**
	 * Sets the deceleration time for the vehicle type.
	 */
	public static void setTrainDeceleration(VehicleType vehicle, double deceleration) {
		vehicle.getAttributes().putAttribute(VEHICLE_ATTRIBUTE_DECELERATION, deceleration);
	}

	/**
	 * Return the default acceleration time or the vehicle-specific value.
	 */
	public static double getTrainAcceleration(VehicleType vehicle, RailsimConfigGroup railsimConfigGroup) {
		double acceleration = railsimConfigGroup.accelerationDefault;
		Object attr = vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_ACCELERATION);
		return attr != null ? (double) attr : acceleration;
	}

	/**
	 * Sets the acceleration time for the vehicle type.
	 */
	public static void setTrainAcceleration(VehicleType vehicle, double acceleration) {
		vehicle.getAttributes().putAttribute(VEHICLE_ATTRIBUTE_ACCELERATION, acceleration);
	}

	/**
	 * Return the resource type for a link, if not set, fixed block is assumed.
	 */
	public static ResourceType getResourceType(Link link) {
		Object attr = link.getAttributes().getAttribute(RESOURCE_TYPE);
		return attr != null ? ResourceType.valueOf(attr.toString()) : ResourceType.fixedBlock;
	}

	/**
	 * Sets the resource type for the link.
	 */
	public static void setResourceType(Link link, ResourceType type) {
		link.getAttributes().putAttribute(RESOURCE_TYPE, type.toString());
	}

}
