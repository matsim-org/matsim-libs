/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedSocialNetworkTest.java
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author thibautd
 */
public class WeightedSocialNetworkTest {

	@Test
	public void testSizeAndGetOverWeight() {
		if ( false ) Logger.getLogger(WeightedSocialNetwork.class).setLevel( Level.TRACE );

		final List<Double> weights =
			Arrays.asList(
					// some lowest than lower bound: should not be added at all
					-1d, -2d, -3d,
					// some between lower bound and desired weight
					1d, 2d, 3d, 4d,
					// over desired weight
					100d, 200d, 300d, 400d, 500d );
		Collections.shuffle( weights );

		final WeightedSocialNetwork testee = new WeightedSocialNetwork( 1 + weights.size() , 0 , 1 + weights.size() );

		final int ego = 0;
		int alter = 1;
		for ( Double w : weights ) {
			testee.addBidirectionalTie( ego , alter++ , w );
		}

		Assert.assertEquals(
				"unexpected size of stored elements",
				9,
				testee.getSize( ego ) );

		final int[] result = testee.getAltersOverWeight( ego , 10 );
		Assert.assertEquals(
				"unexpected nuber of returned elements: "+result,
				5,
				result.length);

		Assert.assertEquals(
				"unexpected nuber of returned elements",
				0,
				testee.getAltersOverWeight( ego , 501 ).length );
	}

	@Test
	public void testLimitedSize() {
		final int maxSize = 10;
		final int popSize = 20;

		final WeightedSocialNetwork testee = new WeightedSocialNetwork( maxSize , Double.NEGATIVE_INFINITY , popSize );

		final int ego = 0;
		for ( int alter=1; alter < popSize; alter++ ) {
			testee.addBidirectionalTie( ego , alter , alter );
		}

		Assert.assertEquals(
				"unexpected size of stored elements",
				10,
				testee.getSize( ego ) );

		for ( int score = popSize - maxSize; score < popSize; score++ ) {
			Assert.assertEquals(
					"unexpected number of elements over "+score,
					popSize - score,
					testee.getAltersOverWeight( ego , score ).length );
		}
	}

	@Test
	public void testReflectivity() {
		final int maxSize = 5;
		final int popSize = 100;

		final WeightedSocialNetwork testee = new WeightedSocialNetwork( maxSize , Double.NEGATIVE_INFINITY , popSize );

		final Random r = new Random( 1234 );
		for ( int ego = 0; ego < popSize; ego++ ) {
			for ( int alter=0; alter < ego; alter++ ) {
				testee.addBidirectionalTie( ego , alter , r.nextDouble() );
			}
		}

		for ( int ego = 0; ego < popSize; ego++ ) {
			for ( int alter = 0; alter < ego; alter++ ) {
				Assert.assertEquals(
						"found non reflective tie!",
						testee.contains( ego , alter ),
						testee.contains( alter , ego ) );
			}
		}
	}
}

