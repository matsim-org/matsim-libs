/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.ownMobsimAgentUsingRouter;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;

/**
 * @author nagel
 *
 */
class MyGuidance {
	
	private TripRouter router ;

	MyGuidance( TripRouter router ) {
		this.router = router ;
	}

	public Id<Link> getBestOutgoingLink(Id<Link> linkId, Id<Link> destinationLinkId, double now ) {
		Person person = null ; // does this work?
		double departureTime = now ;
		String mainMode = TransportMode.car ;
		Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(linkId);
		Facility<ActivityFacility> toFacility = new LinkWrapperFacility(destinationLinkId);
		List<? extends PlanElement> trip = router.calcRoute(mainMode, fromFacility, toFacility, departureTime, person) ;
		
		Leg leg = (Leg) trip.get(0) ;  // test: either plan element 0 or 1 will be a car leg
		
		NetworkRoute route = (NetworkRoute) leg.getRoute() ;
		
		return route.getLinkIds().get(0) ; // entry number 0 should be link connected to next intersection (?)
	}
	

	/*
	 * Wraps a Link into a Facility.
	 */
	private static class LinkWrapperFacility implements Facility<ActivityFacility> {
		
		private Id<Link> linkId;

		public LinkWrapperFacility(final Id<Link> linkId) {
			this.linkId = linkId ;
		}

		@Override
		public Coord getCoord() {
			throw new UnsupportedOperationException();
			// cannot say if this is needed inside the router; if so, then the link instead of the linkID needs to be passed to this class. kai, nov'14
		}

		@Override
		public Id<ActivityFacility> getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id<Link> getLinkId() {
			return this.linkId ;
		}

	}
}
