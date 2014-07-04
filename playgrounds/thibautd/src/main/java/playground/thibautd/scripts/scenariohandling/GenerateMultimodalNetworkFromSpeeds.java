/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateMultimodalNetworkFromSpeeds.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class GenerateMultimodalNetworkFromSpeeds {
	public static void main(final String[] args) {
		final String inputNetwork = args[ 0 ];
		final String outputNetwork = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).readFile( inputNetwork );

		final MultiModalConfigGroup conf = new MultiModalConfigGroup();
		conf.setCreateMultiModalNetwork( true );
		new MultiModalNetworkCreator( conf ).run( sc.getNetwork() );

		new NetworkWriter( sc.getNetwork() ).write( outputNetwork );
	}
}

