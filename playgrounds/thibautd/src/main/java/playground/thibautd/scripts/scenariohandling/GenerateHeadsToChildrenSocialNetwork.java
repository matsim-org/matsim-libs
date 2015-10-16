/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateHeadsToChildrenSocialNetwork.java
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
package playground.thibautd.scripts.scenariohandling;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;

/**
 * Takes households, and generates a social network, where the children are alters
 * of the adults of the household.
 * To be used in score internalization.
 * @author thibautd
 */
public class GenerateHeadsToChildrenSocialNetwork {
	private static final int AGE_OF_REASON = 18;

	public static void main(final String[] args) {
		final ArgParser argParser = new ArgParser();
		
		argParser.setDefaultValue( "-h" , null );
		argParser.setDefaultValue( "-p" , null );
		argParser.setDefaultValue( "-o" , null );

		main( argParser.parseArgs( args ) );
	}

	private static void main(final Args argParser) {
		final String householdFile = argParser.getValue( "-h" );
		final String plansFile = argParser.getValue( "-p" );
		final String socialNetworkFile = argParser.getValue( "-o" );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( sc ).readFile( plansFile );

		final Households households = new HouseholdsImpl();
		new HouseholdsReaderV10( households ).readFile( householdFile );

		final SocialNetwork sn = generateNetwork( sc.getPopulation() , households );

		sn.addMetadata(
				"generated with ",
				GenerateHeadsToChildrenSocialNetwork.class.getName() );
		sn.addMetadata(
				"generated on the ",
				new Date().toString() );
		sn.addMetadata(
				"age of reason ",
				""+AGE_OF_REASON );
		sn.addMetadata(
				"households file ",
				householdFile );
		sn.addMetadata(
				"plans file ",
				plansFile );

		new SocialNetworkWriter( sn ).write( socialNetworkFile );
	}

	private static SocialNetwork generateNetwork(
			final Population population,
			final Households households) {
		final SocialNetwork socialNetwork = new SocialNetworkImpl( false );

		for ( Household hh : households.getHouseholds().values() ) {
			final Set<Id> adults = new HashSet<Id>();
			final Set<Id> children = new HashSet<Id>();

			for ( Id id : hh.getMemberIds() ) {
				socialNetwork.addEgo( id );
				final Person p = population.getPersons().get( id );
				if ( PersonUtils.getAge(p) > AGE_OF_REASON ) adults.add( id );
				else children.add( id );
			}

			for ( Id ego : adults ) {
				for ( Id alter : children ) {
					socialNetwork.addMonodirectionalTie( ego , alter );
				}
			}
		}

		return socialNetwork;
	}
}

