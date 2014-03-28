/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateHouseholdVehiclesBasedOnCarAvailability.java
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
package playground.thibautd.scripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.xml.sax.Attributes;

import playground.thibautd.utils.ArgParser;

/**
 * Generates as many vehicles as persons with "always" a car in each household.
 * @author thibautd
 */
public class GenerateHouseholdVehiclesBasedOnCarAvailability {
	private static final Logger log =
		Logger.getLogger(GenerateHouseholdVehiclesBasedOnCarAvailability.class);

	public static void main(final String[] args) {
		main( new ArgParser( args ) );
	}

	public static void main(final ArgParser args) {
		args.addSwitch( "--cliques" ); // to read cliques and not households
		final String inhh = args.getNonSwitchedArgs()[ 0 ];
		final String inpop = args.getNonSwitchedArgs()[ 1 ];
		final String outhh = args.getNonSwitchedArgs()[ 2 ];

		log.info( "read households from "+inhh );
		final Households households = new HouseholdsImpl();

		if ( args.isSwitched( "--cliques" ) ) {
			log.info( "read households using cliques reader" );

			new MatsimXmlParser( false ) {
				HouseholdImpl currentHousehold = null;
				@Override
				public void startTag(
						final String name,
						final Attributes atts,
						final Stack<String> context) {
					if ( name.equals( "clique" ) ) {
						currentHousehold = new HouseholdImpl( new IdImpl( atts.getValue( "id" ) ) );
						currentHousehold.setMemberIds( new ArrayList<Id>() );
						((HouseholdsImpl) households).addHousehold( currentHousehold );
					}
					if ( name.equals( "person" ) ) {
						currentHousehold.getMemberIds().add( new IdImpl( atts.getValue( "id" ) ) );
					}
				}

				@Override
				public void endTag(
						final String name,
						final String content,
						final Stack<String> context) {}
			}.parse( inhh );
		}
		else {
			log.info( "read households using households reader" );
			new HouseholdsReaderV10( households ).readFile( inhh );
		}

		log.info( "map persons to households" );
		final Map<Id, Household> person2hh = new HashMap<Id, Household>();
		for ( Household hh : households.getHouseholds().values() ) {
			((HouseholdImpl) hh).setVehicleIds( new ArrayList<Id>( hh.getMemberIds().size() ) );
			for ( Id pers : hh.getMemberIds() ) {
				person2hh.put( pers , hh );
			}
		}

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final PopulationImpl pop = (PopulationImpl) sc.getPopulation();

		log.info( "parse persons" );
		pop.setIsStreaming( true );
		pop.addAlgorithm( new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				if ( "always".equals( ((PersonImpl) person).getCarAvail() ) ) {
					final Household hh = person2hh.get( person.getId() );
					((HouseholdImpl) hh).getVehicleIds().add( person.getId() );
				}
			}
		});

		new MatsimPopulationReader( sc ).readFile( inpop );

		log.info( "dump households to "+outhh );
		new HouseholdsWriterV10( households ).writeFile( outhh );
	}
}

