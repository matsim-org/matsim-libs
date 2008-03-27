/* *********************************************************************** *
 * project: org.matsim.*
 * CalcSwissPtPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel.kti_pt;

import java.util.List;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.misc.Time;
import org.matsim.world.Layer;
import org.matsim.world.Location;

public class CalcSwissPtRoute implements PlanAlgorithmI {

	private final static double WALK_SPEED = 3.0/3.6; // 3.6km/h --> m/s = speed of people walking to the next station from home (bee-line!)

	private final Matrix ptTravelTimes;
	private final SwissHaltestellen haltestellen;
	private final Layer municipalities;

	public CalcSwissPtRoute(final Matrix ptTravelTimes, final SwissHaltestellen haltestellen, final Layer municipalities) {
		this.ptTravelTimes = ptTravelTimes;
		this.haltestellen = haltestellen;
		this.municipalities = municipalities;
	}

	public void run(final Plan plan) {
		final List<Object> actslegs = plan.getActsLegs();
		for (int i = 1, n = actslegs.size(); i < n; i +=2) {
			final Leg leg = (Leg)actslegs.get(i);
//			if ("pt".equals(leg.getMode())) {
				handleLeg((Act)actslegs.get(i-1), leg, (Act)actslegs.get(i+1));
//			}
		}
	}

	public void handleLeg(final Act fromAct, final Leg leg, final Act toAct) {
		CoordI fromStop = this.haltestellen.getClosestLocation(fromAct.getCoord());
		CoordI toStop = this.haltestellen.getClosestLocation(toAct.getCoord());
		
		final List<Location> froms = this.municipalities.getNearestLocations(fromStop);
		final List<Location> tos = this.municipalities.getNearestLocations(toStop);
		Location from = froms.get(0);
		Location to = tos.get(0);
		Entry traveltime = this.ptTravelTimes.getEntry(from, to);
		if (traveltime == null) {
			System.err.println("no entry found for " + from.getId() + " --> " + to.getId());
		} else {
			final double timeInVehicle = traveltime.getValue() * 60.0;
			final double beeLineWalkTime = fromAct.getCoord().calcDistance(toAct.getCoord()) / WALK_SPEED;

			final double walkDistance = fromAct.getCoord().calcDistance(fromStop) + toAct.getCoord().calcDistance(toStop);
			final double walkTime = walkDistance / WALK_SPEED;
//			System.out.println(from.getId() + " > " + to.getId() + ": " + timeInVehicle/60 + "min + " + (walkTime / 60) + "min (" + walkDistance + "m walk); beeLine: " + beeLineWalkTime/60 + "min walk");

			Route oldRoute = leg.getRoute();
			if (beeLineWalkTime < (timeInVehicle + walkTime)) {
				leg.createRoute(null, Time.writeTime(beeLineWalkTime));
			} else {
				leg.createRoute(null, Time.writeTime(timeInVehicle + walkTime));
			}
//			System.out.println("cmpr:\t" + Time.writeTime(oldRoute.getTravTime()) + "\t" + Time.writeTime(leg.getRoute().getTravTime()) + "\t" + beeLineWalkTime);
		}
	}

}
