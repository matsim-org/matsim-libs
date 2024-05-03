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

package org.matsim.contrib.drt.analysis.zonal;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZoneSystemParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public static final String SET_NAME = "zonalSystem";

	public DrtZoneSystemParams() {
		super(SET_NAME);
		initSingletonParameterSets();
	}

	public enum TargetLinkSelection {random, mostCentral}

	@Parameter("zoneTargetLinkSelection")
	@Comment("Defines how the target link of a zone is determined (e.g. for rebalancing)."
			+ " Possible values are [random,mostCentral]. Default behavior is mostCentral, where all vehicles are sent to the same link.")
	@NotNull
	public TargetLinkSelection targetLinkSelection = TargetLinkSelection.mostCentral;

	private ZoneSystemParams zoneSystemParams;


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
