/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.southAfrica.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author droeder
 *
 */
public class PtSubModeDependRouterFactory implements TransitRouterFactory {
	
	private boolean routeOnSameMode;
	private Scenario sc;

	/**
	 * Factory to create the <code>PtSubModeDependendRouter</code>
	 * @param sc
	 * @param routeOnSameMode
	 */
	public PtSubModeDependRouterFactory(Scenario sc, boolean routeOnSameMode) {
		this.sc = sc;
		this.routeOnSameMode = routeOnSameMode;
	}

	@Override
	public TransitRouter createTransitRouter() {
		return new PtSubModeDependendRouter(this.sc, this.routeOnSameMode);
	}

}
