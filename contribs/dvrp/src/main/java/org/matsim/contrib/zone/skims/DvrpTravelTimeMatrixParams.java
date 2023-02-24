/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone.skims;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeMatrixParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "travelTimeMatrix";

	@Parameter
	@Comment("size of square cells (meters) used for computing travel time matrix." + " Default value is 200 m")
	@Positive
	public double cellSize = 200; //[m]

	@Parameter
	@Comment("Max network distance from node A to node B for B to be considered a neighbor of A."
			+ " In such cases, a network travel time from A to B is calculated and stored in the sparse travel time matrix."
			+ " Typically, 'maxNeighborDistance' should be higher than 'cellSize' (e.g. 5-10 times)"
			+ " in order to reduce the impact of imprecise zonal travel times for short distances."
			+ " On the other, a too big value will result in large neighborhoods, which may slow down queries."
			+ " The unit is meters. Default value is 1000 m.")
	@PositiveOrZero
	public double maxNeighborDistance = 1000; //[m]

	public DvrpTravelTimeMatrixParams() {
		super(SET_NAME);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		return super.createParameterSet(type);
	}
}
