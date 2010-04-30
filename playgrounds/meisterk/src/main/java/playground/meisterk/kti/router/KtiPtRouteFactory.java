/* *********************************************************************** *
 * project: org.matsim.*
 * KtiPtRouteFactory.java
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

package playground.meisterk.kti.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactory;

public class KtiPtRouteFactory implements RouteFactory {

	private static final long serialVersionUID = 1L;

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;

	public KtiPtRouteFactory(PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		super();
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
	}

	@Override
	public Route createRoute(Id startLinkId, Id endLinkId) {
		return new KtiPtRoute(startLinkId, endLinkId, this.plansCalcRouteKtiInfo);
	}

}
