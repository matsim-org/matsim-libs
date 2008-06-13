/* *********************************************************************** *
 * project: org.matsim.*
 * PSLogitRouter.java
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

package org.matsim.utils.vis.routervis.multipathrouter;

import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.vis.routervis.RouterNetStateWriter;

public class PSLogitRouter extends MultiPathRouter {

	public PSLogitRouter(final NetworkLayer network, final TravelCostI costFunction,
			final TravelTimeI timeFunction, final RouterNetStateWriter writer) {
		super(network, costFunction, timeFunction, writer);
	}

	
	@Override
	void initSelector() {
		this.selector = new PSLogitSelector();
		
	}

}
