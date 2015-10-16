/* *********************************************************************** *
 * project: org.matsim.*
 * TestCenteredPenalty.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride.scoring;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class TestCenteredPenalty {
	private final double radius = 6000;
	private final Coord center = new Coord((double) 0, (double) 0);
	private final Coord somewhereOutside = new Coord((double) 0, 2 * radius);
	private final Coord middleZone = new Coord((double) 0, radius / 2);
	private final double maxPenalty = 1000;

	private CenteredTimeProportionalPenalty penalty;

	@Before
	public void init() {
		penalty = new CenteredTimeProportionalPenalty( center , radius , maxPenalty );
	}

	// ///////////////////////////////////////////////////////////////////
	// check costs
	// ///////////////////////////////////////////////////////////////////
	@Test
	public void testPenaltyAtCenter() throws Exception {
		penalty.park( 0 , center );

		Assert.assertEquals(
				"unexpected penalty at center",
				maxPenalty,
				penalty.getCostPerSecond(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testPenaltyFarAway() throws Exception {
		penalty.park( 0 , somewhereOutside );

		Assert.assertEquals(
				"unexpected penalty outside",
				0,
				penalty.getCostPerSecond(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testPenaltyMiddle() throws Exception {
		penalty.park( 0 , middleZone );

		Assert.assertEquals(
				"unexpected penalty in the middle",
				maxPenalty / 2,
				penalty.getCostPerSecond(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCostAtCenter() throws Exception {
		double start = 25;
		double duration = 10;
		penalty.park( start , center );
		penalty.unPark( start + duration );
		
		penalty.finish();

		Assert.assertEquals(
				"unexpected parking cost",
				-duration * maxPenalty,
				penalty.getPenalty(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCostAtMiddle() throws Exception {
		double start = 25;
		double duration = 10;
		penalty.park( start , middleZone );
		penalty.unPark( start + duration );
		
		penalty.finish();

		Assert.assertEquals(
				"unexpected parking cost",
				-duration * (maxPenalty / 2d),
				penalty.getPenalty(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCostOutside() throws Exception {
		double start = 25;
		double duration = 10;
		penalty.park( start , somewhereOutside );
		penalty.unPark( start + duration );
		
		penalty.finish();

		Assert.assertEquals(
				"unexpected parking cost",
				0,
				penalty.getPenalty(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testChangeOfState() throws Exception {
		double start = 25;
		double duration = 10;
		penalty.park( start , center );
		penalty.unPark( start + duration );
	
		start += 2 * duration;
		penalty.park( start , somewhereOutside );
	
		Assert.assertEquals(
				"unexpected penalty after state change",
				0,
				penalty.getCostPerSecond(),
				MatsimTestUtils.EPSILON);

		Assert.assertEquals(
				"unexpected park time after state change",
				start,
				penalty.getParkingTime(),
				MatsimTestUtils.EPSILON);
	}
}

