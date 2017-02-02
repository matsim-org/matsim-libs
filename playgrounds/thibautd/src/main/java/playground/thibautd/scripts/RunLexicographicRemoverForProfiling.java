/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.scripts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.removers.LexicographicForCompositionExtraPlanRemover;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
public class RunLexicographicRemoverForProfiling {
	private static final Logger log = Logger.getLogger( RunLexicographicRemoverForProfiling.class );
	private static final int POP_SIZE = 10000;
	private static final int N_PLANS = 20;
	private static final int N_JOINT_PLANS = POP_SIZE * 20;

	private static final Random random = new Random( 123 );
	private static final Counter planCounter = new Counter( "create plan # " );

	public static void main( final String... args ) {
		while ( true ) {
			final JointPlans jointPlans = new JointPlans();
			final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

			final Person[] persons = new Person[ POP_SIZE ];
			for ( int i=0; i < POP_SIZE; i++ ) {
				final Person p = sc.getPopulation().getFactory().createPerson( Id.createPersonId( i ) );
				persons[ i ] = p;
				sc.getPopulation().addPerson( p );

				for ( int j=0; j < N_PLANS; j++ ) {
					planCounter.incCounter();
					createPlan( sc, p );
				}
			}
			
			for ( int i=0; i < N_JOINT_PLANS; i++ ) {
				for ( int j=0; j < N_PLANS; j++ ) {
					shuffle( persons, 3 );
					final Person person1 = persons[ 0 ];
					final Person person2 = persons[ 1 ];
					final Person person3 = persons[ 2 ];

					final Plan plan1 = createPlan( sc, person1 );
					final Plan plan2 = createPlan( sc, person2 );
					final Plan plan3 = createPlan( sc, person3 );

					addJointPlan( jointPlans, plan1, plan2, plan3 );
				}
			}
			planCounter.printCounter();
			log.info( (planCounter.getCounter() / (double) POP_SIZE)+" plans per person" );

			new LexicographicForCompositionExtraPlanRemover( 3 , 10 ).removePlansInGroup(
					jointPlans,
					sc.getPopulation().getPersons().values().stream()
							.collect(
									ReplanningGroup::new,
									ReplanningGroup::addPerson,
									(g1 , g2) -> g1.getPersons().forEach( g2::addPerson ) ) );
		}
	}

	private static void addJointPlan(
			final JointPlans jointPlans,
			final Plan... plans ) {
		final Map<Id<Person>,Plan> map = new HashMap<>(  );
		for ( Plan p : plans ) map.put( p.getPerson().getId() , p );
		jointPlans.addJointPlan( jointPlans.getFactory().createJointPlan( map ) );
	}

	private static Plan createPlan( final Scenario sc, final Person p ) {
		planCounter.incCounter();
		final Plan plan = sc.getPopulation().getFactory().createPlan();
		plan.setScore( random.nextDouble() );
		p.addPlan( plan );
		return plan;
	}

	private static void shuffle( final Person[] persons, final int end ) {
		for ( int i=0; i < end; i++ ) {
			final int r = random.nextInt( persons.length );
			final Person p = persons[ i ];
			persons[ i ] = persons[ r ];
			persons[ r ] = p;
		}
	}
}

