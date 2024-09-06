/* *********************************************************************** *
 * project: org.matsim.*
 * FullExplorationVsCuttoffTest.java
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.FullyExploredPlansProvider.SelectedInformation;

/**
 * @author thibautd
 */
@Disabled( "expensive")
public class FullExplorationVsCuttoffTest {
	private static final Logger log =
		LogManager.getLogger(FullExplorationVsCuttoffTest.class);

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testFullExplorationVsCuttoffNonBlocking() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffNonBlocking()" );
		testFullExplorationVsCuttoff( new EmptyIncompatiblePlansIdentifierFactory() , false );
	}

	@Test
	void testFullExplorationVsCuttoffBlocking() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffBlocking()" );
		testFullExplorationVsCuttoff( new EmptyIncompatiblePlansIdentifierFactory() , true );
	}

	@Test
	void testFullExplorationVsCuttoffIncompatibility() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffIncompatibility()" );
		testFullExplorationVsCuttoff(
				new FewGroupsIncompatibilityFactory(),
				false );
	}

	@Test
	void testFullExplorationVsCuttoffIncompatibilityBlocking() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffIncompatibilityBlocking()" );
		testFullExplorationVsCuttoff(
				new FewGroupsIncompatibilityFactory(),
				true );
	}

	private void testFullExplorationVsCuttoff(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final boolean blocking) throws Exception {
		final HighestScoreSumSelector fastSelector =
				new HighestScoreSumSelector(
					incompFactory,
					blocking );

		final Counter count = new Counter( "test plan # " );
		final SelectedInformation selecteds = FullyExploredPlansProvider.getGroupsAndSelected( incompFactory , blocking );
		int countNull = 0;
		for ( Tuple<ReplanningGroup, GroupPlans> groupInfo : selecteds.getGroupInfos() ) {
			final ReplanningGroup clique = groupInfo.getFirst();
			final GroupPlans fullResult = groupInfo.getSecond();
			count.incCounter();
			final GroupPlans fastResult = fastSelector.selectPlans(
					selecteds.getJointPlans(),
					clique );

			final double scoreFast = calcScore( fastResult );
			final double scoreFull = calcScore( fullResult );

			// allow different results, as long as the scores are identical
			Assertions.assertEquals(
					fastResult,
					fullResult,
					"different solutions with both exploration types."+
					"          full score: "+scoreFull+
					"          fast: "+scoreFast+
					"          ");
			if ( fastResult == null ) countNull++;
		}
		count.printCounter();

		log.info( "no valid plan could be found for "+countNull+" groups" );
	}

	private double calcScore(final GroupPlans plans) {
		if ( plans == null ) return Double.NaN;
		double score = 0;

		for (Plan indivPlan : plans.getIndividualPlans()) {
			score += indivPlan.getScore();
		}

		for (JointPlan jp : plans.getJointPlans()) {
			for (Plan indivPlan : jp.getIndividualPlans().values()) {
				score += indivPlan.getScore();
			}
		}

		return score;
	}
}
