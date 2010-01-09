/* *********************************************************************** *
 * project: org.matsim.*
 * CoopersRouteProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.coopers.routeprovider;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.withinday.routeprovider.AStarLandmarksRouteProvider;
import org.matsim.withinday.routeprovider.AbstractRouteProvider;
import org.matsim.withinday.routeprovider.HierarchicalRouteProvider;
import org.matsim.withinday.routeprovider.RouteProvider;
import org.matsim.withinday.trafficmanagement.VDSSign;

/**
 * @author dgrether
 */
public class CoopersRouteProvider extends AbstractRouteProvider {

	private static final Logger log = Logger.getLogger(CoopersRouteProvider.class);

	private List<VDSSign> signs;

//	private VDSSign currentSign;

	private NetworkRouteWRefs currentRoute;

	private HierarchicalRouteProvider hierarchicalProvider;

	public CoopersRouteProvider(final AStarLandmarksRouteProvider aStarProvider,
			final List<VDSSign> signs) {
		this.hierarchicalProvider = new HierarchicalRouteProvider(aStarProvider);
		this.signs = signs;
	}

	public boolean providesRoute(final Id currentLinkId, final NetworkRouteWRefs subRoute) {
		for (VDSSign s : this.signs) {
			log.trace("signLink: " + s.getSignLink().getId() + " currentLInk: " + currentLinkId);
			if (s.getSignLink().getId().equals(currentLinkId) && containsLink(subRoute, s.getDirectionLinks())) {
				this.currentRoute = s.requestRoute();
				if (this.currentRoute == null) {
					log.trace("Sign is currently switched off!");
					return false;
				}
				log.trace("Sign provides route!");
				return true;
			}
		}
		return false;

	}

	private boolean containsLink(final NetworkRouteWRefs subRoute, final Link directionLink) {
		// this should be the natural way of testing if a route contains a link
		for (Id linkId : subRoute.getLinkIds()) {
			if (linkId.equals(directionLink.getId())) {
				return true;
			}
		}
		// as the link of the next activity is not returned by the
		// Route.getLinkRoute() method
		// we have to check if the last node of the subRoute has an outgoing link
		// which is equal to the direction link
		List<Node> route = subRoute.getNodes();
		return route.get(route.size() - 1) == directionLink.getFromNode();
	}

	@Override
	public NetworkRouteWRefs requestRoute(final Link departureLink, final Link destinationLink, final double time) {
		if (this.currentRoute != null) {
			CurrentSignRouteProvider current = new CurrentSignRouteProvider(
					this.currentRoute);
			this.hierarchicalProvider.addRouteProvider(current);
			NetworkRouteWRefs ret = this.hierarchicalProvider.requestRoute(departureLink,
					destinationLink, time);
			this.hierarchicalProvider.removeRouteProvider(current);
			return ret;
		}
		throw new RuntimeException("requestRoute should never be called, if the sign doesn't provide a route!");
	}

	private static class CurrentSignRouteProvider implements RouteProvider {

		private NetworkRouteWRefs route;

		CurrentSignRouteProvider(final NetworkRouteWRefs route) {
			this.route = route;
		}

		@Override
		public int getPriority() {
			return 10;
		}

		@Override
		public boolean providesRoute(final Id currentLinkId, final NetworkRouteWRefs subRoute) {
			return true;
		}

		@Override
		public NetworkRouteWRefs requestRoute(final Link departureLink,
				final Link destinationLink, final double time) {
			return this.route;
		}

		@Override
		public void setPriority(final int p) {
		}
	}

}
