/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * A config group to control how often the included default analyses should be run.
 *
 * @author mrieser
 */
public class AnalysisConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "analysis";

	public AnalysisConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	@Comment("Defines in which iterations the legHistogram analysis is run (iterationNumber % interval == 0). Use 0 to disable this analysis.")
	private int legHistogramInterval = 1;

	@Parameter
	@Comment("Defines in which iterations the legDurations analysis is run (iterationNumber % interval == 0). Use 0 to disable this analysis.")
	private int legDurationsInterval = 1;

	public int getLegHistogramInterval() {
		return this.legHistogramInterval;
	}

	public void setLegHistogramInterval(int legHistogramInterval) {
		this.legHistogramInterval = legHistogramInterval;
	}

	public int getLegDurationsInterval() {
		return this.legDurationsInterval;
	}

	public void setLegDurationsInterval(int legDurationsInterval) {
		this.legDurationsInterval = legDurationsInterval;
	}
}
