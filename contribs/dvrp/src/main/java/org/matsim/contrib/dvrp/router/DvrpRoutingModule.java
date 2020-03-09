/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public class DvrpRoutingModule implements RoutingModule {
	private static final Logger logger = Logger.getLogger(DvrpRoutingModule.class);

	public interface AccessEgressFacilityFinder {
		Optional<Pair<Facility, Facility>> findFacilities(Facility fromFacility, Facility toFacility);
	}

	private final AccessEgressFacilityFinder stopFinder;
	private final String mode;
	private final Scenario scenario;
	private final RoutingModule mainRouter;
	private final RoutingModule accessRouter;
	private final RoutingModule egressRouter;

	public DvrpRoutingModule(RoutingModule mainRouter, RoutingModule accessRouter, RoutingModule egressRouter,
			AccessEgressFacilityFinder stopFinder, String mode, Scenario scenario) {
		this.mainRouter = mainRouter;
		this.stopFinder = stopFinder;
		this.mode = mode;
		this.scenario = scenario;
		this.accessRouter = accessRouter;
		this.egressRouter = egressRouter;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		Optional<Pair<Facility, Facility>> stops = stopFinder.findFacilities(
				Objects.requireNonNull(fromFacility, "fromFacility is null"),
				Objects.requireNonNull(toFacility, "toFacility is null"));
		if (!stops.isPresent()) {
			logger.debug("No access/egress stops found, agent will use fallback mode as leg mode (usually "
					+ TransportMode.walk
					+ ") and routing mode "
					+ mode
					+ ". Agent Id:\t"
					+ person.getId());
			return null;
		}

		Facility accessFacility = stops.get().getLeft();
		Facility egressFacility = stops.get().getRight();
		if (accessFacility.getLinkId().equals(egressFacility.getLinkId())) {
			logger.debug("Start and end stop are the same, agent will use fallback mode as leg mode (usually "
					+ TransportMode.walk
					+ ") and routing mode "
					+ mode
					+ ". Agent Id:\t"
					+ person.getId());
			return null;
		}

		List<PlanElement> trip = new ArrayList<>();

		double now = departureTime;

		// access (sub-)trip:
		List<? extends PlanElement> accessTrip = accessRouter.calcRoute(fromFacility, accessFacility, now, person);
		if (!accessTrip.isEmpty()) {
			trip.addAll(accessTrip);
			for (PlanElement planElement : accessTrip) {
				now = TripRouter.calcEndOfPlanElement(now, planElement, scenario.getConfig());
			}

			// interaction activity:
			trip.add(createDrtStageActivity(accessFacility, now));
			now++;
		}

		// dvrp proper leg:
		List<? extends PlanElement> drtLeg = mainRouter.calcRoute(accessFacility, egressFacility, now, person);
		trip.addAll(drtLeg);
		for (PlanElement planElement : drtLeg) {
			now = TripRouter.calcEndOfPlanElement(now, planElement, scenario.getConfig());
		}

		now++;
		List<? extends PlanElement> egressTrip = egressRouter.calcRoute(egressFacility, toFacility, now, person);
		if (!egressTrip.isEmpty()) {
			// interaction activity:
			trip.add(createDrtStageActivity(egressFacility, now));

			// egress (sub-)trip:
			trip.addAll(egressTrip);
		}

		return trip;
	}

	private Activity createDrtStageActivity(Facility stopFacility, double now) {
		Activity activity = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(stopFacility.getCoord(),
				stopFacility.getLinkId(), mode);
		return activity;
	}
}
