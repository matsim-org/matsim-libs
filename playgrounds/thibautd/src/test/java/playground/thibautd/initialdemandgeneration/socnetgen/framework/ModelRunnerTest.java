/* *********************************************************************** *
 * project: org.matsim.*
 * ModelRunnerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author thibautd
 */
public class ModelRunnerTest {
	// ///////////////////////////////////////////////////////////////////////
	// secondary tie candidates
	// ///////////////////////////////////////////////////////////////////////
	@Test
	public void testGetUnknownFriendsOfFriendsReturnsAllExpected() throws Exception {
		final TestSocialNetwork network = createTestNetwork();

		final Map<Id, Agent> allAgents = new HashMap<Id, Agent>();
		for ( Id ego : network.socialNetwork.getEgos() ) {
			allAgents.put( ego , new AgentImpl( ego ) );
		}

		for ( Agent ego : allAgents.values() ) {
			final Collection<Agent> unkownFriendsOfFriends =
				ModelRunner.getUnknownFriendsOfFriends(
						ego,
						network.socialNetwork,
						allAgents);

			final Set<Id> actual = new TreeSet<Id>();
			for ( Agent candidate : unkownFriendsOfFriends ) {
				actual.add( candidate.getId() );
			}

			Assert.assertEquals(
					"unexpected friends of friends for ego "+ego.getId(),
					new TreeSet<Id>( network.unkownFriendsOfFriends.get( ego.getId() ) ),
					actual );
		}
	}

	@Test
	public void testGetUnknownFriendsOfFriendsReturnsOnlyRemaining() throws Exception {
		final TestSocialNetwork network = createTestNetwork();

		final Map<Id, Agent> allAgents = new LinkedHashMap<Id, Agent>();
		for ( Id ego : network.socialNetwork.getEgos() ) {
			allAgents.put( ego , new AgentImpl( ego ) );
		}

		final Random rand = new Random( 9734 );
		final Map<Id, Agent> partialAgents = new LinkedHashMap<Id, Agent>();
		for ( Id ego : network.socialNetwork.getEgos() ) {
			if ( rand.nextBoolean() ) continue;
			partialAgents.put( ego , allAgents.get( ego ) );
		}

		for ( Agent ego : allAgents.values() ) {
			final Collection<Agent> unkownFriendsOfFriends =
				ModelRunner.getUnknownFriendsOfFriends(
						ego,
						network.socialNetwork,
						partialAgents);

			for ( Agent candidate : unkownFriendsOfFriends ) {
				Assert.assertTrue(
						"got non remaining agent",
						partialAgents.values().contains( candidate ) );
			}
		}
	}

	@Test
	public void testGetUnknownFriendsOfFriendsReturnsUniqueAgents() {
		final TestSocialNetwork network = createTestNetwork();

		final Map<Id, Agent> allAgents = new LinkedHashMap<Id, Agent>();
		for ( Id ego : network.socialNetwork.getEgos() ) {
			allAgents.put( ego , new AgentImpl( ego ) );
		}

		for ( Agent ego : allAgents.values() ) {
			final Collection<Agent> v =
				ModelRunner.getUnknownFriendsOfFriends(
						ego,
						network.socialNetwork,
						Collections.unmodifiableMap( allAgents) );

			Assert.assertEquals(
					"unexpected number of friends of friends: duplicates",
					new HashSet<Agent>( v ).size(),
					v.size() );
		}
	}

	private static TestSocialNetwork createTestNetwork() {
		// 1 -- 2 -- 3
		// | \  |  / |
		// |  \ | /  |
		// 4 -- 5 -- 6
		// |    |    |
		// |    |    |
		// 7 -- 8 -- 9
		final TestSocialNetwork net = new TestSocialNetwork();

		// ids
		final Id<Person> id1 = Id.create( 1, Person.class );
		final Id<Person> id2 = Id.create( 2, Person.class );
		final Id<Person> id3 = Id.create( 3, Person.class );
		final Id<Person> id4 = Id.create( 4, Person.class );
		final Id<Person> id5 = Id.create( 5, Person.class );
		final Id<Person> id6 = Id.create( 6, Person.class );
		final Id<Person> id7 = Id.create( 7, Person.class );
		final Id<Person> id8 = Id.create( 8, Person.class );
		final Id<Person> id9 = Id.create( 9, Person.class );

		// horizontal
		net.socialNetwork.addTie( id1 , id2 );
		net.socialNetwork.addTie( id2 , id3 );
		net.socialNetwork.addTie( id4 , id5 );
		net.socialNetwork.addTie( id5 , id6 );
		net.socialNetwork.addTie( id7 , id8 );
		net.socialNetwork.addTie( id8 , id9 );

		// vertical
		net.socialNetwork.addTie( id1 , id4 );
		net.socialNetwork.addTie( id4 , id7 );
		net.socialNetwork.addTie( id2 , id5 );
		net.socialNetwork.addTie( id5 , id8 );
		net.socialNetwork.addTie( id3 , id6 );
		net.socialNetwork.addTie( id6 , id9 );

		// diagonal
		net.socialNetwork.addTie( id1 , id5 );
		net.socialNetwork.addTie( id5 , id3 );

		// expected friends info
		net.unkownFriendsOfFriends.put(
				id1,
				Arrays.asList( id3 , id6 , id8 , id7 ) );
		net.unkownFriendsOfFriends.put(
				id2,
				Arrays.asList( id4 , id8 , id6 ) );
		net.unkownFriendsOfFriends.put(
				id3,
				Arrays.asList( id1 , id4 , id8 , id9 ) );
		net.unkownFriendsOfFriends.put(
				id4,
				Arrays.asList( id2 , id3 , id6 , id8 ) );
		net.unkownFriendsOfFriends.put(
				id5,
				Arrays.asList( id7 , id9 ) );
		net.unkownFriendsOfFriends.put(
				id6,
				Arrays.asList( id2 , id1 , id4 , id8 ) );
		net.unkownFriendsOfFriends.put(
				id7,
				Arrays.asList( id1 , id5 , id9 ) );
		net.unkownFriendsOfFriends.put(
				id8,
				Arrays.asList( id4 , id6 , id1 , id2 , id3 ) );
		net.unkownFriendsOfFriends.put(
				id9,
				Arrays.asList( id3 , id5 , id7 ) );

		return net;
	}

	private static class TestSocialNetwork {
		public final LockedSocialNetwork socialNetwork = new LockedSocialNetwork( false );
		public final Map<Id<Person>, Collection<Id<Person>>> unkownFriendsOfFriends = new LinkedHashMap<>();
	}
	
	private static class AgentImpl implements Agent {
		private final Id<Person> id;

		public AgentImpl(final Id<Person> id) {
			this.id = id;
		}

		@Override
		public Id<Person> getId() {
			return id;
		}
	}
}

