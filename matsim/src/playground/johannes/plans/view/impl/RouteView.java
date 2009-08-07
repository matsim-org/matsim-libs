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
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.johannes.plans.plain.impl.PlainRouteImpl;
import playground.johannes.plans.view.Route;

/**
 * @author illenberger
 *
 */
public class RouteView extends AbstractView<PlainRouteImpl> implements Route {
	
	private Map<Id, Link> linkMappings;
	
	private List<Link> routeView;
	
	public RouteView(PlainRouteImpl delegate) {
		super(delegate);
	}
	
	public List<Link> getLinks() {
		synchronize();
		return routeView;
	}

	public void setLinks(List<Link> links) {
		List<Id> ids = new ArrayList<Id>(links.size());
		for(int i = 0; i < links.size(); i++)
			ids.add(links.get(i).getId());
		
		delegate.setLinkIds(ids);
	}

	@Override
	protected void update() {
		List<Id> ids = delegate.getLinkIds();
		List<Link> links = new ArrayList<Link>(ids.size());
		for(int i = 0; i < ids.size(); i++) {
			links.add(linkMappings.get(ids.get(i)));
		}
		routeView = Collections.unmodifiableList(links);
	}

}
