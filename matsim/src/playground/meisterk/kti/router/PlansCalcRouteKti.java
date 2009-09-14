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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappedLocation;


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
			return handleSwissPtLeg(fromAct, leg, toAct, depTime);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	public double handleSwissPtLeg(final ActivityImpl fromAct, final LegImpl leg, final ActivityImpl toAct, final double depTime) {
		
		double travelTime = 0.0;
		
		SwissHaltestelle fromStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
		SwissHaltestelle toStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(toAct.getCoord());

		Layer municipalities = this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality");
		final List<MappedLocation> froms = municipalities.getNearestLocations(fromStop.getCoord());
		final List<MappedLocation> tos = municipalities.getNearestLocations(toStop.getCoord());
		Location fromMunicipality = froms.get(0);
		Location toMunicipality = tos.get(0);

		KtiPtRoute newRoute = new KtiPtRoute(fromAct.getLink(), toAct.getLink(), plansCalcRouteKtiInfo, fromStop, fromMunicipality, toMunicipality, toStop);
		leg.setRoute(newRoute);

//		final double timeInVehicle = newRoute.calcInVehicleTime();
		final double timeInVehicle = newRoute.getPtMatrixInVehicleTime();
		
		final double walkAccessEgressDistance = newRoute.calcAccessEgressDistance(fromAct, toAct);
		final double walkAccessEgressTime = PlansCalcRouteKti.getAccessEgressTime(walkAccessEgressDistance, this.configGroup);

		newRoute.setDistance((walkAccessEgressDistance + newRoute.calcInVehicleDistance()));
		
		travelTime = walkAccessEgressTime + timeInVehicle;
		
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travelTime);
		leg.setArrivalTime(depTime + travelTime);
		
		return travelTime;
	}

	public static double getAccessEgressTime(final double distance, final PlansCalcRouteConfigGroup group) {
		return distance / group.getWalkSpeedFactor();
	}
	
}
