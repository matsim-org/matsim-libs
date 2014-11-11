/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChooserSolutionTest.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;

import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * @author thibautd
 */
public class TimeModeChooserSolutionTest {
	private List<Plan> testPlans;

	@Before
	public void initPlans() {
		// TODO: read different plan forms from a file
		testPlans = new ArrayList<Plan>();
		PlanImpl p = new PlanImpl();
		testPlans.add( p );

		ActivityImpl act = p.createAndAddActivity( "h" );
		act.setEndTime( 12 );
		act.setLinkId( Id.create( 1 , Link.class ) );

		p.createAndAddLeg( "bike" );

		act = p.createAndAddActivity( "w" );
		act.setMaximumDuration( 298 );
		act.setLinkId( Id.create( 2 , Link.class) );

		p.createAndAddLeg( "bike" );

		act = p.createAndAddActivity( "h" );
		act.setLinkId( Id.create( 1 , Link.class) );
	}
	
	@Test
	public void testClone() throws Exception {
		for (Plan p : testPlans) {
			TimeModeChooserSolution sol = new TimeModeChooserSolution( p , new TripRouter() );
			Iterator<? extends Value> cloned = sol.getGenotype().iterator();
			Iterator<? extends Value> clone = sol.createClone().getGenotype().iterator();

			while (cloned.hasNext()) {
				assertTrue(
						"reprensentation of clone have less elements than the cloned!",
						clone.hasNext());
				Value cloneValue = clone.next();
				Value clonedValue = cloned.next();

				assertNotSame(
						"representation of clone and cloned reference the same instances!",
						cloneValue,
						clonedValue);

				assertEquals(
						"values of clone and clone are not equal!",
						cloneValue,
						clonedValue);
			}

			assertFalse(
					"reprensentation of clone have more elements than the cloned!",
					clone.hasNext());
		}
	}
}

