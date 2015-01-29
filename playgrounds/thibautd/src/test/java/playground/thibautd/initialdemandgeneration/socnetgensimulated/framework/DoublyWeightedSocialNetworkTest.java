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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

		final DoublyWeightedSocialNetwork testee = new DoublyWeightedSocialNetwork( 2 , 0 , 1 + weights1.size() * weights2.size() );

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

		final Set<Integer> result = testee.getAltersOverWeights( ego , 10 , 50 );
		Assert.assertEquals(
				"unexpected nuber of returned elements: "+result,
				result.size(),
				25 );
	}
}

