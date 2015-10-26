/* *********************************************************************** *
 * project: org.matsim.*
 * SnaUtilsClusteringTest.java
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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.MatsimTestUtils;

/**
 * @author thibautd
 */
public class SnaUtilsClusteringTest {
	// /////////////////////////////////////////////////////////////////////////
	// fixtures
	// /////////////////////////////////////////////////////////////////////////
	private final List<Fixture> fixtures = new ArrayList<Fixture>();
	private static class Fixture {
		public final LockedSocialNetwork socialNetwork;
		public final double clusteringIndex;
		public final double avgNetworkSize;

		public Fixture(
				final LockedSocialNetwork socialNetwork,
				final double clusteringIndex,
				final double avgNetworkSize) {
			this.socialNetwork = socialNetwork;
			this.clusteringIndex = clusteringIndex;
			this.avgNetworkSize = avgNetworkSize;
		}
	}

	@After
	public void clean() {
		fixtures.clear();
	}

	@Before
	public void createArentzeFixture() {
		// example from the paper
		final LockedSocialNetwork net = new LockedSocialNetwork( false );

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

		net.addTie( id1 , id2 );
		net.addTie( id1 , id3 );
		net.addTie( id2 , id3 );
		net.addTie( id1 , id9 );
		net.addTie( id3 , id9 );
		net.addTie( id8 , id9 );
		net.addTie( id8 , id7 );
		net.addTie( id6 , id7 );
		net.addTie( id6 , id8 );
		net.addTie( id6 , id5 );
		net.addTie( id6 , id4 );

		double sumNetSizes = 0;
		sumNetSizes += 3; // agent 1
		sumNetSizes += 2; // agent 2
		sumNetSizes += 3; // agent 3
		sumNetSizes += 1; // agent 4
		sumNetSizes += 1; // agent 5
		sumNetSizes += 4; // agent 6
		sumNetSizes += 2; // agent 7
		sumNetSizes += 3; // agent 8
		sumNetSizes += 3; // agent 9

		fixtures.add( new Fixture( net , 9. / 20 , sumNetSizes / 9. ) );
	}

	@Before
	public void createUnitIndex() {
		// 1 -- 2  5 -- 6
		// | \/ |  | \/ |
		// | /\ |  | /\ |
		// 4 -- 3  8 -- 7
		final LockedSocialNetwork net = new LockedSocialNetwork( false );

		// ids
		final Id<Person> id1 = Id.create( 1, Person.class );
		final Id<Person> id2 = Id.create( 2, Person.class );
		final Id<Person> id3 = Id.create( 3, Person.class );
		final Id<Person> id4 = Id.create( 4, Person.class );
		final Id<Person> id5 = Id.create( 5, Person.class );
		final Id<Person> id6 = Id.create( 6, Person.class );
		final Id<Person> id7 = Id.create( 7, Person.class );
		final Id<Person> id8 = Id.create( 8, Person.class );

		// horizontal
		net.addTie( id1 , id2 );
		net.addTie( id4 , id3 );
		net.addTie( id5 , id6 );
		net.addTie( id7 , id8 );

		// vertical
		net.addTie( id1 , id4 );
		net.addTie( id2 , id3 );
		net.addTie( id5 , id8 );
		net.addTie( id6 , id7 );

		// diagonal
		net.addTie( id1 , id3 );
		net.addTie( id4 , id2 );
		net.addTie( id5 , id7 );
		net.addTie( id8 , id6 );

		fixtures.add( new Fixture( net , 1 , 3 ) );
	}

	@Before
	public void createNullIndex() {
		// 1 -- 2  5    6
		//
		// 4 -- 3  8    7
		final LockedSocialNetwork net = new LockedSocialNetwork( false );

		// ids
		final Id<Person> id1 = Id.create( 1, Person.class );
		final Id<Person> id2 = Id.create( 2, Person.class );
		final Id<Person> id3 = Id.create( 3, Person.class );
		final Id<Person> id4 = Id.create( 4, Person.class );
		final Id<Person> id5 = Id.create( 5, Person.class );
		final Id<Person> id6 = Id.create( 6, Person.class );
		final Id<Person> id7 = Id.create( 7, Person.class );
		final Id<Person> id8 = Id.create( 8, Person.class );

		net.addTie( id1 , id2 );
		net.addTie( id4 , id3 );

		net.addEgo( id5 );
		net.addEgo( id6 );
		net.addEgo( id7 );
		net.addEgo( id8 );

		fixtures.add( new Fixture( net , 0 , .5 ) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testClusteringIndex() {
		for ( Fixture f : fixtures ) {
			Assert.assertEquals(
					"unexpected clustering index",
					f.clusteringIndex,
					SnaUtils.calcClusteringCoefficient( f.socialNetwork ),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testAvgNetworkSize() {
		for ( Fixture f : fixtures ) {
			Assert.assertEquals(
					"unexpected avg. network size",
					f.avgNetworkSize,
					SnaUtils.calcAveragePersonalNetworkSize( f.socialNetwork ),
					MatsimTestUtils.EPSILON);
		}

	}
}

