/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkIOTest.java
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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class SocialNetworkIOTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReinputReflective() {
		testReinput( true );
	}

	@Test
	void testReinputNonReflective() {
		testReinput( false );
	}

	private void testReinput(final boolean isReflective) {
		final SocialNetwork output = generateRandomSocialNetwork( isReflective );
		final String path = utils.getOutputDirectory()+"/sn.xml";

		new SocialNetworkWriter( output ).write( path );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).readFile( path );

		final SocialNetwork input = (SocialNetwork)
			sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );

		Assertions.assertEquals(
				output.isReflective(),
				input.isReflective(),
				"unexpected reflectiveness" );

		Assertions.assertEquals(
				output.getEgos().size(),
				input.getEgos().size(),
				"unexpected number of egos" );

		Assertions.assertEquals(
				output.getEgos(),
				input.getEgos(),
				"different ego ids" );

		final Counter c = new Counter( "Test alters of ego # " );
		for ( Id ego : output.getEgos() ) {
			c.incCounter();
			final Set<Id<Person>> expectedAlters = output.getAlters( ego );
			final Set<Id<Person>> actualAlters = input.getAlters( ego );

			Assertions.assertEquals(
					expectedAlters.size(),
					actualAlters.size(),
					"unexpected number of alters for ego "+ego );

			Assertions.assertEquals(
					expectedAlters,
					actualAlters,
					"unexpected alters for ego "+ego );
		}

		Assertions.assertEquals(
				output.getMetadata(),
				input.getMetadata(),
				"different metadata" );

		c.printCounter();
	}

	private SocialNetwork generateRandomSocialNetwork(final boolean isReflective) {
		final SocialNetwork sn = new SocialNetworkImpl( isReflective );

		final int nEgos = 500;
		final List<Id> ids = new ArrayList<Id>( nEgos );

		for ( int i=0; i < nEgos; i++ ) {
			final Id<Person> id = Id.create( i , Person.class );
			sn.addEgo( id );
			ids.add( id );
		}

		final Random random = new Random( 20140114 );
		for ( Id ego : ids ) {
			final int nAlters = random.nextInt( nEgos );
			final List<Id> remainingPossibleAlters = new ArrayList<Id>( ids );
			remainingPossibleAlters.remove( ego );

			for ( int i=0; i < nAlters; i++ ) {
				final Id alter =
					remainingPossibleAlters.remove(
							random.nextInt(
								remainingPossibleAlters.size() ) );
				if ( isReflective ) sn.addBidirectionalTie( ego , alter );
				else sn.addMonodirectionalTie( ego , alter );
			}
		}

		sn.addMetadata( "some attribute" , "some value" );
		sn.addMetadata( "some other attribute" , "some other value" );
		return sn;
	}
}

