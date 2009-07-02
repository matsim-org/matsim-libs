/* *********************************************************************** *
 * project: org.matsim.*
 * HierarchicalRouteProvider.java
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

package org.matsim.withinday.routeprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRoute;


/**
 * @author dgrether
 *
 */
public class HierarchicalRouteProvider extends AbstractRouteProvider {

	private static final Logger log = Logger.getLogger(HierarchicalRouteProvider.class);

	private List<RouteProvider> providers;

	private RouteProviderComparator comparator;

	public HierarchicalRouteProvider(final AStarLandmarksRouteProvider aStarProvider) {
		this.comparator = new RouteProviderComparator();
		this.providers = new Vector<RouteProvider>();
		this.providers.add(aStarProvider);
	}

	@Override
	public NetworkRoute requestRoute(LinkImpl departureLink, final LinkImpl destinationLink, final double time) {
		NetworkRoute subRoute;
		NetworkRoute returnRoute = (NetworkRoute) ((NetworkLayer) departureLink.getLayer()).getFactory().createRoute(TransportMode.car, departureLink, destinationLink);
		ArrayList<NodeImpl> routeNodes = new ArrayList<NodeImpl>();
		for (RouteProvider rp : this.providers) {
			if (log.isTraceEnabled()) {
				log.trace("Used RouteProvider class: " + rp.getClass());
			}
			subRoute = rp.requestRoute(departureLink, destinationLink, time);
			//in the first iteration of the loop we have to add all nodes
			if (routeNodes.isEmpty()) {
				routeNodes.addAll(subRoute.getNodes());
			}
			//next time we don't have to add the first node
			else {
				routeNodes.addAll(subRoute.getNodes().subList(1, subRoute.getNodes().size()));
			}
			returnRoute.setNodes(departureLink, routeNodes, destinationLink);
			if (isCompleteRoute(returnRoute, destinationLink)) {
				return returnRoute;
			}
			List<LinkImpl> returnRouteLinks = returnRoute.getLinks();
			departureLink = returnRouteLinks.get(returnRouteLinks.size()-1);
		}
		return null;
	}

	private boolean isCompleteRoute(final NetworkRoute subRoute, final LinkImpl destinationLink) {
		NodeImpl endNode = subRoute.getNodes().get(subRoute.getNodes().size() - 1);
		if (endNode.getOutLinks().containsKey(destinationLink.getId())) {
			return true;
		}
		return false;
	}

	/**
	 * @param routeProvider
	 */
	public void addRouteProvider(final RouteProvider routeProvider) {
		this.providers.add(routeProvider);
		Collections.sort(this.providers, Collections.reverseOrder(this.comparator));
	}

	public boolean removeRouteProvider(final RouteProvider routeProvider) {
		boolean ret = this.providers.remove(routeProvider);
		Collections.sort(this.providers,  Collections.reverseOrder(this.comparator));
		return ret;
	}

	/**
	 * As this implementation is backed by a AStarRouter the class provides Routes from everywhere to everywhere
	 * @see org.matsim.withinday.routeprovider.RouteProvider#providesRoute(org.matsim.core.network.LinkImpl, org.matsim.core.population.routes.NetworkRoute)
	 */
	public boolean providesRoute(final LinkImpl currentLink, final NetworkRoute subRoute) {
		return true;
	}

}
