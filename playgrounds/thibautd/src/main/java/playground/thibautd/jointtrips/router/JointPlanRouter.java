/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanRouter.java
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
package playground.thibautd.jointtrips.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.router.PlanRouter;
import playground.thibautd.router.TripRouter;

/**
 * Based on the {@link PlanRouter}, but modifies "old" plan elements
 * rather than inserting new ones. Car passenger mode is specially handled,
 * using explicitly {@link CarPassengerLegRouter}.
 *
 * @author thibautd
 */
public class JointPlanRouter extends PlanRouter {
	private final CarPassengerLegRouter passengerRouter = new CarPassengerLegRouter();

	public JointPlanRouter(
			final TripRouter routingHandler,
			final ActivityFacilities facilities ) {
		super( routingHandler , facilities );
	}

	public JointPlanRouter(
			final TripRouter routingHandler) {
		super( routingHandler );
	}

	protected void updatePlanElements(
			final Plan plan,
			final List<PlanElement> newPlanElements) {
		Iterator<PlanElement> toChange = plan.getPlanElements().iterator();
		Iterator<PlanElement> changeInfo = newPlanElements.iterator() ;

		while (toChange.hasNext()) {
			PlanElement peToChange = toChange.next();
			PlanElement peChangeInfo = changeInfo.next();

			if (peToChange instanceof Leg) {
				Leg legToChange = (Leg) peToChange;
				Leg legChangeInfo = (Leg) peChangeInfo;

				if ( legToChange.getMode().equals( JointActingTypes.PASSENGER ) ) {
					passengerRouter.routeLeg( null , legToChange , null , null , legChangeInfo.getDepartureTime() );
				}
				else {
					legToChange.setDepartureTime( legChangeInfo.getDepartureTime() );
					legToChange.setTravelTime( legChangeInfo.getTravelTime() );
					legToChange.setRoute( legChangeInfo.getRoute() );
				}
			}
		}
		if (changeInfo.hasNext()) {
			throw new RuntimeException( plan.getPlanElements()+" and "+newPlanElements+" have incompatible size" );
		}
	}
}
