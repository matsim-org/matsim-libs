/* *********************************************************************** *
 * project: org.matsim.*
 * LegImpl.java
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

import org.matsim.api.core.v01.TransportMode;

import playground.johannes.plans.plain.impl.PlainLegImpl;
import playground.johannes.plans.view.Leg;
import playground.johannes.plans.view.Route;

/**
 * @author illenberger
 *
 */
public class LegView extends PlanElementView<PlainLegImpl> implements Leg {

	private RouteView route;
	
	public LegView(PlainLegImpl rawLeg) {
		super(rawLeg);
	}
	
	public Route getRoute() {
		synchronize();
		return route;
	}

	public void setRoute(Route route) {
		delegate.setRoute(((RouteView)route).getDelegate());
		this.route = (RouteView) route;
	}

	@Override
	protected void update() {
//		route = new RouteView(delegate.getRoute());
		if(route == null)
			route = new RouteView(delegate.getRoute());
		else
			route.update();
	}

	public TransportMode getMode() {
		return delegate.getMode();
	}

	public void setMode(TransportMode mode) {
		delegate.setMode(mode);
	}

}
