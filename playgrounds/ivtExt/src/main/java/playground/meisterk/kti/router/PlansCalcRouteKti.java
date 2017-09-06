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

import java.util.HashSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;


/**
 * Modifications of the routing module for the KTI project.
 *
 * @author mrieser
 * @author meisterk
 *
 */
public class PlansCalcRouteKti /*extends PlansCalcRoute*/ {

	/**
	 * Transport modes which are considered in the kti project.
	 *
	 * Use this set to check whether the mode of a leg is allowed or not.
	 */
	public static final HashSet<String> KTI_MODES;

	private final Network network;
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;

	static {
		KTI_MODES = new HashSet<String>();
		KTI_MODES.add(TransportMode.car);
		KTI_MODES.add(TransportMode.bike);
		KTI_MODES.add(TransportMode.pt);
		KTI_MODES.add(TransportMode.walk);
		KTI_MODES.add(TransportMode.ride);
	}

	public PlansCalcRouteKti(
			final PlansCalcRouteConfigGroup group,
			final Network network,
			final TravelDisutility costCalculator,
			final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory,
			final RouteFactories routeFactory,
			final PlansCalcRouteKtiInfo ptRoutingInfo) {
		//super(group, network, costCalculator, timeCalculator, factory, routeFactory);
		this.network = network;
		this.plansCalcRouteKtiInfo = ptRoutingInfo;
	}

//	@Override
//	public double handleLeg(Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
//
//		String mode = leg.getMode();
//
//		// TODO meisterk: This is a shortcut. Please find a general solution for that. [balmermi]
////		if (mode == TransportMode.ride) { mode = TransportMode.car; }
//
//		if (!KTI_MODES.contains(mode)) {
//			throw new RuntimeException("cannot handle legmode '" + mode + "'.");
//		}
//
//		double travelTime = 0.0;
//
//		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
//		Link toLink = this.network.getLinks().get(toAct.getLinkId());
//		if (fromLink.equals(toLink)) {
//			// create an empty route == staying on place if toLink == endLink
//			Route route = this.getRouteFactory().createRoute(mode, fromLink.getId(), toLink.getId());
//			route.setTravelTime(travelTime);
//			if (Double.isNaN(route.getDistance())) {
//				route.setDistance(0.0);
//			}
//			leg.setRoute(route);
//			leg.setDepartureTime(depTime);
//			leg.setTravelTime(travelTime);
//		} else {
//			if (mode.equals(TransportMode.pt)) {
//				travelTime = handleSwissPtLeg(fromAct, leg, toAct, depTime);
//			} else {
//				travelTime = super.handleLeg(person, leg, fromAct, toAct, depTime);
//			}
//		}
//
//		return travelTime;
//	}

//	/**
//	 * Make use of the Swiss National transport model to find more or less realistic travel times.
//	 *
//	 * The travel time of a leg with mode 'pt' (meaning pseudo/public transport)
//	 * is estimated by a municipality-to-municipality travel time matrix, plus walk-speed access to and egress from the next pt stop.
//	 *
//	 * @param fromAct
//	 * @param leg
//	 * @param toAct
//	 * @param depTime
//	 * @return
//	 */
//	public double handleSwissPtLeg(final Activity fromAct, final Leg leg, final Activity toAct, final double depTime) {
//
//		double travelTime = 0.0;
//
//		SwissHaltestelle fromStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
//		SwissHaltestelle toStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(toAct.getCoord());
//
//		Layer municipalities = this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality");
//		final List<? extends BasicLocation> froms = municipalities.getNearestLocations(fromStop.getCoord());
//		final List<? extends BasicLocation> tos = municipalities.getNearestLocations(toStop.getCoord());
//		BasicLocation fromMunicipality = froms.get(0);
//		BasicLocation toMunicipality = tos.get(0);
//
//		KtiPtRoute newRoute = new KtiPtRoute(fromAct.getLinkId(), toAct.getLinkId(), plansCalcRouteKtiInfo, fromStop, fromMunicipality, toMunicipality, toStop);
//		leg.setRoute(newRoute);
//
////		final double timeInVehicle = newRoute.calcInVehicleTime();
//		final double timeInVehicle = newRoute.getInVehicleTime();
//
//		final double walkAccessEgressDistance = newRoute.calcAccessEgressDistance(fromAct, toAct);
//		final double walkAccessEgressTime = PlansCalcRouteKti.getAccessEgressTime(walkAccessEgressDistance, this.configGroup);
//
//		newRoute.setDistance((walkAccessEgressDistance + newRoute.calcInVehicleDistance()));
//
//		travelTime = walkAccessEgressTime + timeInVehicle;
//
//		leg.setDepartureTime(depTime);
//		leg.setTravelTime(travelTime);
//
//		return travelTime;
//	}

	public static double getAccessEgressTime(final double distance, final PlansCalcRouteConfigGroup group) {
		return distance / group.getTeleportedModeSpeeds().get(TransportMode.walk);
	}

}
