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
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author thibautd
 */
public class WeightedSocialNetworkTest {

	@Test
	public void testSizeAndGetOverWeight() {
		if ( false ) Logger.getLogger(WeightedSocialNetwork.class).setLevel( Level.TRACE );
		final WeightedSocialNetwork testee = new WeightedSocialNetwork( 2 , 0 );

		final List<Double> weights =
			Arrays.asList(
					// some lowest than lower bound: should not be added at all
					-1d, -2d, -3d,
					// some between lower bound and desired weight
					1d, 2d, 3d, 4d,
					// over desired weight
					100d, 200d, 300d, 400d, 500d );
		Collections.shuffle( weights );

		final Id<Person> ego = Id.createPersonId( "ego" );
		testee.addEgo( ego );
		for ( Double w : weights ) {
			final Id<Person> alter = Id.createPersonId( ""+w );
			testee.addEgo( alter );
			testee.addBidirectionalTie( ego , alter , w );
		}

		Assert.assertEquals(
				"unexpected size of stored elements",
				9,
				testee.getSize( ego ) );

		final Set<Id<Person>> result = testee.getAltersOverWeight( ego , 10 );
		Assert.assertEquals(
				"unexpected nuber of returned elements: "+result,
				result.size(),
				5 );
	}
}

