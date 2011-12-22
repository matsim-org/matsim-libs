/* *********************************************************************** *
 * project: org.matsim.*
 * IdsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.population;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author thibautd
 */
public class IdsTest {
	// ordered list of long values
	private static final long[] longValues = 
			{ -185 , 0 , 1 , 10 , 23 , 42 , 54 , 78 , 88 ,
				103846573, Long.MAX_VALUE };
	
	private List< Tuple<IdLeg, IdLeg> > pairs = new ArrayList< Tuple<IdLeg, IdLeg> >();

	@Before
	public void constructPairs() {
		for (long id : longValues) {
			pairs.add( new Tuple<IdLeg, IdLeg>(
						new IdLeg( id ),
						new IdLeg( id )) );
		}
	}


	@Test
	public void testEquals() {
		for (Tuple< IdLeg , IdLeg > tuple : pairs) {
			Assert.assertTrue(
					"ids with same long value were not considered as equals",
					tuple.getFirst().equals( tuple.getSecond() ));

			for (Tuple< IdLeg , IdLeg > differentTuple : pairs) {
				if (differentTuple != tuple) {
					Assert.assertFalse(
							"ids with different long value were considered as equals",
							tuple.getFirst().equals( differentTuple.getFirst() ));
				}
			}
		}
	}

	@Test
	public void testHashCode() {
		for (Tuple< IdLeg , IdLeg > tuple : pairs) {
			Assert.assertEquals(
					"ids with same long value have different hashCodes",
					tuple.getFirst().hashCode(),
					tuple.getSecond().hashCode());
		}
	}

	@Test
	public void testCompare() {
		for (int i=0; i < longValues.length; i++) {
			IdLeg ref = new IdLeg( i );
			for (int j=0; j < i; j++) {
				Assert.assertTrue(
					 "id with long value "+j+" were not found inferior to "+i,
					 (new IdLeg( j )).compareTo( ref ) < 0);
			}

			Assert.assertTrue(
				 "id with long value "+i+" were not found equal to "+i,
				 (new IdLeg( i )).compareTo( ref ) == 0);

			for (int j= i+1; j < longValues.length; j++) {
				Assert.assertTrue(
					 "id with long value "+j+" were not found superior to "+i,
					 (new IdLeg( j )).compareTo( ref ) > 0);
			}
		}
	}
}

