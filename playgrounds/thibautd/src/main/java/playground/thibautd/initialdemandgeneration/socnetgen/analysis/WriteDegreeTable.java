/* *********************************************************************** *
 * project: org.matsim.*
 * WriteDegreeTable.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;

/**
 * @author thibautd
 */
public class WriteDegreeTable {
	public static void main(final String[] args) {
		final String socialNetworkFile = args[ 0 ];
		final String outputRawFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).parse( socialNetworkFile );
	
		final SocialNetwork socialNetwork = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		writeDegreeTable( outputRawFile, socialNetwork );
	}

	public static void writeDegreeTable(
			final String outputRawFile,
			final SocialNetwork socialNetwork ) {
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputRawFile ) ) {

			writer.write( "egoId\tnetSize" );
			for ( Id ego : socialNetwork.getEgos() ) {
				writer.newLine();
				writer.write( ego + "\t" + socialNetwork.getAlters( ego ).size() );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}

