/* *********************************************************************** *
 * project: org.matsim.*
 * WriteReflectiveSocialNetworkAsNonReflective.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetworkImpl;
import playground.thibautd.socnetsim.framework.population.SocialNetworkReader;
import playground.thibautd.socnetsim.framework.population.SocialNetworkWriter;

/**
 * @author thibautd
 */
public class WriteReflectiveSocialNetworkAsNonReflective {
	public static void main(final String[] args) {
		final String in = args[ 0 ];
		final String out = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		new SocialNetworkReader( sc ).parse( in );
		final SocialNetwork inSocNet = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );

		final SocialNetwork outSocNet = new SocialNetworkImpl( false );

		for ( Id<Person> ego : inSocNet.getEgos() ) {
			outSocNet.addEgo( ego );
			for ( Id<Person> alter : inSocNet.getAlters( ego ) ) {
				outSocNet.addMonodirectionalTie( ego , alter );
			}
		}

		new SocialNetworkWriter( outSocNet ).write( out );
	}
}

