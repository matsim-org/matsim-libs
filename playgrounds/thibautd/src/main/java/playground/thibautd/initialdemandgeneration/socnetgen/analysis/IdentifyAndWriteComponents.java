/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifyAndWriteComponents.java
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
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;

/**
 * @author thibautd
 */
public class IdentifyAndWriteComponents {
	private static final Logger log =
		Logger.getLogger(IdentifyAndWriteComponents.class);

	public static void main(final String[] args) {
		final String inputSocialNetwork = args[ 0 ];
		final String outputRawFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).parse( inputSocialNetwork );
	
		final SocialNetwork sn = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );

		final Collection<Set<Id>> components = SnaUtils.identifyConnectedComponents( sn );

		log.info( components.size()+" components" );

		writeComponents( outputRawFile, components );
	}

	public static void writeComponents( final String outputRawFile , final Collection<Set<Id>> components ) {
		try (final BufferedWriter writer = IOUtils.getBufferedWriter( outputRawFile )) {
			writer.write( "agentId\tcomponentId\tcomponentSize" );

			int i = 0;
			for ( Set<Id> component : components ) {
				final int compNr = i++;

				final int size = component.size();

				for ( Id id : component ) {
					writer.newLine();
					writer.write( id+"\t"+compNr+"\t"+size );
				}
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}

