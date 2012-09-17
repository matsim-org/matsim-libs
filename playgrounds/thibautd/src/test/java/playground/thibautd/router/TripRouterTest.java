/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterTest.java
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
package playground.thibautd.router;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;

/**
 * @author thibautd
 */
public class TripRouterTest {
	@Test
	public void testTripInsertion() {
		PlanImpl plan = new PlanImpl();
		Activity o = plan.createAndAddActivity( "1" );
		Activity d = plan.createAndAddActivity( "5" );

		List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( new LegImpl( "2" ) );
		trip.add( new ActivityImpl( "3" , new IdImpl( "coucou" ) ) );
		trip.add( new LegImpl( "4" ) );

		TripRouter.insertTrip( plan , o , trip , d );

		assertEquals(
				"insertion did not produce the expected plan length!",
				5,
				plan.getPlanElements().size());

		int oldIndex = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			int newIndex = -1;

			if (pe instanceof Activity) {
				newIndex = Integer.parseInt( ((Activity) pe).getType() );
			}
			else {
				newIndex = Integer.parseInt( ((Leg) pe).getMode() );
			}

			assertTrue(
					"wrong inserted sequence: "+plan.getPlanElements(),
					newIndex > oldIndex);
			oldIndex = newIndex;
		}
	}
}

