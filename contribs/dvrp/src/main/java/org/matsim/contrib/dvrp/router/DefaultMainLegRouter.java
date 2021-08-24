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

package org.matsim.contrib.dvrp.router;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DefaultMainLegRouter implements RoutingModule {
	public interface RouteCreator {
		/**
		 * Creates a route based on basic trip characteristics (departure time, origin
		 * and destination location) and potential person- or trip-based requirements.
		 */
		Route createRoute(double departureTime, Link accessActLink, Link egressActLink, Person person,
				Attributes tripAttributes, RouteFactories routeFactories);
	}

	private final String mode;
	private final PopulationFactory populationFactory;
	private final Network modalNetwork;
	private final RouteCreator routeCreator;

	public DefaultMainLegRouter(String mode, Network modalNetwork, PopulationFactory populationFactory,
			RouteCreator routeCreator) {
		this.mode = mode;
		this.populationFactory = populationFactory;
		this.modalNetwork = modalNetwork;
		this.routeCreator = routeCreator;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		
		Link accessActLink = Preconditions.checkNotNull(modalNetwork.getLinks().get(fromFacility.getLinkId()),
				"link: %s does not exist in the network of mode: %s", fromFacility.getLinkId(), mode);
		Link egressActLink = Preconditions.checkNotNull(modalNetwork.getLinks().get(toFacility.getLinkId()),
				"link: %s does not exist in the network of mode: %s", toFacility.getLinkId(), mode);
		Route route = routeCreator.createRoute(departureTime, accessActLink, egressActLink,
				request.getPerson(), request.getAttributes(), populationFactory.getRouteFactories());

		Leg leg = populationFactory.createLeg(mode);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(route.getTravelTime().seconds());
		leg.setRoute(route);
		return ImmutableList.of(leg);
	}
}
