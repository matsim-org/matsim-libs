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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.matrices.Entry;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappedLocation;

import playground.meisterk.kti.router.SwissHaltestellen.SwissHaltestelle;


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
			return handleSwissPtLeg(fromAct, leg, toAct);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	public double handleSwissPtLeg(final ActivityImpl fromAct, final LegImpl leg, final ActivityImpl toAct) {
		
		double travelTime = 0.0;
		
		SwissHaltestelle fromStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
		SwissHaltestelle toStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(toAct.getCoord());

		Layer municipalities = this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality");
		final List<MappedLocation> froms = municipalities.getNearestLocations(fromStop.getCoord());
		final List<MappedLocation> tos = municipalities.getNearestLocations(toStop.getCoord());
		Location fromMunicipality = froms.get(0);
		Location toMunicipality = tos.get(0);

		final double timeInVehicle = PlansCalcRouteKti.getTimeInVehicle(fromStop, fromMunicipality, toMunicipality, toStop, this.plansCalcRouteKtiInfo);
		
		final double walkAccessEgressDistance = PlansCalcRouteKti.getAccessEgressDistance(fromAct, fromStop, toStop, toAct);
		final double walkAccessEgressTime = PlansCalcRouteKti.getAccessEgressTime(walkAccessEgressDistance, this.configGroup);

		GenericRoute newRoute = (GenericRoute) this.network.getFactory().createRoute(TransportMode.pt, fromAct.getLink(), toAct.getLink());
		leg.setRoute(newRoute);
		
		String routeDescription = 
			fromStop.getId().toString() + " " + 
			fromMunicipality.getId().toString() + " " + 
			toMunicipality.getId().toString() + " " + 
			toStop.getId().toString();
		newRoute.setRouteDescription(fromAct.getLink(), routeDescription, toAct.getLink());
		
		newRoute.setDistance(
				(walkAccessEgressDistance + PlansCalcRouteKti.getInVehicleDistance(fromStop, fromMunicipality, toMunicipality, toStop)) * 1.5);
		
		travelTime = walkAccessEgressTime + timeInVehicle;
		leg.setTravelTime(travelTime);
		
		return travelTime;
	}

	public static double getTimeInVehicle(
			final String routeDescription, 
			final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		
		String[] routeDescriptionArray = StringUtils.explode(routeDescription, ' ');
		
		SwissHaltestelle fromStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[0]));
		Location fromMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[1]));
		Location toMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[2]));
		SwissHaltestelle toStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[3]));
		
		return PlansCalcRouteKti.getTimeInVehicle(fromStop, fromMunicipality, toMunicipality, toStop, plansCalcRouteKtiInfo);
		
	}
	
	public static double getTimeInVehicle(
			final SwissHaltestelle fromStop, 
			final Location fromMunicipality, 
			final Location toMunicipality, 
			final SwissHaltestelle toStop, 
			final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		
		Entry traveltime = plansCalcRouteKtiInfo.getPtTravelTimes().getEntry(fromMunicipality, toMunicipality);
		if (traveltime == null) {
			throw new RuntimeException("No entry found for " + fromMunicipality.getId() + " --> " + toMunicipality.getId());
		}
		
		return traveltime.getValue() * 60.0;
		
	}
	
	public static double getInVehicleDistance(
			final String routeDescription, 
			final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {

		String[] routeDescriptionArray = StringUtils.explode(routeDescription, ' ');
		
		SwissHaltestelle fromStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[0]));
		Location fromMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[1]));
		Location toMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[2]));
		SwissHaltestelle toStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[3]));

		return PlansCalcRouteKti.getInVehicleDistance(fromStop, fromMunicipality, toMunicipality, toStop);
		
	}
	
	public static double getInVehicleDistance(
			SwissHaltestelle fromStop, 
			Location fromMunicipality, 
			Location toMunicipality, 
			SwissHaltestelle toStop) {
		
		return CoordUtils.calcDistance(fromStop.getCoord(), toStop.getCoord());
		
	}

	public static double getAccessEgressDistance(
			String routeDescription, 
			ActivityImpl fromAct, 
			ActivityImpl toAct, 
			PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		
		String[] routeDescriptionArray = StringUtils.explode(routeDescription, ' ');
		SwissHaltestelle fromStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[0]));
		SwissHaltestelle toStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[3]));

		return PlansCalcRouteKti.getAccessEgressDistance(fromAct, fromStop, toStop, toAct);
		
	}
	
	public static double getAccessEgressDistance(ActivityImpl fromAct, SwissHaltestelle fromStop, SwissHaltestelle toStop, ActivityImpl toAct) {
		return 
		CoordUtils.calcDistance(fromAct.getCoord(), fromStop.getCoord()) + 
		CoordUtils.calcDistance(toAct.getCoord(), toStop.getCoord());
	}
	
	public static double getAccessEgressTime(final double distance, final PlansCalcRouteConfigGroup group) {
		return distance / group.getWalkSpeedFactor();
	}
	
}
