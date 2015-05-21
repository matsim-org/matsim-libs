/* *********************************************************************** *
 * project: org.matsim.*
 * FilteredScoreStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.usage.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * @author thibautd
 */
public class FilteredScoreStats extends AbstractPlanAnalyzerPerGroup {

	public FilteredScoreStats(
			final int graphWriteInterval,
			final OutputDirectoryHierarchy controlerIO,
			final Scenario scenario,
			final GroupIdentifier groupIdentifier) {
		super(graphWriteInterval, controlerIO, scenario, groupIdentifier);
	}

	@Override
	protected double calcStat(final Plan plan) {
		final Double score = plan.getScore();
		return score == null ? Double.NaN : score;
	}

	@Override
	protected String getStatName() {
		return "Score";
	}
}

