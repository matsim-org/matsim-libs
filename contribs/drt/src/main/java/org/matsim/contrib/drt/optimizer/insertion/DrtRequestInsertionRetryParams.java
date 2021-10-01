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

import java.util.Map;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Steffen Axer
 */
public class DrtRequestInsertionRetryParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "dvrpRequestRetry";

	private static final String RETRY_INTERVAL = "retryInterval";
	private static final String RETRY_INTERVAL_EXP = "The time interval at which rejected request are repeated."
			+ " Default value is 120 s.";

	private static final String MAX_REQUEST_AGE = "maxRequestAge";
	private static final String MAX_REQUEST_AGE_EXP =
			"The maximum age of a request until it gets finally rejected, if not already scheduled."
					+ " The default value is 0 s (i.e. no retry).";

	public DrtRequestInsertionRetryParams() {
		super(SET_NAME);
	}

	@Positive
	private int retryInterval = 120;

	@PositiveOrZero
	private double maxRequestAge = 0;// no retry by default

	@StringGetter(RETRY_INTERVAL)
	public int getRetryInterval() {
		return retryInterval;
	}

	@StringSetter(RETRY_INTERVAL)
	public DrtRequestInsertionRetryParams setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
		return this;
	}

	@StringGetter(MAX_REQUEST_AGE)
	public double getMaxRequestAge() {
		return maxRequestAge;
	}

	@StringSetter(MAX_REQUEST_AGE)
	public DrtRequestInsertionRetryParams setMaxRequestAge(double maxRequestAge) {
		this.maxRequestAge = maxRequestAge;
		return this;
	}

	@Override
	public Map<String, String> getComments() {
		var map = super.getComments();
		map.put(RETRY_INTERVAL, RETRY_INTERVAL_EXP);
		map.put(MAX_REQUEST_AGE, MAX_REQUEST_AGE_EXP);
		return map;
	}
}
