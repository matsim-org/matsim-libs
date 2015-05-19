/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanSizeStats.java
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
package playground.thibautd.socnetsim.usage.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;

/**
 * @author thibautd
 */
public class JointPlanSizeStats extends AbstractPlanAnalyzerPerGroup {
	private final JointPlans jointPlans;

	public JointPlanSizeStats(
			final int graphWriteInterval,
			final OutputDirectoryHierarchy controlerIO,
			final Scenario scenario,
			final GroupIdentifier groupIdentifier) {
		super(graphWriteInterval, controlerIO, scenario, groupIdentifier);
		this.jointPlans = (JointPlans) scenario.getScenarioElement( JointPlans.ELEMENT_NAME );
	}

	@Override
	protected double calcStat(final Plan plan) {
		final JointPlan jp = jointPlans.getJointPlan( plan );
		return jp == null ? 1 : jp.getIndividualPlans().size();
	}

	@Override
	protected String getStatName() {
		return "Joint Plan Size";
	}
}

