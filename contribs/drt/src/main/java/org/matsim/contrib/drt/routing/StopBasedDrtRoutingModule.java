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
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.name.Named;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public class StopBasedDrtRoutingModule implements RoutingModule {
	private static final Logger logger = Logger.getLogger(StopBasedDrtRoutingModule.class);
	
	public interface AccessEgressStopFinder {
		Pair<TransitStopFacility, TransitStopFacility> findStops(Facility fromFacility, Facility toFacility);
	}

	private final DrtStageActivityType drtStageActivityType;
	private final Network modalNetwork;
	private final RoutingModule walkRouter;
	private final AccessEgressStopFinder stopFinder;
	private final DrtConfigGroup drtCfg;
	private final Scenario scenario;
	private final DrtRoutingModule drtRoutingModule;

	public StopBasedDrtRoutingModule(DrtRoutingModule drtRoutingModule,
			@Named(TransportMode.walk) RoutingModule walkRouter, AccessEgressStopFinder stopFinder,
			DrtConfigGroup drtCfg, Scenario scenario, Network modalNetwork) {
		this.drtRoutingModule = drtRoutingModule;
		this.walkRouter = walkRouter;
		this.stopFinder = stopFinder;
		this.drtCfg = drtCfg;
		this.scenario = scenario;
		this.drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());
		this.modalNetwork = modalNetwork;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		Pair<TransitStopFacility, TransitStopFacility> stops = stopFinder.findStops(fromFacility, toFacility);

		TransitStopFacility accessFacility = stops.getLeft();
		if (accessFacility == null) {
			printWarning(() -> "No access stop found, agent will use fallback mode " 
					+ TripRouter.getFallbackMode(drtCfg.getMode())
					+ ". Agent Id:\t"
					+ person.getId());
			return null;
		}

		TransitStopFacility egressFacility = stops.getRight();
		if (egressFacility == null) {
			printWarning(() -> "No egress stop found, agent will use fallback mode " 
					+ TripRouter.getFallbackMode(drtCfg.getMode())
					+ ". Agent Id:\t"
					+ person.getId());
			return null;
		}

		if (accessFacility.getLinkId().equals(egressFacility.getLinkId())) {
			if( drtCfg.isPrintDetailedWarnings() ){
				printWarning(() -> "Start and end stop are the same, agent will use fallback mode " 
						+ TripRouter.getFallbackMode(drtCfg.getMode())
						+ ". Agent Id:\t"
						+ person.getId());
			}
			return null;
		}

		List<PlanElement> trip = new ArrayList<>();

		double now = departureTime;

		// access leg:
		List<? extends PlanElement> accessWalk = createWalkTrip(fromFacility, accessFacility, now, person, TransportMode.non_network_walk );
		trip.addAll(accessWalk);
		for( PlanElement planElement : accessWalk ){
			now = TripRouter.calcEndOfPlanElement( now, planElement, scenario.getConfig() ) ;
		}

		// interaction activity:
		trip.add(createDrtStageActivity(accessFacility));

		// drt proper:
		now++ ;
		Link accessActLink = modalNetwork.getLinks().get(  accessFacility.getLinkId() ) ; // we want that this crashes if not found.  kai/gl, oct'19
		Link egressActLink = modalNetwork.getLinks().get(  egressFacility.getLinkId() ) ; // we want that this crashes if not found.  kai/gl, oct'19
		List<? extends PlanElement> drtLeg = drtRoutingModule.createRealDrtLeg( departureTime, accessActLink, egressActLink ) ;
		trip.addAll(drtLeg);
		for( PlanElement planElement : drtLeg ){
			now = TripRouter.calcEndOfPlanElement( now, planElement, scenario.getConfig() ) ;
		}

		// interaction activity:
		trip.add(createDrtStageActivity(egressFacility));

		// egress leg:
		now++ ;
		trip.addAll(createWalkTrip(egressFacility, toFacility, now, person, TransportMode.non_network_walk));

		return trip;
	}

	private List<? extends PlanElement> createWalkTrip(Facility fromFacility, Facility toFacility, double departureTime, Person person, String mode) {
		List<? extends PlanElement> result = walkRouter.calcRoute( fromFacility, toFacility, departureTime, person ); 
		// Overwrite real walk mode legs with non_network_walk / drt fallback mode
		for (PlanElement pe: result) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg.getMode().equals(TransportMode.walk)) {
					leg.setMode(mode);
				}
			}
		}

		return result ;
	}

	private Activity createDrtStageActivity(Facility stopFacility) {
		Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord(drtStageActivityType.drtStageActivity,
				stopFacility.getCoord());
		activity.setMaximumDuration(0);
		activity.setLinkId(stopFacility.getLinkId());
		return activity;
	}

	private void printWarning(Supplier<String> supplier) {
		if (drtCfg.isPrintDetailedWarnings()) {
			logger.warn(supplier.get());
		}
	}

	static Coord getFacilityCoord(Facility facility, Network network) {
		Coord coord = facility.getCoord();
		if (coord == null) {
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			if (coord == null)
				throw new RuntimeException("From facility has neither coordinates nor link Id. Should not happen.");
		}
		return coord;
	}
}
