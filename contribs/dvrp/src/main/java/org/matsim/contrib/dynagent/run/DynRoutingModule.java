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

import java.util.*;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.facilities.Facility;

public class DynRoutingModule implements RoutingModule {
	private final String mode;
	private StageActivityTypes stageActivityTypes;

	public DynRoutingModule(String mode) {
		this.mode = mode;
	}

	@Override
	public List<? extends PlanElement> calcRoute( Facility fromFacility, Facility toFacility, double departureTime,
								    Person person) {
		Route route = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		route.setDistance(Double.NaN);
		route.setTravelTime(Double.NaN);
		
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(Double.NaN);
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
