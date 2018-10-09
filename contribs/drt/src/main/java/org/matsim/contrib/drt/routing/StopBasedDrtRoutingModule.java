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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public class StopBasedDrtRoutingModule implements RoutingModule {
	private static final Logger LOGGER = Logger.getLogger(StopBasedDrtRoutingModule.class);

	public interface AccessEgressStopFinder {
		Pair<TransitStopFacility, TransitStopFacility> findStops(Facility fromFacility, Facility toFacility);
	}

	private final StageActivityTypes drtStageActivityType = new DrtStageActivityType();
	private final PopulationFactory populationFactory;
	private final RoutingModule walkRouter;
	private final AccessEgressStopFinder stopFinder;
	private final DrtConfigGroup drtCfg;
	private final DrtRoutingModule drtRoutingModule;

	@Inject
	public StopBasedDrtRoutingModule(PopulationFactory populationFactory, DrtRoutingModule drtRoutingModule,
			@Named(TransportMode.walk) RoutingModule walkRouter, AccessEgressStopFinder stopFinder,
			DrtConfigGroup drtCfg) {
		this.populationFactory = populationFactory;
		this.drtRoutingModule = drtRoutingModule;
		this.walkRouter = walkRouter;
		this.stopFinder = stopFinder;
		this.drtCfg = drtCfg;
	}

	@Override
	public List<? extends PlanElement> calcRoute( Facility fromFacility, Facility toFacility, double departureTime,
								    Person person) {
		Pair<TransitStopFacility, TransitStopFacility> stops = stopFinder.findStops(fromFacility, toFacility);

		TransitStopFacility accessFacility = stops.getLeft();
		if (accessFacility == null) {
			printError(() -> "No access stop found, agent will walk, using mode " + DrtStageActivityType.DRT_WALK + ". Agent Id:\t" + person.getId());
            return Collections.singletonList(createDrtWalkLeg(fromFacility, toFacility, departureTime, person));
		}

		TransitStopFacility egressFacility = stops.getRight();
		if (egressFacility == null) {
			printError(() -> "No egress stop found, agent will walk, using mode " + DrtStageActivityType.DRT_WALK + ". Agent Id:\t" + person.getId());
            return Collections.singletonList(createDrtWalkLeg(fromFacility, toFacility, departureTime, person));
		}

		if (accessFacility.getLinkId() == egressFacility.getLinkId()) {
			printError(() -> "Start and end stop are the same, agent will walk, using mode " + DrtStageActivityType.DRT_WALK + ". Agent Id:\t" + person.getId());
            return Collections.singletonList(createDrtWalkLeg(fromFacility, toFacility, departureTime, person));
		}

		List<PlanElement> trip = new ArrayList<>();

		Leg walkToAccessStopLeg = createDrtWalkLeg(fromFacility, accessFacility, departureTime, person);
		trip.add(walkToAccessStopLeg);

		trip.add(createDrtStageActivity(accessFacility));

		double drtLegStartTime = departureTime + walkToAccessStopLeg.getTravelTime() + 1;
		Leg drtLeg = (Leg)drtRoutingModule.calcRoute(accessFacility, egressFacility, drtLegStartTime, person).get(0);
		trip.add(drtLeg);

		trip.add(createDrtStageActivity(egressFacility));

		double walkFromStopStartTime = drtLeg.getDepartureTime() + drtLeg.getTravelTime() + 1;
		trip.add(createDrtWalkLeg(egressFacility, toFacility, walkFromStopStartTime, person));

		return trip;
	}

	private Leg createDrtWalkLeg( Facility fromFacility, Facility toFacility, double departureTime,
						Person person) {
		Leg leg = (Leg)walkRouter.calcRoute(fromFacility, toFacility, departureTime, person).get(0);
		leg.setMode(DrtStageActivityType.DRT_WALK);
		return leg;
	}

	private Activity createDrtStageActivity(Facility stopFacility) {
		Activity activity = populationFactory
				.createActivityFromCoord(DrtStageActivityType.DRT_STAGE_ACTIVITY, stopFacility.getCoord());
		activity.setMaximumDuration(1);
		activity.setLinkId(stopFacility.getLinkId());
		return activity;
	}

	private void printError(Supplier<String> supplier) {
		if (drtCfg.isPrintDetailedWarnings()) {
			Logger.getLogger(getClass()).error(supplier.get());
		}
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return drtStageActivityType;
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
