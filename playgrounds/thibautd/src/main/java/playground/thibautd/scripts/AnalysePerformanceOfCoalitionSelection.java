/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysePerformanceOfCoalitionSelection.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector;
import playground.thibautd.utils.CollectionUtils;

/**
 * @author thibautd
 */
public class AnalysePerformanceOfCoalitionSelection {
	private static final int N_TRIES = 10000;

	private static final int MIN_GROUP_SIZE = 1;
	private static final int MAX_GROUP_SIZE = 50;
	private static final int MAX_MAX_PLANS_PER_SIZE = 10;

	private static final String SEP = "\t";

	public static void main(final String[] args) throws IOException {
		final String outputDataFile = args[ 0 ];

		final BufferedWriter writer = IOUtils.getBufferedWriter( outputDataFile );
		writer.write( "group_size" );
		writer.write( SEP );
		writer.write( "max_plans_per_size" );
		writer.write( SEP );
		writer.write( "duration_ns" );

		final CoalitionSelector selector = new CoalitionSelector();

		final Random random = new Random( 1 );
		final Counter counter = new Counter( "group # " );
		for ( int iter=0; iter < N_TRIES; iter++ ) {
			counter.incCounter();
			final JointPlans jps = new JointPlans();
			final int groupSize = MIN_GROUP_SIZE + random.nextInt( MAX_GROUP_SIZE - MIN_GROUP_SIZE );
			final int maxPlansPerSize = 1 + random.nextInt( MAX_MAX_PLANS_PER_SIZE );

			final ReplanningGroup group =
				createGroup( 
						random,
						jps,
						groupSize,
						maxPlansPerSize );

			final long start = System.nanoTime();
			selector.selectPlans( jps , group );
			final long end = System.nanoTime();

			writer.newLine();
			writer.write( ""+groupSize );
			writer.write( SEP );
			writer.write( ""+maxPlansPerSize );
			writer.write( SEP );
			writer.write( ""+(end - start) );
		}
		counter.printCounter();

		writer.close();
	}

	private static ReplanningGroup createGroup(
			final Random random,
			final JointPlans jps,
			final int groupSize,
			final int maxPlansPerSize) {
		final ReplanningGroup group = new ReplanningGroup();

		for ( int i = 0; i < groupSize; i++ ) {
			group.addPerson( new PersonImpl( Id.create( i , Person.class ) ) );
		}

		for ( Person person : group.getPersons() ) {
			for ( int i = 0; i < maxPlansPerSize; i++ ) {
				final Plan plan = jps.getFactory().createIndividualPlan( person );
				person.addPlan( plan );
				plan.setScore( random.nextDouble() );
			}
		}

		for ( int size = 2; size <= groupSize; size++ ) {
			final List<Person> personsWithCapacity = new ArrayList<Person>( group.getPersons() );
			final Map<Person, Integer> nPlansPerPerson = new HashMap<Person, Integer>();

			while ( personsWithCapacity.size() >= size ) {
				final Collection<Person> personInJp =
					CollectionUtils.getRandomDistinctElements(
							random,
							personsWithCapacity,
							size );

				final Map<Id<Person>, Plan> jp = new HashMap< >();
				for ( Person person : personInJp ) {
					for ( int i = 0; i < maxPlansPerSize; i++ ) {
						final Plan plan = jps.getFactory().createIndividualPlan( person );
						person.addPlan( plan );
						plan.setScore( random.nextDouble() );
						jp.put( person.getId() , plan );
					}

					final Integer prevCount = nPlansPerPerson.remove( person );
					final int newCount = prevCount == null ? 1 : prevCount + 1;

					assert newCount >= 1;
					assert newCount <= maxPlansPerSize;

					if ( newCount == maxPlansPerSize ) personsWithCapacity.remove( person );
					else nPlansPerPerson.put( person , newCount );

				}
				jps.addJointPlan( jps.getFactory().createJointPlan( jp ) );
			}
		}

		return group;
	}
}

