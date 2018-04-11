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
package org.matsim.contrib.drt.routing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author jbischoff
 */
public class StopBasedDrtRoutingModule implements RoutingModule {
	public interface AccessEgressStopFinder {
		Pair<TransitStopFacility, TransitStopFacility> findStops(Facility<?> fromFacility, Facility<?> toFacility);
	}

	private final StageActivityTypes drtStageActivityType = new DrtStageActivityType();
	private final PopulationFactory populationFactory;
	private final RoutingModule walkRouter;
	private final AccessEgressStopFinder stopFinder;
	private final DrtConfigGroup drtconfig;

	@Inject
	public StopBasedDrtRoutingModule(PopulationFactory populationFactory,
			@Named(TransportMode.walk) RoutingModule walkRouter, AccessEgressStopFinder stopFinder,
			DrtConfigGroup drtCfg) {
		this.populationFactory = populationFactory;
		this.walkRouter = walkRouter;
		this.stopFinder = stopFinder;
		this.drtconfig = drtCfg;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		List<PlanElement> legList = new ArrayList<>();
		Pair<TransitStopFacility, TransitStopFacility> stops = stopFinder.findStops(fromFacility, toFacility);
		TransitStopFacility accessFacility = stops.getLeft();
		if (accessFacility == null) {
			if (drtconfig.isPrintDetailedWarnings()) {
				Logger.getLogger(getClass())
						.error("No access stop found, agent will walk. Agent Id:\t" + person.getId());
			}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));
		}
		TransitStopFacility egressFacility = stops.getRight();
		if (egressFacility == null) {
			if (drtconfig.isPrintDetailedWarnings()) {
				Logger.getLogger(getClass())
						.error("No egress stop found, agent will walk. Agent Id:\t" + person.getId());
			}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));
		}
		legList.addAll(walkRouter.calcRoute(fromFacility, accessFacility, departureTime, person));
		Leg walkLeg = (Leg)legList.get(0);
		Activity drtInt1 = populationFactory.createActivityFromCoord(DrtStageActivityType.DRT_STAGE_ACTIVITY,
				accessFacility.getCoord());
		drtInt1.setMaximumDuration(1);
		drtInt1.setLinkId(accessFacility.getLinkId());
		legList.add(drtInt1);

		Route drtRoute = RouteUtils.createGenericRouteImpl(accessFacility.getLinkId(), egressFacility.getLinkId());
		drtRoute.setDistance(drtconfig.getEstimatedBeelineDistanceFactor()
				* CoordUtils.calcEuclideanDistance(accessFacility.getCoord(), egressFacility.getCoord()));
		drtRoute.setTravelTime(drtRoute.getDistance() / drtconfig.getEstimatedDrtSpeed());

		if (drtRoute.getStartLinkId() == drtRoute.getEndLinkId()) {
			if (drtconfig.isPrintDetailedWarnings()) {
				Logger.getLogger(getClass())
						.error("Start and end stop are the same, agent will walk. Agent Id:\t" + person.getId());
			}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));

		}

		Leg drtLeg = PopulationUtils.createLeg(TransportMode.drt);
		drtLeg.setDepartureTime(departureTime + walkLeg.getTravelTime() + 1);
		drtLeg.setTravelTime(drtRoute.getTravelTime());
		drtLeg.setRoute(drtRoute);

		legList.add(drtLeg);

		Activity drtInt2 = populationFactory.createActivityFromCoord(DrtStageActivityType.DRT_STAGE_ACTIVITY,
				egressFacility.getCoord());
		drtInt2.setMaximumDuration(1);
		drtInt2.setLinkId(egressFacility.getLinkId());
		legList.add(drtInt2);
		legList.addAll(walkRouter.calcRoute(egressFacility, toFacility,
				drtLeg.getDepartureTime() + drtLeg.getTravelTime() + 1, person));
		for (PlanElement pE : legList) {
			if (pE instanceof Leg) {
				if (((Leg)pE).getMode().equals(TransportMode.walk)) {
					((Leg)pE).setMode(DrtStageActivityType.DRT_WALK);
				}
			}
		}

		return legList;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return drtStageActivityType;
	}

	static Coord getFacilityCoord(Facility<?> facility, Network network) {
		Coord coord = facility.getCoord();
		if (coord == null) {
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			if (coord == null)
				throw new RuntimeException("From facility has neither coordinates nor link Id. Should not happen.");
		}
		return coord;
	}
}
