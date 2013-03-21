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
package playground.thibautd.socnetsim.replanning.selectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.FullExplorationSelector;

/**
 * @author thibautd
 */
public class FullExplorationVsCuttoffTest {
	private static final Logger log =
		Logger.getLogger(FullExplorationVsCuttoffTest.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore
	public void testExistenceOfBlockingDoesNotDependOnOrder() throws Exception {
		final Scenario sc = createTestScenario();
		final HighestScoreSumSelector highestSelector = new HighestScoreSumSelector( true );
		final LowestScoreSumSelectorForRemoval lowestSelector = new LowestScoreSumSelectorForRemoval( );

		final JointPlans jointPlans = sc.getScenarioElement( JointPlans.class );
		final GroupIdentifier cliquesIdentifier = sc.getScenarioElement( GroupIdentifier.class );

		final Counter count = new Counter( "test group # " );
		int c = 0;
		for (ReplanningGroup clique : cliquesIdentifier.identifyGroups( sc.getPopulation() )) {
			count.incCounter();
			final GroupPlans highResult = highestSelector.selectPlans( jointPlans , clique );
			final GroupPlans lowResult = lowestSelector.selectPlans( jointPlans , clique );

			if (highResult == null || lowResult == null) {
				c++;
				assertNull(
						"lowest selector finds no non-blocking comb. but highest finds! "+clique,
						highResult);

				assertNull(
						"highest selector finds no non-blocking comb. but lowest finds! "+clique,
						lowResult);
			}

		}
		count.printCounter();
		if ( c == 0 ) throw new RuntimeException( "nothing tested" );
	}

	@Test
	public void testFullExplorationVsCuttoffNonBlocking() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffNonBlocking()" );
		testFullExplorationVsCuttoff( false );
	}

	@Test
	public void testFullExplorationVsCuttoffBlocking() throws Exception {
		log.info( "test: testFullExplorationVsCuttoffBlocking()" );
		testFullExplorationVsCuttoff( true );
	}

	private void testFullExplorationVsCuttoff(final boolean blocking) throws Exception {
		final Scenario sc = createTestScenario();
		final HighestScoreSumSelector fastSelector = new HighestScoreSumSelector( blocking );
		final GroupLevelPlanSelector fullSelector =
			new FullExplorationSelector(
					blocking,
					new FullExplorationSelector.WeightCalculator() {
						@Override
						public double getWeight(
								final Plan indivPlan,
								final ReplanningGroup replanningGroup) {
							final Double score = indivPlan.getScore();
							return score == null ? Double.POSITIVE_INFINITY : score;
						}
					});

		final JointPlans jointPlans = sc.getScenarioElement( JointPlans.class );
		final GroupIdentifier cliquesIdentifier = sc.getScenarioElement( GroupIdentifier.class );

		final Counter count = new Counter( "test plan # " );
		final SelectionStopWatch stopWatch = new SelectionStopWatch();
		for (ReplanningGroup clique : cliquesIdentifier.identifyGroups( sc.getPopulation() )) {
			count.incCounter();
			stopWatch.startFast();
			final GroupPlans fastResult = fastSelector.selectPlans( jointPlans , clique );
			stopWatch.endFast();
			stopWatch.startFull();
			final GroupPlans fullResult = fullSelector.selectPlans( jointPlans , clique );
			stopWatch.endFull();

			final double scoreFast = calcScore( fastResult );
			final double scoreFull = calcScore( fullResult );

			// allow different results, as long as the scores are identical
			assertEquals(
					"different scores with both exploration types."+
					"          full: "+fullResult+
					"          fast: "+fastResult+
					"          ",
					scoreFull,
					scoreFast,
					MatsimTestUtils.EPSILON);

			if ( !fastResult.equals( fullResult ) ) {
				log.warn( "full result "+fullResult+" with score "+scoreFull+
						" is not identical to fast result "+fastResult+" with score "+scoreFast );
			}
		}
		count.printCounter();

		// print some stats on time needed
		final double timeFast = stopWatch.endFastAndGetTime();
		final double timeFull = stopWatch.endFullAndGetTime();
		log.info( "full took "+timeFull+"ms" );
		log.info( "fast took "+timeFast+"ms" );
		log.info( "full took "+(timeFull / timeFast)+" times the time needed by fast" );
	}

	private double calcScore(final GroupPlans plans) {
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

	private Scenario createTestScenario() {
		// attempt to get a high diversity of joint structures.
		final int nCliques = 1500;
		final int nMembers = 10;
		final int nPlans = 10;
		final int maxJointPlans = 60;
		final double pJoin = 0.2;
		final List<Collection<Id>> cliques = new ArrayList<Collection<Id>>();
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final JointPlans jointPlans = new JointPlans();

		final Random random = new Random( 1234 );
		int currentId = 0;
		for (int i=0; i < nCliques; i++) {
			final Map<Id, Queue<Plan>> plansPerPerson = new LinkedHashMap<Id, Queue<Plan>>();
			final List<Id> currentClique = new ArrayList<Id>();
			cliques.add( currentClique );

			// create plans
			for (int j=0; j < nMembers; j++) {
				final Id id = new IdImpl( currentId++ );
				final PersonImpl person = new PersonImpl( id );
				scenario.getPopulation().addPerson( person );
				for (int k=0; k < nPlans; k++) {
					final Plan plan = new PlanImpl( person );
					person.addPlan( plan );
					plan.setScore( random.nextDouble() * 1000 );
				}
				plansPerPerson.put( id , new LinkedList<Plan>( person.getPlans() ) );
				currentClique.add( id );
			}

			// join plans randomly
			final int nJointPlans = random.nextInt( maxJointPlans );
			for (int p=0; p < nJointPlans; p++) {
				final Map<Id, Plan> jointPlan = new LinkedHashMap<Id, Plan>();
				for (Queue<Plan> plans : plansPerPerson.values()) {
					if ( random.nextDouble() > pJoin ) continue;
					final Plan plan = plans.poll();
					if (plan != null) jointPlan.put( plan.getPerson().getId() , plan );
				}
				if (jointPlan.size() <= 1) continue;
				jointPlans.addJointPlan( jointPlans.getFactory().createJointPlan( jointPlan ) );
			}
		}

		scenario.addScenarioElement( jointPlans );
		scenario.addScenarioElement( new FixedGroupsIdentifier( cliques ) );
		return scenario;
	}

	private static class SelectionStopWatch {
		// I am not sure of what is correct between currentTimeMillis or nanoTime,
		// so use something done by hopefully smart guys
		private StopWatch fast = null;
		private StopWatch full = null;

		public void startFast() {
			if (fast == null) {
				fast = new StopWatch();
				fast.start();
			}
			else {
				fast.resume();
			}
		}

		public void startFull() {
			if (full == null) {
				full = new StopWatch();
				full.start();
			}
			else {
				full.resume();
			}
		}

		public void endFast() {
			fast.suspend();
		}

		public void endFull() {
			full.suspend();
		}

		public long endFastAndGetTime() {
			fast.stop();
			return fast.getTime();
		}

		public long endFullAndGetTime() {
			full.stop();
			return full.getTime();
		}
	}
}

