/* *********************************************************************** *
 * project: org.matsim.*
 * CarRouteParser.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;

import playground.marcel.pt.interfaces.routes.CarRoute;
import playground.marcel.pt.interfaces.routes.RouteParser;

public class CarRouteParser implements RouteParser {

	public CarRoute createRoute(String stringRepresentation, final double travelTime) {
		String[] parts = stringRepresentation.trim().split("[ \t\n]+");
		Id depLink = null;//new IdImpl(parts[0]);
		Id arrLink = null;//new IdImpl(parts[1]);
		List<Id> links = new ArrayList<Id>();
		for (int i = 0/*2*/, n = parts.length; i < n; i++) {
			if (parts[i].length() > 0) {
				links.add(new IdImpl(parts[i]));
			}
		}
		
		CarRoute carRoute = new CarRouteImpl(depLink, links, arrLink, travelTime);
		return carRoute;
	}
}
