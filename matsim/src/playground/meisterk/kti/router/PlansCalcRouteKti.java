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

package playground.meisterk.kti.router;

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
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappedLocation;


/**
 * Special Routing Module for finding (more or less) realistic public transit travel times.
 * 
 * The travel time of a leg with mode 'pt' (meaning pseudo/public transport) 
 * is estimated by a municipality-to-municipality travel time matrix, plus walk-speed access to and egress from the closest pt stop.
 * 
 * @author mrieser
 * @author meisterk
 *
 */
public class PlansCalcRouteKti extends PlansCalcRoute {

	private final NetworkLayer network;
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	
	public PlansCalcRouteKti(
			final PlansCalcRouteConfigGroup group,
			final NetworkLayer network, 
			final TravelCost costCalculator,
			final TravelTime timeCalculator, 
			final LeastCostPathCalculatorFactory factory,
			final PlansCalcRouteKtiInfo ptRoutingInfo) {
		super(group, network, costCalculator, timeCalculator, factory);
		this.network = network;
		this.plansCalcRouteKtiInfo = ptRoutingInfo;
	}
	
	@Override
	public double handleLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		if (TransportMode.pt.equals(leg.getMode())) {
			return handleSwissPtLeg(fromAct, leg, toAct);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	/**
	 * 
	 * @param fromAct
	 * @param leg
	 * @param toAct
	 * @return
	 */
	public double handleSwissPtLeg(final ActivityImpl fromAct, final LegImpl leg, final ActivityImpl toAct) {
		
		double travelTime = 0.0;
		double distance = 0.0;

		for (LegImpl pseudoLeg : new LegImpl[]{
				PlansCalcRouteKti.getPseudoAccessLeg(fromAct, this.plansCalcRouteKtiInfo.getHaltestellen(), this.network, this.configGroup),
				PlansCalcRouteKti.getPseudoPtLeg(fromAct, toAct, this.plansCalcRouteKtiInfo, this.network),
				PlansCalcRouteKti.getPseudoEgressLeg(toAct, this.plansCalcRouteKtiInfo.getHaltestellen(), this.network, this.configGroup)
		}) {
			travelTime += pseudoLeg.getTravelTime();
			distance += pseudoLeg.getRoute().getDistance();
		}
		
		RouteWRefs newRoute = this.network.getFactory().createRoute(TransportMode.pt, fromAct.getLink(), toAct.getLink());;
		/*
		 * TODO
		 * to stay consistent with outer routers, crow-fly distances are multiplied with 1.5
		 * but why is it not multiplied BEFORE traveltime calculation?
		 */
		newRoute.setDistance(distance * 1.5);
		leg.setRoute(newRoute);
		leg.setTravelTime(travelTime);
		
		return travelTime;
	}

	public static LegImpl getPseudoAccessLeg(
			final ActivityImpl fromAct, 
			final SwissHaltestellen haltestellen, 
			final NetworkLayer network,
			final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		
		LegImpl pseudoLeg = new LegImpl(TransportMode.undefined);
		
		Coord fromStop = haltestellen.getClosestLocation(fromAct.getCoord());
		pseudoLeg.setRoute(network.getFactory().createRoute(TransportMode.undefined, null, null));
		
		double distance = CoordUtils.calcDistance(fromAct.getCoord(), fromStop);
		pseudoLeg.getRoute().setDistance(distance);
		
		pseudoLeg.setTravelTime(distance / plansCalcRouteConfigGroup.getWalkSpeedFactor());
		
		return pseudoLeg;
		
	}
	
	public static LegImpl getPseudoPtLeg(
			final ActivityImpl fromAct, 
			final ActivityImpl toAct,
			final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo,
			final NetworkLayer network) { 

		LegImpl pseudoLeg = new LegImpl(TransportMode.undefined);

		Coord fromStop = plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
		Coord toStop = plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(toAct.getCoord());

		Layer municipalities = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality");
		final List<MappedLocation> froms = municipalities.getNearestLocations(fromStop);
		final List<MappedLocation> tos = municipalities.getNearestLocations(toStop);
		Location from = froms.get(0);
		Location to = tos.get(0);
		Entry traveltime = plansCalcRouteKtiInfo.getPtTravelTimes().getEntry(from, to);
		if (traveltime == null) {
			throw new RuntimeException("No entry found for " + from.getId() + " --> " + to.getId());
		}
		
		pseudoLeg.setTravelTime(traveltime.getValue() * 60.0);

		pseudoLeg.setRoute(network.getFactory().createRoute(TransportMode.undefined, null, null));
		double distance = CoordUtils.calcDistance(fromStop, toStop);
		pseudoLeg.getRoute().setDistance(distance);
		
		return pseudoLeg;
		
	}
	
	public static LegImpl getPseudoEgressLeg(
			final ActivityImpl toAct, 
			final SwissHaltestellen haltestellen, 
			final NetworkLayer network,
			final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		
		LegImpl pseudoLeg = new LegImpl(TransportMode.undefined);
		
		Coord toStop = haltestellen.getClosestLocation(toAct.getCoord());
		pseudoLeg.setRoute(network.getFactory().createRoute(TransportMode.undefined, null, null));
		
		double distance = CoordUtils.calcDistance(toAct.getCoord(), toStop);
		pseudoLeg.getRoute().setDistance(distance);
		
		pseudoLeg.setTravelTime(distance / plansCalcRouteConfigGroup.getWalkSpeedFactor());
		
		return pseudoLeg;
		
	}
	
}
