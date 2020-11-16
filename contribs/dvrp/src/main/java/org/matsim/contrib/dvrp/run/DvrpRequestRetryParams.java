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

package org.matsim.contrib.dvrp.run;

import java.util.Map;

import javax.validation.constraints.Positive;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author Steffen Axer
 *
 */
public class DvrpRequestRetryParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "dvrpRequestRetry";
	private static final String RETRY_INTERVAL = "retryInterval";
	private static final String RETRY_INTERVAL_EXP = "The time interval at which rejected request are repeated";
	private static final String MAX_REQUEST_AGE = "maxRequestAge";
	private static final String MAX_REQUEST_AGE_EXP = "The maximum age of a request till it gets finally rejected, if not already scheduled"
			+ "Used with serviceArea Operational Scheme";

	public DvrpRequestRetryParams() {
		super(SET_NAME);
	}
	
	@Positive
	private double retryInterval = 100.0;
	
	@Positive
	private double maxRequestAge = 600.0;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

	}

	@StringGetter(RETRY_INTERVAL)
	public double getRetryInterval() {
		return retryInterval;
	}

	@StringSetter(RETRY_INTERVAL)
	public void setRetryInterval(double retryInterval) {
		this.retryInterval = retryInterval;
	}

	@StringGetter(MAX_REQUEST_AGE)
	public double getMaxRequestAge() {
		return maxRequestAge;
	}

	@StringSetter(MAX_REQUEST_AGE)
	public void setMaxRequestAge(double maxRequestAge) {
		this.maxRequestAge = maxRequestAge;
	}
	
	@Override
	public ConfigGroup createParameterSet(String type) {
		return super.createParameterSet(type);
	}
	
	
	@Override
	public Map<String, String> getComments() {
		var map = super.getComments();
		map.put(RETRY_INTERVAL, RETRY_INTERVAL_EXP);
		map.put(MAX_REQUEST_AGE,MAX_REQUEST_AGE_EXP );
		return map;
	}

}
