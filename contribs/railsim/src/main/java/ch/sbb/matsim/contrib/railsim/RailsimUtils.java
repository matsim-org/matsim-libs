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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.vehicles.VehicleType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.ResourceType;
import jakarta.annotation.Nullable;


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
	public static final String LINK_ATTRIBUTE_VMAX = "railsimVMax";
	public static final String LINK_NONBLOCKING_AREA = "railsimNonBlockingArea";

	public static final String VEHICLE_ATTRIBUTE_ACCELERATION = "railsimAcceleration";
	public static final String VEHICLE_ATTRIBUTE_DECELERATION = "railsimDeceleration";
	public static final String VEHICLE_ATTRIBUTE_REVERSIBLE = "railsimReversible";
	public static final String RESOURCE_TYPE = "railsimResourceType";
	public static final String FORMATION = "railsimFormation";

	private static final Joiner.MapJoiner JOINER = Joiner.on(",").withKeyValueSeparator("=");
	private static final Splitter.MapSplitter SPLITTER = Splitter.on(",").withKeyValueSeparator("=");

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
		return attr instanceof Number n ? n.intValue(): 1;
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
	 * Sets whether this link is an intersection area.
	 */
	public static void setLinkNonBlockingArea(Link link, boolean isIntersectionArea) {
		link.getAttributes().putAttribute(LINK_NONBLOCKING_AREA, isIntersectionArea);
	}

	/**
	 * Whether this link is an intersection area.
	 */
	public static boolean isLinkNonBlockingArea(Link link) {
		return Objects.equals(link.getAttributes().getAttribute(LINK_NONBLOCKING_AREA), true);
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
		double deceleration = railsimConfigGroup.getDecelerationDefault();
		Object attr = vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_DECELERATION);
		return attr instanceof Number n ? n.doubleValue() : deceleration;
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
		double acceleration = railsimConfigGroup.getAccelerationDefault();
		Object attr = vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_ACCELERATION);
		return attr instanceof Number n ? n.doubleValue() : acceleration;
	}

	/**
	 * Sets the acceleration time for the vehicle type.
	 */
	public static void setTrainAcceleration(VehicleType vehicle, double acceleration) {
		vehicle.getAttributes().putAttribute(VEHICLE_ATTRIBUTE_ACCELERATION, acceleration);
	}

	/**
	 * Sets whether the train is reversible.
	 *
	 * @param vehicle    The vehicle type to set the attribute for.
	 * @param reversible time in seconds it takes to reverse the train, or null if not reversible.
	 */
	public static void setTrainReversible(VehicleType vehicle, @Nullable Double reversible) {
		if (reversible == null) {
			vehicle.getAttributes().removeAttribute(VEHICLE_ATTRIBUTE_REVERSIBLE);
		} else
			vehicle.getAttributes().putAttribute(VEHICLE_ATTRIBUTE_REVERSIBLE, reversible);
	}

	/**
	 * Returns whether the train can be reversed and the time in seconds it takes.
	 *
	 * @param vehicle The vehicle type to check.
	 * @return time to reverse, if the vehicle is reversible, otherwise an empty OptionalDouble.
	 */
	public static OptionalDouble getTrainReversible(VehicleType vehicle) {
		Object attr = vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_REVERSIBLE);
		return attr instanceof Number n
			? OptionalDouble.of(n.doubleValue())
			: OptionalDouble.empty();
	}

	/**
	 * Sets the maximum velocity (vMax) for the given link, varying by vehicle type.
	 *
	 * @param link                The link for which the maximum velocity is being set.
	 * @param vMaxPerVehicleType  A map specifying the maximum velocity for different vehicle types,
	 *                            where the key is the vehicle type ID and the value is the associated maximum velocity.
	 */
	public static void setLinkVMax(Link link, Map<Id<VehicleType>, Double> vMaxPerVehicleType) {
		if (vMaxPerVehicleType.isEmpty()) {
			link.getAttributes().removeAttribute(LINK_ATTRIBUTE_VMAX);
		} else {
			link.getAttributes().putAttribute(LINK_ATTRIBUTE_VMAX, JOINER.join(vMaxPerVehicleType));
		}
	}

	/**
	 * Return the maximum velocity map for the given link, if not set returns null.
	 */
	public static Map<Id<VehicleType>, Double> getLinkVMax(Link link) {
		Object attr = link.getAttributes().getAttribute(LINK_ATTRIBUTE_VMAX);
		if (attr == null) return Map.of();

		Map<String, String> split = SPLITTER.split(attr.toString());

		return split.entrySet().stream().collect(Collectors.toMap(
			e -> Id.create(e.getKey(), VehicleType.class),
			e -> Double.valueOf(e.getValue()))
		);
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


	/**
	 * Return the defined formation of vehicle units attached to departure.
	 */
	public static List<String> getFormation(Departure departure) {
		Object attr = departure.getAttributes().getAttribute(FORMATION);
		return attr instanceof String s
			? List.of(s.split(","))
			: List.of();
	}

	/**
	 * Sets the formation of vehicle ids for a departure.
	 */
	public static void setFormation(Departure departure, List<String> formations) {
		if (formations.isEmpty()) {
			departure.getAttributes().removeAttribute(FORMATION);
		} else {
			departure.getAttributes().putAttribute(FORMATION, String.join(",", formations));
		}
	}

	/**
	 * Return the id string representation of an identifiable object.
	 */
	public static String objectIdToString(Identifiable<?> obj) {
		if (obj == null) return "";
		return Objects.toString(obj.getId(), "");
	}

}
