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
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.routes.CarRoute;
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

	private CarRoute currentRoute;

	private HierarchicalRouteProvider hierarchicalProvider;

	public CoopersRouteProvider(final AStarLandmarksRouteProvider aStarProvider,
			final List<VDSSign> signs) {
		this.hierarchicalProvider = new HierarchicalRouteProvider(aStarProvider);
		this.signs = signs;
	}

	/**
	 *
	 * @see org.matsim.withinday.routeprovider.RouteProvider#providesRoute(org.matsim.network.Link,
	 *      org.matsim.population.routes.CarRoute)
	 */
	public boolean providesRoute(final Link currentLink, final CarRoute subRoute) {
		for (VDSSign s : this.signs) {
			log.trace("signLink: " + s.getSignLink().getId() + " currentLInk: " + currentLink.getId());
			if (s.getSignLink().equals(currentLink) && containsLink(subRoute, s.getDirectionLinks())) {
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

	private boolean containsLink(final CarRoute subRoute, final Link directionLink) {
		// this should be the natural way of testing if a route contains a link
		for (Link l : subRoute.getLinks()) {
			if (l.equals(directionLink)) {
				return true;
			}
		}
		// as the link of the next activity is not returned by the
		// Route.getLinkRoute() method
		// we have to check if the last node of the subRoute has an outgoing link
		// which is equal to the direction link
		List<Node> route = subRoute.getNodes();
		for (Link link : route.get(route.size() - 1).getOutLinks().values()) {
			if (link.equals(directionLink)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public CarRoute requestRoute(final Link departureLink, final Link destinationLink, final double time) {
		if (this.currentRoute != null) {
			CurrentSignRouteProvider current = new CurrentSignRouteProvider(
					this.currentRoute);
			this.hierarchicalProvider.addRouteProvider(current);
			CarRoute ret = this.hierarchicalProvider.requestRoute(departureLink,
					destinationLink, time);
			this.hierarchicalProvider.removeRouteProvider(current);
			return ret;
		}
		throw new RuntimeException("requestRoute should never be called, if the sign doesn't provide a route!");
	}

	private static class CurrentSignRouteProvider implements RouteProvider {

		private CarRoute route;

		CurrentSignRouteProvider(final CarRoute route) {
			this.route = route;
		}

		public int getPriority() {
			return 10;
		}

		public boolean providesRoute(final Link currentLink, final CarRoute subRoute) {
			return true;
		}

		public CarRoute requestRoute(final Link departureLink,
				final Link destinationLink, final double time) {
			return this.route;
		}

		public void setPriority(final int p) {
		}
	}

}
