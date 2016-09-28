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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.simplesnowball;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSamplerUtils;

/**
 * @author thibautd
 */
public class RunSimpleCliquesSampling {
	public static void main( String[] args ) {
		final SnowballSamplingConfigGroup configGroup = new SnowballSamplingConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , configGroup );

		final SocialNetwork socialNetwork =
				SocialNetworkSamplerUtils.sampleSocialNetwork(
						config,
						new SimpleSnowballModule(
								SnowballCliques.readCliques(
										configGroup.getInputCliquesCsv() ) ) );

		new SocialNetworkWriter( socialNetwork ).write( configGroup.getOutputSocialNetwork() );
	}
}

