/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.insertion.spatialFilter;

import com.google.common.base.Verify;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;

/**
 * @author steffenaxer
 */
public class DrtSpatialRequestFleetFilterParams extends ReflectiveConfigGroupWithConfigurableParameterSets {

	public static final String SET_NAME = "spatialRequestFleetFilter";

	public DrtSpatialRequestFleetFilterParams() {
		super(SET_NAME);
	}

	@Parameter
	@Positive
	@Comment("Expansion factor for the iterative expansion of search radius until max expansion or the" +
			"minimum number of candidates is reached. Must be positive.")
	public double expansionFactor = 2;

	@Parameter
	@PositiveOrZero
	@Comment("Minimum expansion in map units (meters in most projections).")
	public double minExpansion = 1000;

	@Parameter
	@PositiveOrZero
	@Comment("Maximum expansion in map units (meters in most projections).")
	public double maxExpansion = 5000;

	@Parameter
	@Comment("Returns the unfiltered fleet if the filter did not keep enough candidates.")
	public boolean returnAllIfEmpty = true;

	@Parameter
	@Positive
	@Comment("Minimum number of vehicle candidates the filter has to find.")
	public int minCandidates = 1;

	@Parameter
	@PositiveOrZero
	@Comment("Update interval of the periodically built spatial search tree of vehicle positions.")
	public double updateInterval = 5 * 600;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(expansionFactor > 0, "Expansion factor must be greater than zero");
		Verify.verify(minExpansion <= maxExpansion, "Max expansion must not be smaller than minimum expansion");
		Verify.verify(minExpansion >= 0, "Expansion must be greater than zero");
		Verify.verify(minCandidates > 0, "Minimum number of candidates must be positive");
		Verify.verify(updateInterval >= 0, "Update interval must not be negative");
	}
}
