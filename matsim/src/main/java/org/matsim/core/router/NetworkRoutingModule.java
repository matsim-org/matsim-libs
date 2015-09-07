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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.Arrays;
import java.util.List;

/**
 * @thibautd
 */
public class NetworkRoutingModule implements RoutingModule {

	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network network;
	private final ModeRouteFactory routeFactory;
	private final LeastCostPathCalculator routeAlgo;


	 public NetworkRoutingModule(
			final String mode,
			final PopulationFactory populationFactory,
			final Network network,
			final LeastCostPathCalculator routeAlgo,
			final ModeRouteFactory routeFactory) {
		this.network = network;
		this.routeAlgo = routeAlgo;
		this.routeFactory = routeFactory;
		this.mode = mode;
		this.populationFactory = populationFactory;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Leg newLeg = populationFactory.createLeg( mode );
		newLeg.setDepartureTime( departureTime );

		double travTime = routeLeg(
				person,
				newLeg,
				new FacilityWrapper( fromFacility ),
				new FacilityWrapper( toFacility ),
				departureTime);

		// otherwise, information may be lost
		newLeg.setTravelTime( travTime );

		return Arrays.asList( newLeg );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode="+mode+"]";
	}

	private static class FacilityWrapper implements Activity {
		private final Facility wrapped;

		public FacilityWrapper(final Facility toWrap) {
			this.wrapped = toWrap;
		}

		@Override
		public double getEndTime() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setEndTime(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public String getType() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setType(String type) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public Coord getCoord() {
			return wrapped.getCoord();
		}

		@Override
		public double getStartTime() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setStartTime(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public double getMaximumDuration() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setMaximumDuration(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public Id<Link> getLinkId() {
			return wrapped.getLinkId();
		}

		@Override
		public Id<ActivityFacility> getFacilityId() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public String toString() {
			return "[FacilityWrapper: wrapped="+wrapped+"]";
		}
	}


	/*package (Tests)*/ double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		double travTime = 0;
		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		Link toLink = this.network.getLinks().get(toAct.getLinkId());

		/* Remove this and next three lines once debugged. */
		if(fromLink == null || toLink == null){
			Logger.getLogger(NetworkRoutingModule.class).error("  ==>  null from/to link for person " + person.getId().toString());
		}
		if (fromLink == null) throw new RuntimeException("fromLink "+fromAct.getLinkId()+" missing.");
		if (toLink == null) throw new RuntimeException("toLink "+toAct.getLinkId()+" missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

//		CarRoute route = null;
		Path path = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, null);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, this.network));
			leg.setRoute(route);
			travTime = (int) path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

}
