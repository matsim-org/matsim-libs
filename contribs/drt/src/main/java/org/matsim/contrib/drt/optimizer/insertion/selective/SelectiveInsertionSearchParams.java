/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion.selective;

import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;

import jakarta.validation.constraints.Positive;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SelectiveInsertionSearchParams extends DrtInsertionSearchParams {
	public static final String SET_NAME = "SelectiveInsertionSearch";

	public static final String RESTRICTIVE_BEELINE_SPEED_FACTOR = "restrictiveBeelineSpeedFactor";
	//use values that underestimate the actual speed, so the risk that the pre-filtering returns an insertion that
	//violates time windows constraints (for existing passengers, given the actual path)
	@Positive
	private double restrictiveBeelineSpeedFactor = 0.5;

	public SelectiveInsertionSearchParams() {
		super(SET_NAME);
	}

	@StringGetter(RESTRICTIVE_BEELINE_SPEED_FACTOR)
	public double getRestrictiveBeelineSpeedFactor() {
		return restrictiveBeelineSpeedFactor;
	}

	@StringSetter(RESTRICTIVE_BEELINE_SPEED_FACTOR)
	public void setRestrictiveBeelineSpeedFactor(double restrictiveBeelineSpeedFactor) {
		this.restrictiveBeelineSpeedFactor = restrictiveBeelineSpeedFactor;
	}
}
