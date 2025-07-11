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
 * @author nkuehnel
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
	private double expansionFactor = 2;

	@Parameter
	@PositiveOrZero
	@Comment("Minimum expansion in map units (meters in most projections).")
	private double minExpansion = 1000;

	@Parameter
	@PositiveOrZero
	@Comment("Maximum expansion in map units (meters in most projections).")
	private double maxExpansion = 5000;

	@Parameter
	@Comment("Returns the unfiltered fleet if the filter did not keep enough candidates.")
	private boolean returnAllIfEmpty = true;

	@Parameter
	@Positive
	@Comment("Minimum number of vehicle candidates the filter has to find.")
	private int minCandidates = 1;

	@Parameter
	@PositiveOrZero
	@Comment("Update interval of the periodically built spatial search tree of vehicle positions.")
	private double updateInterval = 5 * 60;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(getExpansionFactor() > 0, "Expansion factor must be greater than zero");
		Verify.verify(getMinExpansion() <= getMaxExpansion(), "Max expansion must not be smaller than minimum expansion");
		Verify.verify(getMinExpansion() >= 0, "Expansion must be greater than zero");
		Verify.verify(getMinCandidates() > 0, "Minimum number of candidates must be positive");
		Verify.verify(getUpdateInterval() >= 0, "Update interval must not be negative");
	}

	@Positive
	public double getExpansionFactor() {
		return expansionFactor;
	}

	public void setExpansionFactor(@Positive double expansionFactor) {
		this.expansionFactor = expansionFactor;
	}

	@PositiveOrZero
	public double getMinExpansion() {
		return minExpansion;
	}

	public void setMinExpansion(@PositiveOrZero double minExpansion) {
		this.minExpansion = minExpansion;
	}

	@PositiveOrZero
	public double getMaxExpansion() {
		return maxExpansion;
	}

	public void setMaxExpansion(@PositiveOrZero double maxExpansion) {
		this.maxExpansion = maxExpansion;
	}

	public boolean isReturnAllIfEmpty() {
		return returnAllIfEmpty;
	}

	public void setReturnAllIfEmpty(boolean returnAllIfEmpty) {
		this.returnAllIfEmpty = returnAllIfEmpty;
	}

	@Positive
	public int getMinCandidates() {
		return minCandidates;
	}

	public void setMinCandidates(@Positive int minCandidates) {
		this.minCandidates = minCandidates;
	}

	@PositiveOrZero
	public double getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(@PositiveOrZero double updateInterval) {
		this.updateInterval = updateInterval;
	}
}
