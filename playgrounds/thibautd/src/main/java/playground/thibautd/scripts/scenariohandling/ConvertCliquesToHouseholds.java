/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertCliquesToHouseholds.java
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
import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

import org.matsim.contrib.socnetsim.framework.cliques.population.CliquesSchemaNames;

/**
 * @author thibautd
 */
public class ConvertCliquesToHouseholds {
	public static void main(final String[] args) {
		final String inputFile = args[ 0 ];
		final String outputFile = args[ 1 ];
		final double vehiclesPerPerson =
			args.length > 2 ?
				Double.parseDouble( args[ 2 ] ) :
				0.5;

		final ConvertingParser parser = new ConvertingParser();
		parser.parse( inputFile );

		int currentVehicle = 0;
		for ( Household household : parser.households.getHouseholds().values() ) {
			for ( int nCreatedVehicles = 0 ;
					nCreatedVehicles < vehiclesPerPerson * household.getMemberIds().size();
					nCreatedVehicles++ ) {
				if ( household.getVehicleIds() == null ) {
					((HouseholdImpl) household).setVehicleIds( new ArrayList<Id<Vehicle>>() );
				}
				household.getVehicleIds().add( Id.create( "vehicle-"+(currentVehicle++) , Vehicle.class ) );
			}
		}

		new HouseholdsWriterV10( parser.households ).writeFile( outputFile );
	}

	private static class ConvertingParser extends MatsimXmlParser {
		private HouseholdsImpl households = null;
		private Counter counter = null;
		private Id<Household> currentId = null;
		private List<Id<Person>> currentMembers = null;

		public ConvertingParser() {
			super( false );
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ( name.equals( CliquesSchemaNames.CLIQUES ) ) {
				households = new HouseholdsImpl();
				counter = new Counter( "converting clique # " );
			}
			if ( name.equals( CliquesSchemaNames.CLIQUE ) ) {
				counter.incCounter();
				currentId = Id.create( atts.getValue( CliquesSchemaNames.CLIQUE_ID ) , Household.class);
				currentMembers = new ArrayList<Id<Person>>();
			}
			if ( name.equals( CliquesSchemaNames.MEMBER ) ) {
				currentMembers.add( Id.create( atts.getValue( CliquesSchemaNames.MEMBER_ID ) , Person.class ) );
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if ( name.equals( CliquesSchemaNames.CLIQUE ) ) {
				final HouseholdImpl household = new HouseholdImpl( currentId );
				household.setMemberIds( currentMembers );
				households.addHousehold( household );
			}		
			if ( name.equals( CliquesSchemaNames.CLIQUES ) ) {
				counter.printCounter();
			}
		}
	}
}

