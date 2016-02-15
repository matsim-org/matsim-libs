/* *********************************************************************** *
 * project: org.matsim.*
 * ComputeClustering.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;

/**
 * @author thibautd
 */
public class ComputeClustering {
	private static final Logger log =
		Logger.getLogger(ComputeClustering.class);

	public static void main(final String[] args) {
		final String socialNetworkFile = args[ 0 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).parse( socialNetworkFile );
	
		final SocialNetwork socialNetwork = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		log.info( "clustering: "+SnaUtils.calcClusteringCoefficient( socialNetwork ) );
	}
}

