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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author thibautd
 */
public class GenerateMultimodalNetworkWithSlopes {
	private static final Set<String> MODES =
			new HashSet<>(Arrays.asList(
					TransportMode.car,
					TransportMode.walk,
					TransportMode.bike,
					TransportMode.pt
			));

	public static void main(final String[] args) {
		final String inputNetwork = args[ 0 ];
		final String outputNetwork = args[ 1 ];
		final String outputSlopes = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader( sc ).readFile(inputNetwork);

		final Random random = new Random( 1234 );
		final ObjectAttributes slopes = new ObjectAttributes();

		for ( Link l : sc.getNetwork().getLinks().values() ) {
			l.setAllowedModes( MODES );
			slopes.putAttribute( l.getId().toString() , "slope" , -50d + random.nextDouble() * 100 );
		}

		new NetworkWriter( sc.getNetwork() ).write( outputNetwork );
		new ObjectAttributesXmlWriter( slopes ).writeFile( outputSlopes );
	}
}
