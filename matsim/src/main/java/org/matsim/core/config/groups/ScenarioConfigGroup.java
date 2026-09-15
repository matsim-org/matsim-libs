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

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author dgrether
 *
 */
public final class ScenarioConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "scenario";

	@Parameter
	@Comment("Number of days the simulation covers. Activity scoring assumes that plans repeat with this period.")
	private double simulationPeriodInDays = 1.0;

	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	public void setSimulationPeriodInDays(final double simulationPeriodInDays) {
		this.simulationPeriodInDays = simulationPeriodInDays;
	}

	public double getSimulationPeriodInDays() {
		return this.simulationPeriodInDays;
	}

}
