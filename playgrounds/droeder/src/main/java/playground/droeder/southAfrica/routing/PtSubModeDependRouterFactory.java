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
package playground.droeder.southAfrica.routing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterFactory;

/**
 * @author droeder
 *
 */
public class PtSubModeDependRouterFactory implements TransitRouterFactory{
	
	private boolean routeOnSameMode;
	private Scenario sc;

	/**
	 * Factory to create the <code>PtSubModeDependendRouter</code>
	 * @param sc
	 * @param routeOnSameMode
	 */
	public PtSubModeDependRouterFactory(Controler c, boolean routeOnSameMode) {
//		super(c);
		this.sc = c.getScenario();
		this.routeOnSameMode = routeOnSameMode;
	}
	// TODO[dr] create RouterNetworks only once per iteration and add them here to the router!
	public TransitRouter createTransitRouter() {
		return new PtSubModeDependendRouter(this.sc, this.routeOnSameMode);
	}

}
