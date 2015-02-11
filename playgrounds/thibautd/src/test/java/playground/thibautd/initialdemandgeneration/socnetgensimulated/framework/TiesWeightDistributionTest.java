/* *********************************************************************** *
 * project: org.matsim.*
 * TiesWeightDistributionTest.java
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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author thibautd
 */
public class TiesWeightDistributionTest {
	@Test
	public void testSimple() {
		final TiesWeightDistribution distr = new TiesWeightDistribution( 1 );

		distr.addValue( 0.1 );
		distr.addValue( -1.5 );
		distr.addValue( 15 , 3 );
		distr.addValue( 10 );
		distr.addValue( 0.2 );

		Assert.assertEquals(
				"wrong total count" ,
				7,
				distr.getOverallCount() );

		final double[] starts = distr.getBinStarts();
		final int[] counts = distr.getBinCounts();

		Assert.assertEquals(
				"starts and counts do not have same lenght" ,
				counts.length,
				starts.length );

		Assert.assertEquals(
				"wrong number of bins" ,
				4,
				starts.length );

		Assert.assertEquals(
				" unexpected bin start",
				-2,
				starts[ 0 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin start",
				0,
				starts[ 1 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin start",
				10,
				starts[ 2 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin start",
				15,
				starts[ 3 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin count",
				1,
				counts[ 0 ] );

		Assert.assertEquals(
				" unexpected bin count",
				2,
				counts[ 1 ] );

		Assert.assertEquals(
				" unexpected bin count",
				1,
				counts[ 2 ] );

		Assert.assertEquals(
				" unexpected bin count",
				3,
				counts[ 3 ] );
	}

	@Test
	public void testCombine() {
		final TiesWeightDistribution toAdd = new TiesWeightDistribution( 1 );

		toAdd.addValue( -1.5 );
		toAdd.addValue( 0.1 );
		toAdd.addValue( 0.2 );
		toAdd.addValue( 10 );
		toAdd.addValue( 15 , 3 );

		final TiesWeightDistribution distr = new TiesWeightDistribution( 1 );
		distr.addValue( 10.5 );
		distr.addCounts( toAdd );

		Assert.assertEquals(
				"wrong total count" ,
				8,
				distr.getOverallCount() );

		final double[] starts = distr.getBinStarts();
		final int[] counts = distr.getBinCounts();

		Assert.assertEquals(
				"starts and counts do not have same lenght" ,
				counts.length,
				starts.length );

		Assert.assertEquals(
				"wrong number of bins" ,
				4,
				starts.length );

		Assert.assertEquals(
				" unexpected bin start",
				-2,
				starts[ 0 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin start",
				0,
				starts[ 1 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin start",
				10,
				starts[ 2 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin start",
				15,
				starts[ 3 ],
				1E-9);

		Assert.assertEquals(
				" unexpected bin count",
				1,
				counts[ 0 ] );

		Assert.assertEquals(
				" unexpected bin count",
				2,
				counts[ 1 ] );

		Assert.assertEquals(
				" unexpected bin count",
				2,
				counts[ 2 ] );

		Assert.assertEquals(
				" unexpected bin count",
				3,
				counts[ 3 ] );
	}
}

