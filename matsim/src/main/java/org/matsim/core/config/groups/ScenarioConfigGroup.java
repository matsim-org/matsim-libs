/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioConfigGroup
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.config.groups;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.config.ReflectiveConfigGroup.StringSetter;

/**
 * @author dgrether
 *
 */
public final class ScenarioConfigGroup extends ConfigGroup {
	public static final String GROUP_NAME = "scenario";

	private static final String SIMULATION_PERIOD_DAYS = "simulationPeriodInDays"; // is not yet written to log-output so we can still rename it internally

	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger( ScenarioConfigGroup.class ) ;

	private double simulationPeriodInDays = 1.0;

	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		return map;
	}

	@Override
	public void addParam(final String paramName, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		throw new IllegalArgumentException("Parameter '" + paramName + "' is not supported by config group '" + GROUP_NAME + "'.");
	}
	@Override
	public String getValue(final String param_name) {
		throw new UnsupportedOperationException("Use getters for accessing values!");
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> params = super.getParams();
		return params;
	}

	// Once the methods below are removed throughout the code, those exceptions can be moved into the addParam method.
	// Eventually, the whole scenario config group can be moved away.
	// kai, jul'15

	@StringSetter( SIMULATION_PERIOD_DAYS )
	public void setSimulationPeriodInDays(final double simulationPeriodInDays) {
		this.simulationPeriodInDays = simulationPeriodInDays;
	}

	@StringGetter( SIMULATION_PERIOD_DAYS )
	public double getSimulationPeriodInDays() {
		return this.simulationPeriodInDays;
	}

}
