/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingcost.config;


import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Set;

/**
 * @author mrieser, jfbischoff (SBB)
 */
public class ParkingCostConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "parkingCosts";

	@Parameter
	@Comment("Enables / disables parkingCost use")
	public boolean useParkingCost = true;

	@Parameter
	@Comment("Parking Cost link Attribute prefix." +
			" This needs to be followed by the parking cost for the mode, " +
			"e.g., \"pc_\" as prefix and \"car\" as mode would " +
			"require an attribute \"pc_ride\" to be set on every " +
			"link where parking costs should be considered.")
	public String linkAttributePrefix = "pc_";

	@Parameter
	@Comment("Modes that should use parking costs, separated by comma")
	public String modesWithParkingCosts = null;

	@Parameter
	@Comment("Activitiy types where no parking costs are charged, e.g., at home. char sequence must be part of the activity.")
	public String activityTypesWithoutParkingCost = null;

	public ParkingCostConfigGroup() {
		super(GROUP_NAME);
	}

	public Set<String> getModesWithParkingCosts() {
		return CollectionUtils.stringToSet(modesWithParkingCosts);
	}

	public Set<String> getActivityTypesWithoutParkingCost() {
		return CollectionUtils.stringToSet(activityTypesWithoutParkingCost);
	}
}
