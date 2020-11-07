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

package org.matsim.contrib.drt.optimizer.insertion;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Positive;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ExtensiveInsertionSearchParams extends DrtInsertionSearchParams {
	public static final String SET_NAME = "ExtensiveInsertionSearch";

	public static final String NEAREST_INSERTIONS_AT_END_LIMIT = "nearestInsertionsAtEndLimit";
	@Positive
	private int nearestInsertionsAtEndLimit = 40;

	public static final String ADMISSIBLE_BEELINE_SPEED_FACTOR = "admissibleBeelineSpeedFactor";
	@DecimalMin("1.0")
	private double admissibleBeelineSpeedFactor = 1.0;

	public ExtensiveInsertionSearchParams() {
		super(SET_NAME);
	}

	@StringGetter(NEAREST_INSERTIONS_AT_END_LIMIT)
	public int getNearestInsertionsAtEndLimit() {
		return nearestInsertionsAtEndLimit;
	}

	@StringSetter(NEAREST_INSERTIONS_AT_END_LIMIT)
	public void setNearestInsertionsAtEndLimit(int nearestInsertionsAtEndLimit) {
		this.nearestInsertionsAtEndLimit = nearestInsertionsAtEndLimit;
	}

	@StringGetter(ADMISSIBLE_BEELINE_SPEED_FACTOR)
	public double getAdmissibleBeelineSpeedFactor() {
		return admissibleBeelineSpeedFactor;
	}

	@StringSetter(ADMISSIBLE_BEELINE_SPEED_FACTOR)
	public void setAdmissibleBeelineSpeedFactor(double admissibleBeelineSpeedFactor) {
		this.admissibleBeelineSpeedFactor = admissibleBeelineSpeedFactor;
	}
}
