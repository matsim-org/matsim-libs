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
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;

import com.google.common.base.Verify;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public class DrtRoutingModule implements RoutingModule {
	private static final Logger logger = Logger.getLogger(DrtRoutingModule.class);

	public interface AccessEgressFacilityFinder {
		Optional<Pair<Facility, Facility>> findFacilities(Facility fromFacility, Facility toFacility);
	}

	private final DrtStageActivityType drtStageActivityType;
	private final Network modalNetwork;
	private final AccessEgressFacilityFinder stopFinder;
	private final DrtConfigGroup drtCfg;
	private final Scenario scenario;
	private final DrtRouteLegCalculator drtRouteLegCalculator;
	private final RoutingModule accessRouter;
	private final RoutingModule egressRouter;

	public DrtRoutingModule(DrtRouteLegCalculator drtRouteLegCalculator, RoutingModule accessRouter,
			RoutingModule egressRouter, AccessEgressFacilityFinder stopFinder, DrtConfigGroup drtCfg, Scenario scenario,
			Network modalNetwork) {
		this.drtRouteLegCalculator = drtRouteLegCalculator;
		this.stopFinder = stopFinder;
		this.drtCfg = drtCfg;
		this.scenario = scenario;
		this.drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());
		this.modalNetwork = modalNetwork;
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
			logger.debug("No access/egress stops found, agent will use fallback mode " + TripRouter.getFallbackMode(
					drtCfg.getMode()) + ". Agent Id:\t" + person.getId());
			return null;
		}

		Facility accessFacility = stops.get().getLeft();
		Facility egressFacility = stops.get().getRight();
		if (accessFacility.getLinkId().equals(egressFacility.getLinkId())) {
			logger.debug("Start and end stop are the same, agent will use fallback mode " + TripRouter.getFallbackMode(
					drtCfg.getMode()) + ". Agent Id:\t" + person.getId());
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
			trip.add(createDrtStageActivity(accessFacility));
			now++;
		}

		// drt proper leg:
		Link accessActLink = Verify.verifyNotNull(modalNetwork.getLinks().get(accessFacility.getLinkId()),
				"link: %s does not exist in the network of mode: %s", accessFacility.getLinkId(), drtCfg.getMode());
		Link egressActLink = Verify.verifyNotNull(modalNetwork.getLinks().get(egressFacility.getLinkId()),
				"link: %s does not exist in the network of mode: %s", egressFacility.getLinkId(), drtCfg.getMode());
		List<? extends PlanElement> drtLeg = drtRouteLegCalculator.createRealDrtLeg(now, accessActLink,
				egressActLink);
		trip.addAll(drtLeg);
		for (PlanElement planElement : drtLeg) {
			now = TripRouter.calcEndOfPlanElement(now, planElement, scenario.getConfig());
		}

		now++;
		List<? extends PlanElement> egressTrip = egressRouter.calcRoute(egressFacility, toFacility, now, person);
		if (!egressTrip.isEmpty()) {
			// interaction activity:
			trip.add(createDrtStageActivity(egressFacility));

			// egress (sub-)trip:
			trip.addAll(egressTrip);
		}

		return trip;
	}

	private Activity createDrtStageActivity(Facility stopFacility) {
		Activity activity = scenario.getPopulation()
				.getFactory()
				.createActivityFromCoord(drtStageActivityType.drtStageActivity, stopFacility.getCoord());
		activity.setMaximumDuration(0);
		activity.setLinkId(stopFacility.getLinkId());
		return activity;
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
