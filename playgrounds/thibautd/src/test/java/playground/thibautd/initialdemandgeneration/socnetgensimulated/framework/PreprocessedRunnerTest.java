/* *********************************************************************** *
 * project: org.matsim.*
 * PreprocessedRunnerTest.java
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.DeterministicPart;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.NoErrorTerm;
import playground.thibautd.socnetsim.population.SocialNetwork;

/**
 * @author thibautd
 */
public class PreprocessedRunnerTest {
	private static final Logger log =
		Logger.getLogger(PreprocessedRunnerTest.class);

	@Before
	public void logLevel() {
		Logger.getLogger( PreprocessedModelRunner.class ).setLevel( Level.TRACE );
	}


	@Test
	public void testNone() {
		final SocialNetwork network = run( 2 , 0 );

		Assert.assertEquals(
				"unexpected alters for ego 0 ",
				Collections.emptySet(),
				network.getAlters( Id.createPersonId( 0 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 1 ",
				Collections.emptySet(),
				network.getAlters( Id.createPersonId( 1 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 2 ",
				Collections.emptySet(),
				network.getAlters( Id.createPersonId( 2 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 3 ",
				Collections.emptySet(),
				network.getAlters( Id.createPersonId( 3 ) ) );

	}

	@Test
	public void testOnlyPrimary() {
		final SocialNetwork network = run( 1 , 0 );

		Assert.assertEquals(
				"unexpected alters for ego 0 ",
				idSet( 1 ),
				network.getAlters( Id.createPersonId( 0 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 1 ",
				idSet( 0 , 2 , 3 ),
				network.getAlters( Id.createPersonId( 1 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 2 ",
				idSet( 1 ),
				network.getAlters( Id.createPersonId( 2 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 3 ",
				idSet( 1 ),
				network.getAlters( Id.createPersonId( 3 ) ) );

	}

	@Test
	public void testWithFriendsOfFriends() {
		final SocialNetwork network = run( 1 , 1 );

		Assert.assertEquals(
				"unexpected alters for ego 0 ",
				idSet( 1 , 3 ),
				network.getAlters( Id.createPersonId( 0 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 1 ",
				idSet( 0 , 2 , 3 ),
				network.getAlters( Id.createPersonId( 1 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 2 ",
				idSet( 1 ),
				network.getAlters( Id.createPersonId( 2 ) ) );

		Assert.assertEquals(
				"unexpected alters for ego 3 ",
				idSet( 0 , 1 ),
				network.getAlters( Id.createPersonId( 3 ) ) );

	}

	private SocialNetwork run( final double primary , final double secondaryReduction ) {
		final PreprocessedModelRunnerConfigGroup config = new PreprocessedModelRunnerConfigGroup();
		config.setPrimarySampleRate( 1 );
		config.setSecondarySampleRate( 1 );

		final IndexedPopulation population =
			new IndexedPopulation( new Id[ ]{
				Id.createPersonId( 0 ),
				Id.createPersonId( 1 ),
				Id.createPersonId( 2 ),
				Id.createPersonId( 3 )
			} ) {};
		final TieUtility utility =
			new TieUtility(
				 new DeterministicPart() {
					@Override
					public double calcDeterministicPart( int ego , int alter ) {
						return isTie( ego , alter , 0 , 1 ) ? 1 :
							isTie( ego , alter , 0 , 2 ) ? -1 :
							isTie( ego , alter , 0 , 3 ) ? 0 :
							isTie( ego , alter , 1 , 2 ) ? 1 :
							isTie( ego , alter , 1 , 3 ) ? 1 :
							isTie( ego , alter , 2 , 3 ) ? -1 :
							failInteger( ego, alter );
					}

					private int failInteger( int ego , int alter ) {
						throw new RuntimeException( ego+"-"+alter );
					}
				},
				new NoErrorTerm(),
				false );

		final ModelRunner runner =
			new PreprocessedModelRunner(
					config,
					population,
					utility,
					null );

		final Thresholds thr = new Thresholds( primary , secondaryReduction );
		log.info( "generate network with "+thr );
		return runner.runModel( thr );
	}

	private static Set<Id<Person>> idSet( int... ids ) {
		final HashSet<Id<Person>> set = new HashSet< >();
		for ( int id : ids ) set.add( Id.createPersonId( id ) );
		return set;
	}

	private static boolean isTie( final int e, final int a, final int et, final int at ) {
		return (e == et && a == at) || (e == at && a == et);
	}
}

