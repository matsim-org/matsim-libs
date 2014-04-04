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
		final SocialNetwork sn = new SocialNetworkImpl( true );
		sn.addMonodirectionalTie( new IdImpl( 1 ) , new IdImpl( 2 ) );
	}

	@Test
	public void testMonodirectionalTie() {
		final Id ego = new IdImpl( 1 );
		final Id alter = new IdImpl( 2 );

		final SocialNetwork sn = new SocialNetworkImpl( false );
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

		final SocialNetwork sn = new SocialNetworkImpl( false );
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

	@Test
	public void testRemoveEgo() {
		final SocialNetworkImpl sn = new SocialNetworkImpl( true );

		final Id ego = new IdImpl( "ego" );
		final Id a1 = new IdImpl( "alter1" );
		final Id a2 = new IdImpl( "alter2" );
		final Id a3 = new IdImpl( "alter3" );
		final Id a4 = new IdImpl( "alter4" );
		final Id a5 = new IdImpl( "alter5" );
		final Id a6 = new IdImpl( "alter6" );

		final Id a7 = new IdImpl( "alter7" );
		final Id a8 = new IdImpl( "alter8" );
		final Id a9 = new IdImpl( "alter9" );
		final Id a10 = new IdImpl( "alter10" );

		sn.addEgo( ego );
		sn.addEgo( a1 );
		sn.addEgo( a2 );
		sn.addEgo( a3 );
		sn.addEgo( a4 );
		sn.addEgo( a5 );
		sn.addEgo( a6 );
		sn.addEgo( a7 );
		sn.addEgo( a8 );
		sn.addEgo( a9 );
		sn.addEgo( a10 );

		sn.addBidirectionalTie( ego , a1 );
		sn.addBidirectionalTie( ego , a2 );
		sn.addBidirectionalTie( ego , a3 );
		sn.addBidirectionalTie( ego , a4 );
		sn.addBidirectionalTie( ego , a5 );
		sn.addBidirectionalTie( ego , a6 );

		// add some agents not directly connected to ego 
		for ( Id external : new Id[]{ a7 , a8 , a9 , a10 } ) {
			sn.addBidirectionalTie( a6 , external );
		}

		sn.removeEgo( ego );
		Assert.assertFalse(
				"ego still in social network",
				sn.getEgos().contains( ego ) );

		for ( Id alter : new Id[]{ a1 , a2 , a3 , a4 , a5 , a6 , a7 , a8 , a9 , a10 } ) {
			Assert.assertFalse(
					"ego still in network of agent "+alter,
					sn.getAlters( alter ).contains( ego ) );
		}

		for ( Id alter : new Id[]{ a1 , a2 , a3 , a4 , a5 } ) {
			Assert.assertEquals(
					"wrong network size for "+alter,
					0,
					sn.getAlters( alter ).size() );
		}

		for ( Id alter : new Id[]{ a7 , a8 , a9 , a10 } ) {
			Assert.assertEquals(
					"wrong network size for "+alter,
					1,
					sn.getAlters( alter ).size() );
		}

		Assert.assertEquals(
				"wrong network size for "+a6,
				4,
				sn.getAlters( a6 ).size() );

	}
}

