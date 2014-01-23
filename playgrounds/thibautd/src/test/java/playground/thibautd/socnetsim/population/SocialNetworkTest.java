/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkTest.java
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
package playground.thibautd.socnetsim.population;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author thibautd
 */
public class SocialNetworkTest {
	@Test( expected=IllegalStateException.class )
	public void testFailsIfAddingDirectedTieInReflectiveNetwork() {
		final SocialNetwork sn = new SocialNetwork( true );
		sn.addMonodirectionalTie( new IdImpl( 1 ) , new IdImpl( 2 ) );
	}

	@Test
	public void testMonodirectionalTie() {
		final Id ego = new IdImpl( 1 );
		final Id alter = new IdImpl( 2 );

		final SocialNetwork sn = new SocialNetwork( false );
		sn.addEgo( ego );
		sn.addEgo( alter );
		sn.addMonodirectionalTie( ego , alter );

		Assert.assertEquals(
				"unexpected egos",
				new HashSet<Id>( Arrays.asList( ego , alter ) ),
				sn.getEgos() );

		Assert.assertEquals(
				"unexpected alters of ego",
				Collections.singleton( alter ),
				sn.getAlters( ego ) );
		
		Assert.assertEquals(
				"unexpected alters of alter",
				Collections.<Id>emptySet(),
				sn.getAlters( alter ) );
	}

	@Test
	public void testBidirectionalTie() {
		final Id ego = new IdImpl( 1 );
		final Id alter = new IdImpl( 2 );

		final SocialNetwork sn = new SocialNetwork( false );
		sn.addEgo( ego );
		sn.addEgo( alter );
		sn.addBidirectionalTie( ego , alter );

		Assert.assertEquals(
				"unexpected egos",
				new HashSet<Id>( Arrays.asList( ego , alter ) ),
				sn.getEgos() );

		Assert.assertEquals(
				"unexpected alters of ego",
				Collections.singleton( alter ),
				sn.getAlters( ego ) );

		Assert.assertEquals(
				"unexpected alters of alter",
				Collections.singleton( ego ),
				sn.getAlters( alter ) );
	}
}

