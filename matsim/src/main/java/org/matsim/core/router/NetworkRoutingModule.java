/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 *
 * @author thibautd
 */
@Deprecated // use NetworkRoutingInclAccessEgressModule instead
public final class NetworkRoutingModule implements RoutingModule {
	// I think it makes sense to NOT add the bushwhacking mode directly into here ...
	// ... since it makes sense be able to to route from facility.getLinkId() to facility.getLinkId(). kai, dec'15

	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network network;
	private final LeastCostPathCalculator routeAlgo;


	 public NetworkRoutingModule(
			final String mode,
			final PopulationFactory populationFactory,
			final Network network,
			final LeastCostPathCalculator routeAlgo) {
		 Gbl.assertNotNull(network);
//		 Gbl.assertIf( network.getLinks().size()>0 ) ; // otherwise network for mode probably not defined
		 // makes many tests fail.
		 this.network = network;
		 this.routeAlgo = routeAlgo;
		 this.mode = mode;
		 this.populationFactory = populationFactory;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();

		Leg newLeg = this.populationFactory.createLeg( this.mode );

		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);

		Link fromLink = this.network.getLinks().get(fromFacility.getLinkId());
		if ( fromLink==null ) {
			//if an activity takes place on a link which is not part of the modal network, use coord as fallback
			Gbl.assertNotNull( fromFacility.getCoord() ) ;
			fromLink = NetworkUtils.getNearestLink( network, fromFacility.getCoord()) ;
		}
		Link toLink = this.network.getLinks().get(toFacility.getLinkId());
		if ( toLink==null ) {
			//if an activity takes place on a link which is not part of the modal network, use coord as fallback
			Gbl.assertNotNull( toFacility.getCoord() ) ;
			toLink = NetworkUtils.getNearestLink(network, toFacility.getCoord());
		}
		Gbl.assertNotNull(fromLink);
		Gbl.assertNotNull(toLink);

		// use vehicle from routing request attribute, if defined
		Id<Vehicle> vehicleId = (Id<Vehicle>) request.getAttributes().getAttribute(DefaultRoutingRequest.ATTRIBUTE_VEHICLE_ID);
		if (vehicleId == null) {
			vehicleId = VehicleUtils.getVehicleId(person, mode);
		}

		if (toLink != fromLink) {
			// (a "true" route)

			Path path = this.routeAlgo.calcLeastCostPath(fromLink, toLink, departureTime, person, null);
			if (path == null)
				throw new RuntimeException("No route found from link " + fromLink.getId() + " to link " + toLink.getId() + " by mode " + this.mode + ".");
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime(path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, this.network));
			route.setVehicleId(vehicleId);
			newLeg.setRoute(route);
			newLeg.setTravelTime(path.travelTime);
		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			route.setVehicleId(vehicleId);
			newLeg.setRoute(route);
			newLeg.setTravelTime(0);
		}
		newLeg.setDepartureTime(departureTime);

		return Arrays.asList( newLeg );
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode="+this.mode+"]";
	}

}
