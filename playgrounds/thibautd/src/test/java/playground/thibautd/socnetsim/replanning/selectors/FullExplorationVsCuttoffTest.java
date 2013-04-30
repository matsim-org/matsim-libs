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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
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

		final Counter count = new Counter( "test plan # " );
		final SelectionStopWatch stopWatch = new SelectionStopWatch();
		final JointPlans jointPlans = new JointPlans();
		final Random random = new Random( 1234 );
		final int nTries = 20000;
		int countNull = 0;
		log.info( nTries+" random test plans" );
		for ( int i=0; i < nTries; i++ ) {
			final ReplanningGroup clique = createNextTestClique( jointPlans , random );
			count.incCounter();
			stopWatch.startFast();
			final GroupPlans fastResult = fastSelector.selectPlans(
					incompFactory,
					jointPlans,
					clique );
			stopWatch.endFast();
			stopWatch.startFull();
			final GroupPlans fullResult = fullSelector.selectPlans(
					incompFactory,
					jointPlans,
					clique );
			stopWatch.endFull();

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

		// print some stats on time needed
		final double timeFast = stopWatch.endFastAndGetTime();
		final double timeFull = stopWatch.endFullAndGetTime();
		log.info( "no valid plan could be found for "+countNull+" groups" );
		log.info( "full took "+timeFull+"ms" );
		log.info( "fast took "+timeFast+"ms" );
		log.info( "full took "+(timeFull / timeFast)+" times the time needed by fast (with assertions enabled...)" );
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

	private ReplanningGroup createNextTestClique(
			final JointPlans jointPlans,
			final Random random) {
		// attempt to get a high diversity of joint structures.
		final int nMembers = 1 + random.nextInt( 20 );
		final int nPlans = 1 + random.nextInt( 10 );
		// this is the max number of attempts to create a joint plan
		final int maxJointPlans = 1000;
		final double pJoin = random.nextDouble();
		final ReplanningGroup group = new ReplanningGroup();
		final PopulationFactory factory = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();

		int currentId = 0;
		final Map<Id, Queue<Plan>> plansPerPerson = new LinkedHashMap<Id, Queue<Plan>>();

		// create plans
		for (int j=0; j < nMembers; j++) {
			final Id id = new IdImpl( currentId++ );
			final Person person = factory.createPerson( id );
			group.addPerson( person );
			for (int k=0; k < nPlans; k++) {
				final Plan plan = factory.createPlan();
				plan.setPerson( person );
				person.addPlan( plan );
				plan.setScore( random.nextDouble() * 1000 );
			}
			plansPerPerson.put( id , new LinkedList<Plan>( person.getPlans() ) );
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

		return group;
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

class FewGroupsIncompatibilityFactory implements IncompatiblePlansIdentifierFactory {

	@Override
	public IncompatiblePlansIdentifier createIdentifier(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Set<JointPlan> knownJointPlans = new HashSet<JointPlan>();
		final IncompatiblePlansIdentifierImpl identifier = new IncompatiblePlansIdentifierImpl();

		for ( Person person : group.getPersons() ) {
			int i = 0;
			for ( Plan plan : person.getPlans() ) {
				final JointPlan jp = jointPlans.getJointPlan( plan );
				final int groupNr =  i++ % 3;

				if ( groupNr == 0 ) continue;
				final Set<Id> groups =
						Collections.<Id>singleton(
							new IdImpl( groupNr ) );
				if ( jp == null ) {
					identifier.put(
							plan,
							groups );
				}
				else if ( !knownJointPlans.add( jp ) ) {
					for ( Plan p : jp.getIndividualPlans().values() ) {
						identifier.put(
								p,
								groups );
					}
				}
			}
		}

		return identifier;
	}
}
