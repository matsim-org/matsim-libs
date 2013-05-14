/* *********************************************************************** *
 * project: org.matsim.*
 * FullyExploredPlansProvider.java
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.FullExplorationSelector;

/**
 * @author thibautd
 */
public class FullyExploredPlansProvider {
	private static final Logger log =
		Logger.getLogger(FullyExploredPlansProvider.class);

	private FullyExploredPlansProvider() {};

	public static SelectedInformation getGroupsAndSelected(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final boolean isBlocking) {
		return createGroupsAndSelected( incompFactory , isBlocking );
	}

	private static SelectedInformation createGroupsAndSelected(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final boolean isBlocking) {
		log.info( "creating test plans" );
		final SelectedInformation selecteds = new SelectedInformation();
		final GroupLevelPlanSelector fullSelector =
			new FullExplorationSelector(
					isBlocking,
					incompFactory,
					new FullExplorationSelector.WeightCalculator() {
						@Override
						public double getWeight(
								final Plan indivPlan,
								final ReplanningGroup replanningGroup) {
							final Double score = indivPlan.getScore();
							return score == null ? Double.POSITIVE_INFINITY : score;
						}
					});

		final int nTries = 20000;
		log.info( nTries+" random test plans" );
		final Counter counter = new Counter( "Create test instance # " );
		final Random random = new Random( 1234 );
		for ( int i=0; i < nTries; i++ ) {
			counter.incCounter();
			final ReplanningGroup clique = createNextTestClique( selecteds.getJointPlans() , random );
			final GroupPlans fullResult = fullSelector.selectPlans(
						selecteds.getJointPlans(),
						clique );
			selecteds.addInformation(
						clique,
						fullResult );
		}
		counter.printCounter();

		return selecteds;
	}

	private static ReplanningGroup createNextTestClique(
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

	public static class SelectedInformation {
		private final JointPlans jointPlans = new JointPlans();
		private final ArrayList<Tuple<ReplanningGroup, GroupPlans>> plans =
			new ArrayList<Tuple<ReplanningGroup, GroupPlans>>();

		public JointPlans getJointPlans() {
			return jointPlans;
		}

		public Iterable<Tuple<ReplanningGroup, GroupPlans>> getGroupInfos() {
			return plans;
		}

		private void addInformation(
				final ReplanningGroup group,
				final GroupPlans selected) {
			plans.add( new Tuple<ReplanningGroup, GroupPlans>( group , selected ) );
		}
	}
}

