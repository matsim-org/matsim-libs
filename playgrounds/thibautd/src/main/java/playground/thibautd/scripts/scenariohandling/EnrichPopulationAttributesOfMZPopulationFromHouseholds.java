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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.thibautd.utils.CsvParser;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class EnrichPopulationAttributesOfMZPopulationFromHouseholds {
	public static void main( final String... args ) {
		final String inputPopulationFile = args[ 0 ];
		final String inputPopulationAttributes = args[ 1 ];
		final String inputHousholdDatFile = args[ 2 ];
		final String outputPopulationAttributes = args[ 3 ];

		final ObjectAttributes attributes = new ObjectAttributes();
		new ObjectAttributesXmlReader( attributes ).parse( inputPopulationAttributes );

		final Map<String, Collection<Id<Person>>> personsInHousehold = new HashMap<>();
		for ( Person p : readPopulation( inputPopulationFile ) ) {
			final String hh = (String) attributes.getAttribute( p.getId().toString() , "household number" );
			MapUtils.getCollection( hh , personsInHousehold ).add( p.getId() );
		}

		try ( final CsvParser parser = new CsvParser( '\t' , '\"' , inputHousholdDatFile ) ) {
			while ( parser.nextLine() ) {
				final String hhNumber = parser.getField( "HHNR" );
				final String size = parser.getField( "hhgr" );
				final String income = parser.getField( "F20601" );

				if ( !personsInHousehold.containsKey( hhNumber ) ) continue;
				for ( Id<Person> member : personsInHousehold.get( hhNumber ) ) {
					attributes.putAttribute(
							member.toString(),
							"householdSize",
							Integer.valueOf( size ) );

					attributes.putAttribute(
							member.toString(),
							"householdIncome",
							incomeClass( income ) );
				}
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}

		new ObjectAttributesXmlWriter( attributes ).writeFile( outputPopulationAttributes );
	}

	private static String incomeClass( String income ) {
		switch ( income ) {
			case "-98":	return "no Answer";
			case "-97":	return "do not know";
			case "1":	return "less than CHF 2000";
			case "2":	return "CHF 2000 to 4000";
			case "3":	return "CHF 4001 to 6000";
			case "4":	return "CHF 6001 to 8000";
			case "5":	return "CHF 8001 to 10000";
			case "6":	return "CHF 10001 to 12000";
			case "7":	return "CHF 12001 to 14000";
			case "8":	return "CHF 14001 to 16000";
			case "9":	return "greater than CHF 16000";
			default: throw new IllegalArgumentException( income );
		}
	}

	private static Iterable<? extends Person> readPopulation( final String inputPopulationFile ) {
		final Scenario s = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( s ).readFile( inputPopulationFile );
		return s.getPopulation().getPersons().values();
	}
}

