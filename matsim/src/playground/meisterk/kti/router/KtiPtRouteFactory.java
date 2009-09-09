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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.population.routes.RouteWRefs;

public class KtiPtRouteFactory implements RouteFactory {

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;

	public KtiPtRouteFactory(PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		super();
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
	}

	public RouteWRefs createRoute(Link startLink, Link endLink) {
		return new KtiPtRoute(startLink, endLink, this.plansCalcRouteKtiInfo);
	}

}
