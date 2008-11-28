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

package playground.marcel.kti.router;

import java.util.List;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.routes.CarRoute;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.geometry.Coord;
import org.matsim.world.Layer;
import org.matsim.world.Location;

public class PlansCalcRouteKti extends PlansCalcRoute {

	private final static double WALK_SPEED = 3.0/3.6; // 3.0km/h --> m/s = speed of people walking to the next station from home (bee-line!)

	private final Matrix ptTravelTimes;
	private final SwissHaltestellen haltestellen;
	private final Layer municipalities;

	public PlansCalcRouteKti(final NetworkLayer network, final PreProcessLandmarks preProcessData,
			final TravelCost costCalculator, final TravelTime timeCalculator,
			final Matrix ptTravelTimes, final SwissHaltestellen haltestellen, final Layer municipalities) {
		this(network, preProcessData, costCalculator, timeCalculator, new FreespeedTravelTimeCost(), ptTravelTimes, haltestellen, municipalities);
	}

	private PlansCalcRouteKti(final NetworkLayer network, final PreProcessLandmarks preProcessData,
			final TravelCost costCalculator, final TravelTime timeCalculator,
			final FreespeedTravelTimeCost timeCostCalc,
			final Matrix ptTravelTimes, final SwissHaltestellen haltestellen, final Layer municipalities) {
		super(new AStarLandmarks(network, preProcessData, costCalculator, timeCalculator),
				new AStarLandmarks(network, preProcessData, timeCostCalc, timeCostCalc));
		this.ptTravelTimes = ptTravelTimes;
		this.haltestellen = haltestellen;
		this.municipalities = municipalities;
	}

	@Override
	public double handleLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		if (BasicLeg.Mode.pt.equals(leg.getMode())) {
			return handleSwissPtLeg(fromAct, leg, toAct);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	public double handleSwissPtLeg(final Act fromAct, final Leg leg, final Act toAct) {
		Coord fromStop = this.haltestellen.getClosestLocation(fromAct.getCoord());
		Coord toStop = this.haltestellen.getClosestLocation(toAct.getCoord());

		final List<Location> froms = this.municipalities.getNearestLocations(fromStop);
		final List<Location> tos = this.municipalities.getNearestLocations(toStop);
		Location from = froms.get(0);
		Location to = tos.get(0);
		Entry traveltime = this.ptTravelTimes.getEntry(from, to);
		if (traveltime == null) {
			throw new RuntimeException("No entry found for " + from.getId() + " --> " + to.getId());
		}
		final double timeInVehicle = traveltime.getValue() * 60.0;
		final double beeLineWalkTime = fromAct.getCoord().calcDistance(toAct.getCoord()) / WALK_SPEED;

		final double walkDistance = fromAct.getCoord().calcDistance(fromStop) + toAct.getCoord().calcDistance(toStop);
		final double walkTime = walkDistance / WALK_SPEED;
//		System.out.println(from.getId() + " > " + to.getId() + ": " + timeInVehicle/60 + "min + " + (walkTime / 60) + "min (" + walkDistance + "m walk); beeLine: " + beeLineWalkTime/60 + "min walk");

//		Route oldRoute = leg.getRoute();
		CarRoute newRoute;
		if (beeLineWalkTime < (timeInVehicle + walkTime)) {
			newRoute = leg.createRoute();
			newRoute.setTravTime(beeLineWalkTime);
		} else {
			newRoute = leg.createRoute();
			newRoute.setTravTime(timeInVehicle + walkTime);
		}
//		System.out.println("cmpr:\t" + Time.writeTime(oldRoute.getTravTime()) + "\t" + Time.writeTime(leg.getRoute().getTravTime()) + "\t" + beeLineWalkTime);
		return newRoute.getTravTime();
	}

}
