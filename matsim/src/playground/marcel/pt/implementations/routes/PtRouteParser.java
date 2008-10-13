/* *********************************************************************** *
 * project: org.matsim.*
 * PtRouteParser.java
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

package playground.marcel.pt.implementations.routes;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;

import playground.marcel.pt.interfaces.routes.PtRoute;
import playground.marcel.pt.interfaces.routes.RouteParser;

public class PtRouteParser implements RouteParser {

	public PtRoute createRoute(final String stringRepresentation, final double travelTime) {
		String[] parts = stringRepresentation.trim().split("[ \t\n]+");

		if (parts.length != 3) {
			throw new IllegalArgumentException("Error parsing pt-route: " + stringRepresentation);
		}

		final Id departureId = new IdImpl(parts[0]);
		final Id arrivalId = new IdImpl(parts[1]);
		final Id lineId = new IdImpl(parts[2]);
		PtRoute ptRoute = new PtRouteImpl(lineId, departureId, arrivalId, travelTime);

		return ptRoute;
	}
}
