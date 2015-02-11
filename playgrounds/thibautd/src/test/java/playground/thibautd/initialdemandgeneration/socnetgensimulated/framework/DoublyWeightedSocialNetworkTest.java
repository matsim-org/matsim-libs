/* *********************************************************************** *
 * project: org.matsim.*
 * DoublyWeightedSocialNetworkTest.java
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

import gnu.trove.set.TIntSet;

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
public class DoublyWeightedSocialNetworkTest {
	@Test
	public void testSizeAndGetOverWeight() {
		if ( false ) Logger.getLogger(DoublyWeightedSocialNetwork.class).setLevel( Level.TRACE );

		final List<Double> weights1 =
			Arrays.asList(
					// some lowest than lower bound: should not be added at all
					-1d, -2d, -3d,
					// some between lower bound and desired weight
					1d, 2d, 3d, 4d,
					// over desired weight
					100d, 200d, 300d, 400d, 500d );
		Collections.shuffle( weights1 );

		final List<Double> weights2 =
			Arrays.asList(
					// some lowest than lower bound: should not be added at all
					-1d, -2d, -3d,
					// some between lower bound and desired weight
					10d, 20d, 30d, 40d,
					// over desired weight
					100d, 200d, 300d, 400d, 500d );
		Collections.shuffle( weights2 );

		final DoublyWeightedSocialNetwork testee = new DoublyWeightedSocialNetwork( 2 , 0 , 1 + weights1.size() * weights2.size() , Short.MAX_VALUE );

		final int ego = 0;
		int alter = 1;
		for ( Double w1 : weights1 ) {
			for ( Double w2 : weights2 ) {
				testee.addBidirectionalTie( ego , alter++ , w1 , w2 );
			}
		}

		Assert.assertEquals(
				"unexpected size of stored elements",
				9 * 9,
				testee.getSize( ego ) );

		final TIntSet result = testee.getAltersOverWeights( ego , 10 , 50 );
		Assert.assertEquals(
				"unexpected nuber of returned elements: "+result,
				result.size(),
				25 );
	}

	@Test
	public void testMaximumCapacity() {
		if ( false ) Logger.getLogger(DoublyWeightedSocialNetwork.class).setLevel( Level.TRACE );

		// get a population size much greater than the max size to need a lot
		// of replacements, but get a maximum size big enough to have a somehow
		// "complex" tree, increasing the chances to fail if something is wrong
		final int popSize = 500_000;
		final int maxSize = 500;
		final DoublyWeightedSocialNetwork testee =
			new DoublyWeightedSocialNetwork(
					2 ,
					Double.NEGATIVE_INFINITY ,
					popSize ,
					maxSize );

		final int ego = 0;
		final Random random = new Random( 1234 );
		for ( int alter=1; alter < popSize; alter++ ) {
			testee.addBidirectionalTie( ego , alter , random.nextFloat() , random.nextFloat() );
			
			Assert.assertEquals(
					"number of elements connected to the root differs from size!",
					testee.getSize( ego ),
					testee.getTreeSize( ego ) );
		}

		Assert.assertEquals(
				"unexpected number of alters",
				maxSize,
				testee.getAltersOverWeights( ego , Double.NEGATIVE_INFINITY , Double.NEGATIVE_INFINITY ).size() );

		Assert.assertEquals(
				"invalid size",
				testee.getSize( ego ),
				testee.getAltersOverWeights( ego , Double.NEGATIVE_INFINITY , Double.NEGATIVE_INFINITY ).size() );

	}
}

