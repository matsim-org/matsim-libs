/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateRandomSocialNetwork.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Date;
import java.util.Random;

/**
 * @author thibautd
 */
public class GenerateRandomSocialNetwork {
	public static void main(final String[] args) {
		final int avgEgocentricNetworkSize = Integer.parseInt( args[ 0 ] );
		final String inputPopulationFile = args[ 1 ];
		final String outputSocialNetworkFile = args[ 2 ];

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( scenario ).readFile( inputPopulationFile );

		final Id[] ids = getIds( scenario );

		final double prob = ((double) avgEgocentricNetworkSize) / ids.length;
		if ( prob <= 0 || prob > 1 ) throw new RuntimeException( "prob="+prob );

		final SocialNetwork sn = new SocialNetworkImpl( true );
		sn.addMetadata( "generated with", "playground.thibautd.scripts.GenerateRandomSocialNetwork" );
		sn.addMetadata( "avg egocentric network size", ""+avgEgocentricNetworkSize );
		sn.addMetadata( "input population file", inputPopulationFile );
		sn.addMetadata( "date", new Date().toString() );

		for ( int i=0; i < ids.length; i++ ) {
			sn.addEgo( ids[ i ] );		
		}
		final Random random = new Random( 1234 );
		for ( int i=0; i < ids.length; i++ ) {
			final Id ego = ids[ i ];

			for ( int j=i+1; j < ids.length; j++ ) {
				if ( random.nextDouble() < prob ) {
					sn.addBidirectionalTie( ego , ids[ j ] );
				}
			}
		}

		new SocialNetworkWriter( sn ).write( outputSocialNetworkFile );
	}

	private static Id[] getIds(final Scenario scenario) {
		return scenario.getPopulation().getPersons().keySet().toArray( 
				new Id[ scenario.getPopulation().getPersons().size() ] );
	}
}

