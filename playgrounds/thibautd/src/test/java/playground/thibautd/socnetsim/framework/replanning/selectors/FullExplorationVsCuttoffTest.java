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
package playground.thibautd.socnetsim.framework.replanning.selectors;


import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.framework.replanning.selectors.FullyExploredPlansProvider.SelectedInformation;

/**
 * @author thibautd
 */
public class FullExplorationVsCuttoffTest {
	private static final Logger log =
		Logger.getLogger(FullExplorationVsCuttoffTest.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testFullExplorationVsCuttoffNonBlocking() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffNonBlocking()" );
		testFullExplorationVsCuttoff( new EmptyIncompatiblePlansIdentifierFactory() , false );
	}

	@Test
	public void testFullExplorationVsCuttoffBlocking() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffBlocking()" );
		testFullExplorationVsCuttoff( new EmptyIncompatiblePlansIdentifierFactory() , true );
	}

	@Test
	public void testFullExplorationVsCuttoffIncompatibility() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffIncompatibility()" );
		testFullExplorationVsCuttoff(
				new FewGroupsIncompatibilityFactory(),
				false );
	}

	@Test
	public void testFullExplorationVsCuttoffIncompatibilityBlocking() throws Exception {
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
			Assert.assertEquals(
					"different solutions with both exploration types."+
					"          full score: "+scoreFull+
					"          fast: "+scoreFast+
					"          ",
					fastResult,
					fullResult);
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
