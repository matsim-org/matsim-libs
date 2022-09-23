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

package org.matsim.contrib.drt.optimizer;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author Steffen Axer
 */
public class DrtRequestInsertionRetryParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "dvrpRequestRetry";

	@Parameter
	@Comment("The time interval at which rejected request are repeated." + " Default value is 120 s.")
	@Positive
	public int retryInterval = 120;

	@Parameter
	@Comment("The maximum age of a request until it gets finally rejected, if not already scheduled."
			+ " The default value is 0 s (i.e. no retry).")
	@PositiveOrZero
	public double maxRequestAge = 0;// no retry by default

	public DrtRequestInsertionRetryParams() {
		super(SET_NAME);
	}
}
