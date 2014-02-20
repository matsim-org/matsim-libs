/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateRandomHouseholdScenario.java
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;

import playground.thibautd.utils.UniqueIdFactory;

/**
 * @author thibautd
 */
public class GenerateRandomHouseholdScenario {
	public static void main(final String[] args) {
		final String inputNetworkFile = args[ 0 ];
		final String outputPopulationFile = args[ 1 ];
		final String outputHouseholdsFile = args[ 2 ];
		final int popSize = Integer.valueOf( args[ 3 ] ).intValue();
		final int maxHouseholdSize = Integer.valueOf( args[ 4 ] ).intValue();

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( scenario ).readFile( inputNetworkFile );

		final Network network = scenario.getNetwork();
		final Population population = scenario.getPopulation();
		final PopulationFactory popFactory = population.getFactory();
		final Households households = new HouseholdsImpl();

		final HouseholdSizes hhSizes = new HouseholdSizes( maxHouseholdSize );
		final RandomLinkGetter randomLinks = new RandomLinkGetter( network );
		final UniqueIdFactory hhIdFactory = new UniqueIdFactory( "hh-" );
		final UniqueIdFactory personIdFactory = new UniqueIdFactory( "person-" );
		final Random random = new Random( 1234 );
		while ( population.getPersons().size() < popSize ) {
			final int hhSize = hhSizes.nextSize();
			final Id hhId = hhIdFactory.createNextId();
			final Household hh = households.getFactory().createHousehold( hhId );
			((HouseholdsImpl) households).addHousehold( hh );
			// that's stupid. This has to be changed.
			// household should not change the ref to its internal list,
			// and should have "add" methods.
			final List<Id> members = new ArrayList<Id>();
			((HouseholdImpl) hh).setMemberIds( members );

			final Link homeLink = randomLinks.nextLink();

			for (int i=0; i < hhSize; i+=2 ) {
				final Link workLink = randomLinks.nextLink();
				final Link leisureLink = randomLinks.nextLink();

				// generate twice the "same" plan, once with car, once without
				final Person driver = popFactory.createPerson( personIdFactory.createNextId() );
				((PersonImpl) driver).setCarAvail( "always" );
				createPlan( random , popFactory , driver , homeLink , workLink , leisureLink );
				members.add( driver.getId() );
				population.addPerson( driver );

				final Person passenger = popFactory.createPerson( personIdFactory.createNextId() );
				((PersonImpl) passenger).setCarAvail( "never" );
				createPlan( random , popFactory , passenger , homeLink , workLink , leisureLink );
				members.add( passenger.getId() );
				population.addPerson( passenger );
			}
		}

		new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).write( outputPopulationFile );
		new HouseholdsWriterV10( households ).writeFile( outputHouseholdsFile );
	}

	private static Plan createPlan(
			final Random random,
			final PopulationFactory popFactory,
			final Person person,
			final Link homeLink,
			final Link workLink,
			final Link leisureLink) {
		final Plan plan = popFactory.createPlan();
		person.addPlan( plan );
		plan.setPerson( person );

		plan.addActivity(
				createActivity(
					popFactory,
					"h",
					homeLink,
					random.nextDouble() * 24 * 3600 ) );

		plan.addLeg( popFactory.createLeg( TransportMode.pt ) );

		plan.addActivity(
				createActivity(
					popFactory,
					"w",
					workLink,
					random.nextDouble() * 24 * 3600 ) );

		plan.addLeg( popFactory.createLeg( TransportMode.pt ) );

		plan.addActivity(
				createActivity(
					popFactory,
					"l",
					leisureLink,
					random.nextDouble() * 24 * 3600 ) );

		plan.addLeg( popFactory.createLeg( TransportMode.pt ) );

		plan.addActivity(
				createActivity(
					popFactory,
					"h",
					homeLink,
					Time.UNDEFINED_TIME ) );

		return plan;
	}

	private static Activity createActivity(
			final PopulationFactory popFactory,
			final String type,
			final Link link,
			final double endTime) {
		final Activity act = popFactory.createActivityFromLinkId( type , link.getId() );
		((ActivityImpl) act).setCoord( link.getCoord() );
		act.setEndTime( endTime );

		return act;
	}

	private static class RandomLinkGetter {
		private final List<Id> ids;
		private final Network network;
		private final Random random = new Random( 1234 );

		public RandomLinkGetter( final Network network ) {
			this.network = network;
			this.ids = new ArrayList<Id>( network.getLinks().keySet() );
			Collections.sort( ids );
		}

		public Link nextLink() {
			return network.getLinks().get(
					ids.get( random.nextInt( ids.size() ) ) );
		}
	}

	private static class HouseholdSizes {
		private final int step = 2;
		private final int min = 2;
		private final int max;

		private int current = 0;

		public HouseholdSizes(final int max) {
			this.max = max;
		}

		public int nextSize() {
			return min + ( (current += step) % (max - min) );
		}
	}
}

