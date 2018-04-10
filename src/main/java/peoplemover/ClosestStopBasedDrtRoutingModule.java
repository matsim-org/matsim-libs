/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package peoplemover;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ClosestStopBasedDrtRoutingModule implements RoutingModule {

	private final StageActivityTypes drtStageActivityType = new DrtStageActivityType();
	private final RoutingModule walkRouter;
	private final Map<Id<TransitStopFacility>, TransitStopFacility> stops;
	private final DrtConfigGroup drtconfig;
	private final double walkBeelineFactor;
	private final Network network;
	private final Scenario scenario;

	/**
	 * 
	 */
	@Inject
	public ClosestStopBasedDrtRoutingModule(@Named(TransportMode.walk) RoutingModule walkRouter,
			@Named(TransportMode.drt) TransitSchedule transitSchedule, Scenario scenario) {
		transitSchedule.getFacilities();
		this.walkRouter = walkRouter;
		this.stops = transitSchedule.getFacilities();
		this.drtconfig = (DrtConfigGroup)scenario.getConfig().getModules().get(DrtConfigGroup.GROUP_NAME);
		this.walkBeelineFactor = scenario.getConfig().plansCalcRoute().getModeRoutingParams().get(TransportMode.walk)
				.getBeelineDistanceFactor();
		this.network = scenario.getNetwork();
		this.scenario = scenario;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		List<PlanElement> legList = new ArrayList<>();
		TransitStopFacility accessFacility = findAccessFacility(fromFacility);
		if (accessFacility == null) {
			if (drtconfig.isPrintDetailedWarnings()){
			Logger.getLogger(getClass()).error("No access stop found, agent will walk. Agent Id:\t" + person.getId());}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));
		}
		TransitStopFacility egressFacility = findEgressFacility(toFacility);
		if (egressFacility == null) {
			if (drtconfig.isPrintDetailedWarnings()){
			Logger.getLogger(getClass()).error("No egress stop found, agent will walk. Agent Id:\t" + person.getId());}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));
		}
		legList.addAll(walkRouter.calcRoute(fromFacility, accessFacility, departureTime, person));
		Leg walkLeg = (Leg)legList.get(0);
		Activity drtInt1 = scenario.getPopulation().getFactory()
				.createActivityFromCoord(DrtStageActivityType.DRT_STAGE_ACTIVITY, accessFacility.getCoord());
		drtInt1.setMaximumDuration(1);
		drtInt1.setLinkId(accessFacility.getLinkId());
		legList.add(drtInt1);

		Route drtRoute = RouteUtils.createGenericRouteImpl(accessFacility.getLinkId(), egressFacility.getLinkId());
		drtRoute.setDistance(drtconfig.getEstimatedBeelineDistanceFactor()
				* CoordUtils.calcEuclideanDistance(accessFacility.getCoord(), egressFacility.getCoord()));
		drtRoute.setTravelTime(drtRoute.getDistance() / drtconfig.getEstimatedDrtSpeed());

		if (drtRoute.getStartLinkId() == drtRoute.getEndLinkId()) {
			if (drtconfig.isPrintDetailedWarnings()){
			Logger.getLogger(getClass()).error("Start and end stop are the same, agent will walk. Agent Id:\t" + person.getId());
			}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));

		}

		Leg drtLeg = PopulationUtils.createLeg(TransportMode.drt);
		drtLeg.setDepartureTime(departureTime + walkLeg.getTravelTime() + 1);
		drtLeg.setTravelTime(drtRoute.getTravelTime());
		drtLeg.setRoute(drtRoute);

		legList.add(drtLeg);

		Activity drtInt2 = scenario.getPopulation().getFactory()
				.createActivityFromCoord(DrtStageActivityType.DRT_STAGE_ACTIVITY, egressFacility.getCoord());
		drtInt2.setMaximumDuration(1);
		drtInt2.setLinkId(egressFacility.getLinkId());
		legList.add(drtInt2);
		legList.addAll(walkRouter.calcRoute(egressFacility, toFacility,
				drtLeg.getDepartureTime() + drtLeg.getTravelTime() + 1, person));
		return legList;
	}

	/**
	 * @param fromFacility
	 * @param toFacility
	 * @return
	 */
	private TransitStopFacility findAccessFacility(Facility<?> fromFacility) {
		Coord fromCoord = getFacilityCoord(fromFacility);
		TransitStopFacility accessFacility = findClosestStop(fromCoord);
		
		return accessFacility;
	}

	private TransitStopFacility findEgressFacility(Facility<?> toFacility) {
		Coord toCoord = getFacilityCoord(toFacility);
		TransitStopFacility stop = findClosestStop(toCoord);
		return stop;

	}

	
	private TransitStopFacility findClosestStop(Coord coord) {
		TransitStopFacility bestStop = null;
		double bestDist = Double.MAX_VALUE;
		for (TransitStopFacility stop : this.stops.values()) {
			double distance = walkBeelineFactor * CoordUtils.calcEuclideanDistance(coord, stop.getCoord());
			if (distance <= drtconfig.getMaxWalkDistance()) {
				if (distance<bestDist){
					bestDist = distance;
					bestStop = stop;
				}
				
			}

		}
		return bestStop;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return drtStageActivityType;
	}

	Coord getFacilityCoord(Facility<?> facility) {
		Coord coord = facility.getCoord();
		if (coord == null) {
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			if (coord == null)
				throw new RuntimeException("From facility has neither coordinates nor link Id. Should not happen.");
		}
		return coord;
	}

	

}
