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

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ExtensiveInsertionSearchParams extends DrtInsertionSearchParams {
	public static final String SET_NAME = "ExtensiveInsertionSearch";

	@Parameter
	@PositiveOrZero
	public int nearestInsertionsAtEndLimit = 10;

	@Parameter
	@DecimalMin("1.0")
	public double admissibleBeelineSpeedFactor = 1.0;

	public ExtensiveInsertionSearchParams() {
		super(SET_NAME);
	}
}
