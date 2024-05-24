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

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeMatrixParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public static final String SET_NAME = "travelTimeMatrix";

	// Satisfying only one criterion (max distance or travel time) is enough to be considered a neighbour.

	@Parameter
	@Comment("Max network distance from node A to node B for B to be considered a neighbor of A."
			+ " In such cases, a network travel time from A to B is calculated and stored in the sparse travel time matrix."
			+ " Typically, 'maxNeighborDistance' should be higher than 'cellSize' (e.g. 5-10 times)"
			+ " in order to reduce the impact of imprecise zonal travel times for short distances."
			+ " On the other, a too big value will result in large neighborhoods, which may slow down queries."
			+ " The unit is meters. Default value is 1000 m.")
	@PositiveOrZero
	public double maxNeighborDistance = 1000; //[m]

	@Parameter
	@Comment("Max network travel time from node A to node B for B to be considered a neighbor of A."
			+ " In such cases, a network travel time from A to B is calculated and stored in the sparse travel time matrix."
			+ " Typically, 'maxNeighborTravelTime' should correspond to a distance that are higher than 'cellSize' (e.g. 5-10 times)"
			+ " in order to reduce the impact of imprecise zonal travel times for short distances."
			+ " On the other, a too big value will result in large neighborhoods, which may slow down queries."
			+ " The unit is seconds. Default value is 0 s (for backward compatibility).")
	@PositiveOrZero
	public double maxNeighborTravelTime = 0; //[s]
	private ZoneSystemParams zoneSystemParams;


	public DvrpTravelTimeMatrixParams() {
		super(SET_NAME);
		initSingletonParameterSets();
	}


	private void initSingletonParameterSets() {

		//insertion search params (one of: extensive, selective, repeated selective)
		addDefinition(SquareGridZoneSystemParams.SET_NAME, SquareGridZoneSystemParams::new,
			() -> zoneSystemParams,
			params -> zoneSystemParams = (SquareGridZoneSystemParams)params);

		addDefinition(GISFileZoneSystemParams.SET_NAME, GISFileZoneSystemParams::new,
			() -> zoneSystemParams,
			params -> zoneSystemParams = (GISFileZoneSystemParams)params);

		addDefinition(H3GridZoneSystemParams.SET_NAME, H3GridZoneSystemParams::new,
			() -> zoneSystemParams,
			params -> zoneSystemParams = (H3GridZoneSystemParams)params);
	}

	public ZoneSystemParams getZoneSystemParams() {
		return zoneSystemParams;
	}
}
