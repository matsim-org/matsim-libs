/* *********************************************************************** *
 * project: org.matsim.*
 * ComputeSocialDistanceBetweenRandomIndividuals.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

/**
 * @author thibautd
 */
public class ComputeSocialDistanceBetweenRandomIndividuals {
	public static void main(final String[] args) {
		final String inputSocialNetwork = args[ 0 ];
		final String outputDataFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).readFile( inputSocialNetwork );
	
		final SocialNetwork socialNetwork = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		writeRandomDistances( outputDataFile, socialNetwork, 10000 );
	}

	public static void writeRandomDistances(
			final String outputDataFile,
			final SocialNetwork socialNetwork,
			final int nPairs ) {
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputDataFile ) ) {
			writer.write( "ego\talter\tdistance" );

			SnaUtils.sampleSocialDistances(
					socialNetwork,
					new Random( 123 ),
					nPairs,
					(ego, alter, distance) -> {
						try {
							writer.newLine();
							writer.write( ego + "\t" + alter + "\t" + distance );
						} catch ( IOException e ) {
							throw new UncheckedIOException( e );
						}
					} );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}

