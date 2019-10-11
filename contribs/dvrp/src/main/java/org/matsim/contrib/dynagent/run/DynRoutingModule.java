/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.NetworkRoutingInclAccessEgressModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;

public class DynRoutingModule implements RoutingModule {
	private final String stageActivityType;
	@Inject
	private Network network;
	@Inject
	private Population population;
	@Inject
	private PlansCalcRouteConfigGroup calcRouteConfig;

	private final String mode;

	public DynRoutingModule(String mode) {
		this.mode = mode;
		this.stageActivityType = mode + " interaction";
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		Link accessActLink = FacilitiesUtils.decideOnLink(Objects.requireNonNull(fromFacility), network);
		Link egressActLink = FacilitiesUtils.decideOnLink(Objects.requireNonNull(toFacility), network);

		List<PlanElement> result = new ArrayList<>();

		// access leg:
		if (calcRouteConfig.isInsertingAccessEgressWalk()) {
			departureTime += NetworkRoutingInclAccessEgressModule.addBushwhackingLegFromFacilityToLinkIfNecessary(
					fromFacility, person, accessActLink, departureTime, result, population.getFactory(),
					stageActivityType);
		}

		// leg proper:
		{
			Route route = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
			route.setDistance(Double.NaN);
			route.setTravelTime(Double.NaN);

			Leg leg = PopulationUtils.createLeg(mode);
			leg.setDepartureTime(departureTime);
			leg.setTravelTime(Double.NaN);
			leg.setRoute(route);
			if (fromFacility.getLinkId().equals(toFacility.getLinkId())) {
				leg.setMode(TransportMode.walk);
			}
			result.add(leg);
		}

		// egress leg:
		if (calcRouteConfig.isInsertingAccessEgressWalk()) {
			NetworkRoutingInclAccessEgressModule.addBushwhackingLegFromLinkToFacilityIfNecessary(toFacility, person,
					egressActLink, departureTime, result, population.getFactory(), stageActivityType);
		}

		return result;
	}

}
