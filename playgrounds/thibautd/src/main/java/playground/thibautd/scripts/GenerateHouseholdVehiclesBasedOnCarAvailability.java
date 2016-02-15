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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
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
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Generates as many vehicles as persons with "always" a car in each household.
 * @author thibautd
 */
public class GenerateHouseholdVehiclesBasedOnCarAvailability {
	private static final Logger log =
		Logger.getLogger(GenerateHouseholdVehiclesBasedOnCarAvailability.class);

	public static void main(final String[] sargs) {
		final ArgParser args = new ArgParser();
		args.addSwitch( "--cliques" ); // to read cliques and not households
		main( args.parseArgs( sargs ) );
	}

	public static void main(final Args args) {
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
						currentHousehold = new HouseholdImpl( Id.create( atts.getValue( "id" ) , Household.class ) );
						currentHousehold.setMemberIds( new ArrayList<Id<Person>>() );
						((HouseholdsImpl) households).addHousehold( currentHousehold );
					}
					if ( name.equals( "person" ) ) {
						currentHousehold.getMemberIds().add( Id.create( atts.getValue( "id" ) , Person.class ) );
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
			((HouseholdImpl) hh).setVehicleIds( new ArrayList<Id<Vehicle>>( hh.getMemberIds().size() ) );
			for ( Id pers : hh.getMemberIds() ) {
				person2hh.put( pers , hh );
			}
		}

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final PopulationImpl pop = (PopulationImpl) sc.getPopulation();

		log.info( "parse persons" );
		final Set<Id> hhsWithSometimes = new HashSet<Id>();
		pop.setIsStreaming( true );
		pop.addAlgorithm( new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				final Household hh = person2hh.get( person.getId() );
				if ( "always".equals( PersonUtils.getCarAvail(person) ) ) {
					((HouseholdImpl) hh).getVehicleIds().add( Id.create(person.getId().toString(), Vehicle.class) );
				}

				if ( "sometimes".equals( PersonUtils.getCarAvail(person) ) ) {
					hhsWithSometimes.add( hh.getId() );
				}
			}
		});

		new MatsimPopulationReader( sc ).readFile( inpop );

		log.info( "correction: do not let households with only \"sometimes\" without a car" );
		int c=0;
		for ( Household hh : households.getHouseholds().values() ) {
			if ( hhsWithSometimes.contains( hh.getId() ) && hh.getVehicleIds().isEmpty() ) {
				c++;
				hh.getVehicleIds().add( Id.create ( "corr-"+hh.getId() , Vehicle.class ) );
			}
		}
		log.info( "created "+c+" correction vehicles" );

		log.info( "dump households to "+outhh );
		new HouseholdsWriterV10( households ).writeFile( outhh );
	}
}

