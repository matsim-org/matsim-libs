/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.mobsim.qsim;

import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.BookingEngine;
import org.matsim.core.mobsim.qsim.DrtTaxiPrebookingWakeupGenerator;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import com.google.inject.Inject;

public class DrtAgentPlanUpdater implements BookingEngine.AgentPlanUpdater {
	private EditTrips editTrips;
	private EditPlans editPlans;

	@Inject
	public DrtAgentPlanUpdater( BookingEngine bookingEngine ) {
		bookingEngine.registerAgentPlanUpdater(TransportMode.drt, this);
	}

	@Override
	public void init(EditTrips editTrips, EditPlans editPlans) {
		this.editTrips = editTrips;
		this.editPlans = editPlans;
	}

	@Override
	public void updateAgentPlan(MobsimAgent agent, TripInfo tripInfo) {
		Gbl.assertIf(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Activity);

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		TripStructureUtils.Trip theTrip = null;
		for (TripStructureUtils.Trip drtTrip : TripStructureUtils.getTrips(plan,
				DrtTaxiPrebookingWakeupGenerator.drtStageActivities )) {
			// recall that we have set the activity end time of the current activity to infinity, so we cannot use that any more.  :-( ?!
			// could instead use some kind of ID.  Not sure if that would really be better.
			if (CoordUtils.calcEuclideanDistance(drtTrip.getOriginActivity().getCoord(),
					tripInfo.getPickupLocation().getCoord()) > 1000.) {
				continue;
			}
			if (CoordUtils.calcEuclideanDistance(drtTrip.getDestinationActivity().getCoord(),
					tripInfo.getDropoffLocation().getCoord()) > 1000.) {
				continue;
			}
			theTrip = drtTrip;
			break;
		}
		Gbl.assertNotNull(theTrip);
		Iterator<TripStructureUtils.Trip> walkTripsIter = TripStructureUtils.getTrips(theTrip.getTripElements(),
				new StageActivityTypesImpl(TransportMode.walk)).iterator();

		// ---

		Gbl.assertIf(walkTripsIter.hasNext());
		TripStructureUtils.Trip accessWalkTrip = walkTripsIter.next();
		accessWalkTrip.getDestinationActivity().setCoord(tripInfo.getPickupLocation().getCoord());
		accessWalkTrip.getDestinationActivity().setLinkId(tripInfo.getPickupLocation().getLinkId());
		accessWalkTrip.getDestinationActivity().setFacilityId(null);

		List<? extends PlanElement> pe = editTrips.replanFutureTrip(accessWalkTrip, plan, TransportMode.walk);
		List<Leg> legs = TripStructureUtils.getLegs(pe);
		double ttime = 0.;
		for (Leg leg : legs) {
			if (leg.getRoute() != null) {
				ttime += leg.getTravelTime();
			} else {
				ttime += leg.getTravelTime();
			}
		}
		double buffer = 300.;
		editPlans.rescheduleCurrentActivityEndtime(agent, tripInfo.getExpectedBoardingTime() - ttime - buffer);

		// ---

		Gbl.assertIf(walkTripsIter.hasNext());
		TripStructureUtils.Trip egressWalkTrip = walkTripsIter.next();
		final Activity egressWalkOriginActivity = egressWalkTrip.getOriginActivity();
		egressWalkOriginActivity.setCoord(tripInfo.getDropoffLocation().getCoord());
		egressWalkOriginActivity.setLinkId(tripInfo.getDropoffLocation().getLinkId());
		egressWalkOriginActivity.setFacilityId(null);

		editTrips.replanFutureTrip(egressWalkTrip, plan, TransportMode.walk);
		// yy maybe better do this at dropoff?

		// ----

		Gbl.assertIf(!walkTripsIter.hasNext());
	}
}
