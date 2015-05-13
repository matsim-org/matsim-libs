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
package playground.thibautd.socnetsim.jointtrips.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetworkImpl;
import playground.thibautd.socnetsim.framework.population.SocialNetworkReader;
import playground.thibautd.socnetsim.framework.population.SocialNetworkWriter;

/**
 * @author thibautd
 */
public class SocialNetworkIOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReinputReflective() {
		testReinput( true );
	}

	@Test
	public void testReinputNonReflective() {
		testReinput( false );
	}

	private void testReinput(final boolean isReflective) {
		final SocialNetwork output = generateRandomSocialNetwork( isReflective );
		final String path = utils.getOutputDirectory()+"/sn.xml";

		new SocialNetworkWriter( output ).write( path );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).parse( path );

		final SocialNetwork input = (SocialNetwork)
			sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );

		Assert.assertEquals(
				"unexpected reflectiveness",
				output.isReflective(),
				input.isReflective() );

		Assert.assertEquals(
				"unexpected number of egos",
				output.getEgos().size(),
				input.getEgos().size() );

		Assert.assertEquals(
				"different ego ids",
				output.getEgos(),
				input.getEgos() );

		final Counter c = new Counter( "Test alters of ego # " );
		for ( Id ego : output.getEgos() ) {
			c.incCounter();
			final Set<Id<Person>> expectedAlters = output.getAlters( ego );
			final Set<Id<Person>> actualAlters = input.getAlters( ego );

			Assert.assertEquals(
					"unexpected number of alters for ego "+ego,
					expectedAlters.size(),
					actualAlters.size() );

			Assert.assertEquals(
					"unexpected alters for ego "+ego,
					expectedAlters,
					actualAlters );
		}

		Assert.assertEquals(
				"different metadata",
				output.getMetadata(),
				input.getMetadata() );

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

