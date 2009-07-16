/* *********************************************************************** *
 * project: org.matsim.*
 * NodeCarRouteTest.java
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

package org.matsim.core.population.routes;

import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.network.NetworkLayer;

/**
 * @author mrieser
 */
public class NodeCarRouteTest extends AbstractNetworkRouteTest {

	public NetworkRoute getCarRouteInstance(final Link fromLink, final Link toLink, NetworkLayer network) {
		return new NodeNetworkRoute(fromLink, toLink);
	}
	
}
