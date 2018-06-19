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

package org.matsim.contrib.drt.routing;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;

public class DrtRoutingModule implements RoutingModule {
	private final DrtConfigGroup drtconfig;
	private StageActivityTypes stageActivityTypes;
	private final Network network;

	@Inject
	public DrtRoutingModule(Config config, Network network) {
		this.drtconfig = DrtConfigGroup.get(config);
		this.network = network;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		Route route = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		Coord fromCoord = fromFacility.getCoord();
		Coord toCoord = toFacility.getCoord();

		if (fromCoord == null) {
			fromCoord = network.getLinks().get(fromFacility.getLinkId()).getCoord();
		}
		if (toCoord == null) {
			toCoord = network.getLinks().get(toFacility.getLinkId()).getCoord();
		}
		double distanceEstimate = CoordUtils.calcEuclideanDistance(fromCoord, toCoord)
				* drtconfig.getEstimatedBeelineDistanceFactor();
		route.setDistance(distanceEstimate);
		route.setTravelTime(distanceEstimate / drtconfig.getEstimatedDrtSpeed());

		Leg leg = PopulationUtils.createLeg(TransportMode.drt);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(route.getTravelTime());
		leg.setRoute(route);
		if (fromFacility.getLinkId().equals(toFacility.getLinkId())) {
			leg.setMode(TransportMode.walk);
		}

		return Collections.singletonList(leg);
	}

	/**
	 * @param stageActivityTypes
	 *            the stageActivityTypes to set
	 */
	public void setStageActivityTypes(StageActivityTypes stageActivityTypes) {
		this.stageActivityTypes = stageActivityTypes;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return this.stageActivityTypes != null ? this.stageActivityTypes : EmptyStageActivityTypes.INSTANCE;
	}
}
