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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappedLocation;

import playground.meisterk.org.matsim.run.ptRouting.PlansCalcRouteKtiInfo;

/**
 * Special Routing Module for finding (more or less) realistic public transit travel times.
 * 
 * The travel time of a leg with mode 'pt' (meaning pseudo/public transport) 
 * is estimated by a municipality-to-municipality travel time matrix, plus walk-speed access to and egress from the next pt stop.
 * 
 * @author mrieser
 *
 */
public class PlansCalcRouteKti extends PlansCalcRoute {

	private final PlansCalcRouteConfigGroup group;
	private final NetworkLayer network;
	private final Matrix ptTravelTimes;
	private final SwissHaltestellen haltestellen;
	private final Layer municipalities;
	
	public PlansCalcRouteKti(
			final PlansCalcRouteConfigGroup group,
			final NetworkLayer network, 
			final TravelCost costCalculator,
			final TravelTime timeCalculator, 
			final LeastCostPathCalculatorFactory factory,
			final PlansCalcRouteKtiInfo ptRoutingInfo) {
		super(group, network, costCalculator, timeCalculator, factory);
		this.group = group;
		this.network = network;
		this.ptTravelTimes = ptRoutingInfo.getPtTravelTimes();
		this.haltestellen = ptRoutingInfo.getHaltestellen();
		this.municipalities = ptRoutingInfo.getLocalWorld().getLayer("municipality");
	}
	
	@Override
	public double handleLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		if (TransportMode.pt.equals(leg.getMode())) {
			return handleSwissPtLeg(fromAct, leg, toAct);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	public double handleSwissPtLeg(final ActivityImpl fromAct, final LegImpl leg, final ActivityImpl toAct) {
		Coord fromStop = this.haltestellen.getClosestLocation(fromAct.getCoord());
		Coord toStop = this.haltestellen.getClosestLocation(toAct.getCoord());

		final List<MappedLocation> froms = this.municipalities.getNearestLocations(fromStop);
		final List<MappedLocation> tos = this.municipalities.getNearestLocations(toStop);
		Location from = froms.get(0);
		Location to = tos.get(0);
		Entry traveltime = this.ptTravelTimes.getEntry(from, to);
		if (traveltime == null) {
			throw new RuntimeException("No entry found for " + from.getId() + " --> " + to.getId());
		}
		final double timeInVehicle = traveltime.getValue() * 60.0;
		final double beeLineWalkTime = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord()) / this.group.getWalkSpeedFactor();

		final double walkDistance = CoordUtils.calcDistance(fromAct.getCoord(), fromStop) + CoordUtils.calcDistance(toAct.getCoord(), toStop);
		final double walkTime = walkDistance / this.group.getWalkSpeedFactor();
//		System.out.println(from.getId() + " > " + to.getId() + ": " + timeInVehicle/60 + "min + " + (walkTime / 60) + "min (" + walkDistance + "m walk); beeLine: " + beeLineWalkTime/60 + "min walk");

		RouteWRefs newRoute;
		if (beeLineWalkTime < (timeInVehicle + walkTime)) {
			newRoute = this.network.getFactory().createRoute(TransportMode.walk, fromAct.getLink(), toAct.getLink());
			leg.setRoute(newRoute);
			newRoute.setTravelTime(beeLineWalkTime);
		} else {
			newRoute = this.network.getFactory().createRoute(TransportMode.pt, fromAct.getLink(), toAct.getLink());
			leg.setRoute(newRoute);
			newRoute.setTravelTime(timeInVehicle + walkTime);
		}
		leg.setTravelTime(newRoute.getTravelTime());
//		System.out.println("cmpr:\t" + Time.writeTime(oldRoute.getTravTime()) + "\t" + Time.writeTime(leg.getRoute().getTravTime()) + "\t" + beeLineWalkTime);
		return newRoute.getTravelTime();
	}

}
