/* *********************************************************************** *
 * project: org.matsim.*
 * RouteImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.view.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.johannes.plans.plain.impl.PlainRouteImpl;
import playground.johannes.plans.view.Route;

/**
 * @author illenberger
 *
 */
public class RouteView extends AbstractView<PlainRouteImpl> implements Route {
	
	private List<Link> routeView;
	
	public RouteView(PlainRouteImpl delegate) {
		super(delegate);
	}
	
	public List<Link> getLinks() {
		synchronize();
		return routeView;
	}

	public void setLinks(List<Link> links) {
		List<Id> ids = new ArrayList<Id>(links.size() + 1);
		if (links.size() > 0) {
			for (int i = 0; i < links.size(); i++)
				ids.add(links.get(i).getFromNode().getId());
			ids.add(links.get(links.size() - 1).getToNode().getId());
		}
		delegate.setNodeIds(ids);
	}

	@Override
	protected void update() {
		List<Id> ids = delegate.getNodeIds();
		ArrayList<Link> links = new ArrayList<Link>(ids.size());
		for(int i = 0; i < ids.size()-1; i++) {
			Node node = IdMapping.getNode(ids.get(i));
			Node nextNode = IdMapping.getNode(ids.get(i+1));
			Link next = null;
			for(Link link : node.getOutLinks().values()) {
				if(link.getToNode().equals(nextNode)) {
					next = link;
					break;
				}
			}
			if(next == null)
				throw new NullPointerException();
			links.add(next);
			next = null;
		}
//		links.trimToSize();
		routeView = Collections.unmodifiableList(links);
	}

}
