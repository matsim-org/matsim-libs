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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author thibautd
 */
public class SocialNetworkTest {
	@Test
	void testFailsIfAddingDirectedTieInReflectiveNetwork() {
		assertThrows(IllegalStateException.class, () -> {
			final SocialNetwork sn = new SocialNetworkImpl( true );
			sn.addMonodirectionalTie(Id.create(1, Person.class), Id.create(2, Person.class));
		});
	}

	@Test
	void testMonodirectionalTie() {
		final Id<Person> ego = Id.create( 1 , Person.class );
		final Id<Person> alter = Id.create( 2 , Person.class );

		final SocialNetwork sn = new SocialNetworkImpl( false );
		sn.addEgo( ego );
		sn.addEgo( alter );
		sn.addMonodirectionalTie( ego , alter );

		Assertions.assertEquals(
				new HashSet<Id<Person>>( Arrays.asList( ego , alter ) ),
				sn.getEgos(),
				"unexpected egos" );

		Assertions.assertEquals(
				Collections.singleton( alter ),
				sn.getAlters( ego ),
				"unexpected alters of ego" );
		
		Assertions.assertEquals(
				Collections.<Id<Person>>emptySet(),
				sn.getAlters( alter ),
				"unexpected alters of alter" );
	}

	@Test
	void testBidirectionalTie() {
		final Id<Person> ego = Id.create( 1 , Person.class );
		final Id<Person> alter = Id.create( 2 , Person.class );

		final SocialNetwork sn = new SocialNetworkImpl( false );
		sn.addEgo( ego );
		sn.addEgo( alter );
		sn.addBidirectionalTie( ego , alter );

		Assertions.assertEquals(
				new HashSet<Id<Person>>( Arrays.asList( ego , alter ) ),
				sn.getEgos(),
				"unexpected egos" );

		Assertions.assertEquals(
				Collections.singleton( alter ),
				sn.getAlters( ego ),
				"unexpected alters of ego" );

		Assertions.assertEquals(
				Collections.singleton( ego ),
				sn.getAlters( alter ),
				"unexpected alters of alter" );
	}

	@Test
	void testRemoveEgo() {
		final SocialNetworkImpl sn = new SocialNetworkImpl( true );

		final Id<Person> ego = Id.create( "ego" , Person.class );
		final Id<Person> a1 = Id.create( "alter1" , Person.class );
		final Id<Person> a2 = Id.create( "alter2" , Person.class );
		final Id<Person> a3 = Id.create( "alter3" , Person.class );
		final Id<Person> a4 = Id.create( "alter4" , Person.class );
		final Id<Person> a5 = Id.create( "alter5" , Person.class );
		final Id<Person> a6 = Id.create( "alter6" , Person.class );

		final Id<Person> a7 = Id.create( "alter7" , Person.class );
		final Id<Person> a8 = Id.create( "alter8" , Person.class );
		final Id<Person> a9 = Id.create( "alter9" , Person.class );
		final Id<Person> a10 = Id.create( "alter10" , Person.class);

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
		for ( Id<Person> external : new Id[]{ a7 , a8 , a9 , a10 } ) {
			sn.addBidirectionalTie( a6 , external );
		}

		sn.removeEgo( ego );
		Assertions.assertFalse(
				sn.getEgos().contains( ego ),
				"ego still in social network" );

		for ( Id<Person> alter : new Id[]{ a1 , a2 , a3 , a4 , a5 , a6 , a7 , a8 , a9 , a10 } ) {
			Assertions.assertFalse(
					sn.getAlters( alter ).contains( ego ),
					"ego still in network of agent "+alter );
		}

		for ( Id<Person> alter : new Id[]{ a1 , a2 , a3 , a4 , a5 } ) {
			Assertions.assertEquals(
					0,
					sn.getAlters( alter ).size(),
					"wrong network size for "+alter );
		}

		for ( Id<Person> alter : new Id[]{ a7 , a8 , a9 , a10 } ) {
			Assertions.assertEquals(
					1,
					sn.getAlters( alter ).size(),
					"wrong network size for "+alter );
		}

		Assertions.assertEquals(
				4,
				sn.getAlters( a6 ).size(),
				"wrong network size for "+a6 );

	}
}

