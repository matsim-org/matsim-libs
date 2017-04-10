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

package playground.michalm.drt.routing;

import java.util.*;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.*;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;

import playground.michalm.drt.run.DrtConfigGroup;

public class DrtRoutingModule implements RoutingModule {
	private final DrtConfigGroup drtconfig;
	private StageActivityTypes stageActivityTypes;

	@Inject
	public DrtRoutingModule(Config config) {
		this.drtconfig = DrtConfigGroup.get(config);
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		Route route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		double distanceEstimate = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord())*drtconfig.getEstimatedBeelineDistanceFactor();
		route.setDistance(distanceEstimate);
		route.setTravelTime(distanceEstimate/drtconfig.getEstimatedDrtSpeed());
		
		Leg leg = PopulationUtils.createLeg(DrtConfigGroup.DRT_MODE);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(route.getTravelTime());
		leg.setRoute(route);
		if (fromFacility.getLinkId().equals(toFacility.getLinkId())){
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
