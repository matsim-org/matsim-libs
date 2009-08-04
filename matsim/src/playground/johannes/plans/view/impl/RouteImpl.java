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
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.johannes.plans.plain.impl.PlainRouteImpl;
import playground.johannes.plans.view.Route;

/**
 * @author illenberger
 *
 */
public class RouteImpl extends AbstractView<PlainRouteImpl> implements Route {
	

	private Map<String, Link> linkMappings;
	
	private List<Link> routeView;
	
	public RouteImpl(PlainRouteImpl delegate) {
		super(delegate);
	}
	
	public List<Link> getLinks() {
//		updateView();
		return routeView;
	}

	public List<String> getLinkIds() {
		return delegate.getLinkIds();
	}
	
//	private void updateView() {
//		if(lastModCount < delegate.getModCount()) {
//			List<Link> newRoute = new ArrayList<Link>();
//			
//			for(String id : getLinkIds())
//				newRoute.add(linkMappings.get(id));
//			
//			routeView = Collections.unmodifiableList(newRoute);
//			
//			lastModCount = delegate.getModCount();
//		}
//	}

	public void setLinkIds(List<String> linkIds) {
		delegate.setLinkIds(linkIds);	
	}

	public void setLinks(List<Link> links) {
		List<String> ids = new ArrayList<String>(links.size());
		for(int i = 0; i < links.size(); i++)
			ids.add(links.get(i).getId().toString());
		
		delegate.setLinkIds(ids);
	}

	/* (non-Javadoc)
	 * @see playground.johannes.plans.view.impl.AbstractView#update()
	 */
	@Override
	protected void update() {
		// TODO Auto-generated method stub
		
	}

}
