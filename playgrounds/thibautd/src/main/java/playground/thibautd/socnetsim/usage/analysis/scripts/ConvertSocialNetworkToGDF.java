/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertSocialNetworkToGDF.java
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
package playground.thibautd.socnetsim.usage.analysis.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.thibautd.initialdemandgeneration.socnetgen.analysis.ConvertSocialNetworkToLocatedMatsimNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetworkReader;

/**
 * @author thibautd
 */
public class ConvertSocialNetworkToGDF {
	public static void main(final String[] args) throws IOException {
		final String socNetFile = args[ 0 ];
		final String popFile = args[ 1 ];
		final String outNetwork = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final Map<Id<Person>, Coord> coords = ConvertSocialNetworkToLocatedMatsimNetwork.parsePopulation( popFile );
		new SocialNetworkReader( sc ).parse( socNetFile );

		final BufferedWriter writer = IOUtils.getBufferedWriter( outNetwork );
		writeEgos(
				(SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME ),
				coords,
				writer );
		writeTies( (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME ) , writer );
		writer.close();
	}

	private static void writeEgos(
			final SocialNetwork sn,
			final Map<Id<Person>, Coord> coords,
			final BufferedWriter writer) throws IOException {
		writer.write( "nodedef>name VARCHAR,x DOUBLE,y DOUBLE" );
		for ( Id<Person> ego : sn.getEgos() ) {
			final Coord c = coords.get( ego );
			writer.newLine();
			writer.write( ego+","+c.getX()+","+c.getY() );
		}
	}

	private static void writeTies(
			final SocialNetwork sn,
			final BufferedWriter writer) throws IOException {
		writer.newLine();
		writer.write( "edgedef>node1 VARCHAR,node2 VARCHAR" );
		for ( Map.Entry<Id<Person>, Set<Id<Person>>> e : sn.getMapRepresentation().entrySet() ) {
			for ( Id alter : e.getValue() ) {
				writer.newLine();
				writer.write( e.getKey()+","+alter );
			}
		}
	
	}
}

