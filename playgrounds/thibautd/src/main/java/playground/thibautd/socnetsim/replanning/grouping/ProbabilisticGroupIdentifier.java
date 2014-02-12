/* *********************************************************************** *
 * project: org.matsim.*
 * ProbabilisticGroupIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.grouping;

import java.util.Collection;
import java.util.Random;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;

/**
 * @author thibautd
 */
public class ProbabilisticGroupIdentifier implements GroupIdentifier {
	private final Scenario scenario;
	private static final double PROBA_ACTIVATION = 0.01;
	private final Random random = MatsimRandom.getLocalInstance();

	public  ProbabilisticGroupIdentifier(final Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public Collection<ReplanningGroup> identifyGroups(
			final Population population) {
		return GroupingUtils.randomlyGroupPersons(
				random,
				PROBA_ACTIVATION,
				0d,
				population,
				(JointPlans) scenario.getScenarioElement( JointPlans.ELEMENT_NAME ),
				(SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME ) );
	}
}

