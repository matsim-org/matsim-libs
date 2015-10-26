/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedSocialNetworkIOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.Random;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class WeightedSocialNetworkIOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void setupLogging() {
		if ( false ) Logger.getLogger( WeightedSocialNetwork.class ).setLevel( Level.TRACE );
	}

	@Test
	public void testStableThroughIO() {
		final WeightedSocialNetwork testNetwork = createRandomNetwork();

		final String filePath = utils.getOutputDirectory() + "/weighted_network.xml";

		new WeightedSocialNetworkWriter().write( testNetwork, filePath );

		final WeightedSocialNetwork readNetwork = new WeightedSocialNetworkReader().read( filePath );

		Assert.assertEquals(
				"unexpected size",
				testNetwork.getNEgos(),
				readNetwork.getNEgos() );

		Assert.assertEquals(
				"unexpected maximal size",
				testNetwork.getMaximalSize(),
				readNetwork.getMaximalSize() );

		Assert.assertEquals(
				"unexpected lowest stored weight",
				testNetwork.getLowestAllowedWeight(),
				readNetwork.getLowestAllowedWeight(),
				MatsimTestUtils.EPSILON );

		for ( int i = 0; i < testNetwork.getNEgos(); i++ ) {
			Assert.assertEquals(
					"unexpected alters for ego "+i,
					testNetwork.getAlters( i ),
					readNetwork.getAlters( i ) );
		}
	}

	private WeightedSocialNetwork createRandomNetwork() {
		final WeightedSocialNetwork testNetwork = new WeightedSocialNetwork( 4 , 0.5 , 100 );

		final Random r = new Random( 4235 );
		for ( int i=1; i < 100; i++) {
			for ( int j=0; j < i; j++ ) {
				testNetwork.addBidirectionalTie( i , j , r.nextDouble() );
			}
		}
		return testNetwork;
	}
}

