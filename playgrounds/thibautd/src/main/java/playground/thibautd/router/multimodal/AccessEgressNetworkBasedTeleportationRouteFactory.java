/* *********************************************************************** *
 * project: org.matsim.*
 * AccessEgressNetworkBasedTeleportationRouteFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.router.multimodal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.IdFactory;
import org.matsim.core.population.routes.RouteFactory;

/**
 * @author thibautd
 */
public class AccessEgressNetworkBasedTeleportationRouteFactory implements RouteFactory {
	private final IdFactory idFactory;

	public AccessEgressNetworkBasedTeleportationRouteFactory(IdFactory idFactory) {
		this.idFactory = idFactory;
	}

	@Override
	public Route createRoute(
			final Id startLinkId,
			final Id endLinkId) {
		return new AccessEgressNetworkBasedTeleportationRoute(
				idFactory,
				startLinkId,
				endLinkId );
	}
}

