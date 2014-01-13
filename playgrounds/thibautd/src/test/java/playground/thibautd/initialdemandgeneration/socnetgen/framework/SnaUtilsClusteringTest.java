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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class SnaUtilsClusteringTest {
	// /////////////////////////////////////////////////////////////////////////
	// fixtures
	// /////////////////////////////////////////////////////////////////////////
	private final List<Fixture> fixtures = new ArrayList<Fixture>();
	private static class Fixture {
		public final SocialNetwork socialNetwork;
		public final double clusteringIndex;

		public Fixture(
				final SocialNetwork socialNetwork,
				final double clusteringIndex) {
			this.socialNetwork = socialNetwork;
			this.clusteringIndex = clusteringIndex;
		}
	}

	@After
	public void clean() {
		fixtures.clear();
	}

	@Before
	public void createArentzeFixture() {
		// example from the paper
		final SocialNetwork net = new SocialNetwork( false );

		// ids
		final Id id1 = new IdImpl( 1 );
		final Id id2 = new IdImpl( 2 );
		final Id id3 = new IdImpl( 3 );
		final Id id4 = new IdImpl( 4 );
		final Id id5 = new IdImpl( 5 );
		final Id id6 = new IdImpl( 6 );
		final Id id7 = new IdImpl( 7 );
		final Id id8 = new IdImpl( 8 );
		final Id id9 = new IdImpl( 9 );

		net.addTie( new Tie( id1 , id2 ) );
		net.addTie( new Tie( id1 , id3 ) );
		net.addTie( new Tie( id2 , id3 ) );
		net.addTie( new Tie( id1 , id9 ) );
		net.addTie( new Tie( id3 , id9 ) );
		net.addTie( new Tie( id8 , id9 ) );
		net.addTie( new Tie( id8 , id7 ) );
		net.addTie( new Tie( id6 , id7 ) );
		net.addTie( new Tie( id6 , id8 ) );
		net.addTie( new Tie( id6 , id5 ) );
		net.addTie( new Tie( id6 , id4 ) );

		fixtures.add( new Fixture( net , 9. / 20 ) );
	}

	@Before
	public void createUnitIndex() {
		// 1 -- 2 
		// | \/ |
		// | /\ |
		// 4 -- 3
		final SocialNetwork net = new SocialNetwork( false );

		// ids
		final Id id1 = new IdImpl( 1 );
		final Id id2 = new IdImpl( 2 );
		final Id id3 = new IdImpl( 3 );
		final Id id4 = new IdImpl( 4 );

		// horizontal
		net.addTie( new Tie( id1 , id2 ) );
		net.addTie( new Tie( id4 , id3 ) );

		// vertical
		net.addTie( new Tie( id1 , id4 ) );
		net.addTie( new Tie( id2 , id3 ) );

		// diagonal
		net.addTie( new Tie( id1 , id3 ) );
		net.addTie( new Tie( id4 , id2 ) );

		fixtures.add( new Fixture( net , 1 ) );
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
}

