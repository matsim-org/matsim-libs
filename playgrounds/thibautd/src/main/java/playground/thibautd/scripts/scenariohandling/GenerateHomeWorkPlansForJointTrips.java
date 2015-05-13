/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateEquilPlansForJointTrips.java
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
package playground.thibautd.scripts.scenariohandling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.socnetsim.framework.cliques.Clique;

/**
 * @author thibautd
 */
public class GenerateHomeWorkPlansForJointTrips {
	private static final Id<Link> HOME_LINK_ID = Id.create( 1 , Link.class );
	private static final Id<Link> WORK_LINK_ID = Id.create( 2 , Link.class );

	public static void main(final String[] args) {
		final String outputCliquesFile = args[ 0 ];
		final String outputPopFile = args[ 1 ];
		// only even clique sizes will be generated
		final int maxCliqueSize = Integer.parseInt( args[ 2 ] );
		final int popSize = Integer.parseInt( args[ 3 ] );

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final Population population = scenario.getPopulation();

		final Random random = new Random( 19239784 );
		final SizeIterator sizes = new SizeIterator( maxCliqueSize );
		final CliquesTracker cliques = new CliquesTracker();
		final IdIterator<Person> ids = new IdIterator<>( "person-" , Person.class);
		final Counter counter = new Counter( "generating clique # ");
		while ( population.getPersons().size() < popSize ) {
			counter.incCounter();
			final int size = sizes.next();
			Collection<Person> persons =
				createClique(
						population.getFactory(),
						random,
						ids,
						size );
			for (Person p : persons) population.addPerson( p );
			cliques.addClique( persons );
		}
		counter.printCounter();

		new PopulationWriter( population , scenario.getNetwork() ).write( outputPopFile );
		new CliquesWriter( cliques.getCliques() ).writeFile( outputCliquesFile );
	}

	private static Collection<Person> createClique(
			final PopulationFactory factory,
			final Random random,
			final IdIterator<Person> ids,
			final int size) {
		if (size % 2 != 0) throw new IllegalArgumentException( ""+size );

		final List<Person> persons = new ArrayList<Person>();
		for (int i=1; i <= size; i++) {
			final boolean isDriver = i % 2 == 0;
			final String mode = isDriver ? TransportMode.car : TransportMode.pt;

			PersonImpl person = new PersonImpl( ids.next() );
			if (!isDriver) person.setCarAvail( "never" );
			persons.add( person );

			assert person.getPlans().size() == 0;
			Plan plan = factory.createPlan();
			plan.setPerson( person );
			person.addPlan( plan );
			assert person.getPlans().size() == 1;
			person.setSelectedPlan( plan );

			Activity act = factory.createActivityFromLinkId( "h" , HOME_LINK_ID );
			act.setEndTime( random.nextDouble() * 12 * 3600 );
			plan.addActivity( act );

			Leg leg = factory.createLeg( mode );
			plan.addLeg( leg );


			act = factory.createActivityFromLinkId( "w" , WORK_LINK_ID );
			act.setEndTime( (1 + random.nextDouble()) * 12 * 3600 );
			plan.addActivity( act );


			leg = factory.createLeg( mode );
			plan.addLeg( leg );


			act = factory.createActivityFromLinkId( "h" , HOME_LINK_ID );
			plan.addActivity( act );
		}

		return persons;
	}

	private static class SizeIterator {
		private int current = 0;
		private final int max;

		public SizeIterator(final int max) {
			this.max = max;
		}

		public int next() {
			current += 2;
			if (current > max) current = 2;
			return current;
		}
	}

	private static class IdIterator<T> {
		private int count = 0;
		private final String prefix;
		private final Class<T> idType;

		public IdIterator( final String prefix , final Class<T> idType) {
			this.prefix = prefix;
			this.idType = idType;
		}

		public Id<T> next() {
			return Id.create( prefix+(count++) , this.idType);
		}
	}

	private static class CliquesTracker {
		private final IdIterator<Clique> ids = new IdIterator<>( "clique-" , Clique.class );
		private final Map<Id<Clique>, List<Id<Person>>> cliques = new HashMap<>();

		public void addClique( final Collection<Person> persons ) {
			final List<Id<Person>> memberIds = new ArrayList<>();

			for (Person p : persons) {
				memberIds.add( p.getId() );
			}

			cliques.put( ids.next() , memberIds );
		}

		public Map<Id<Clique>, List<Id<Person>>> getCliques() {
			return cliques;
		}
	}
}

