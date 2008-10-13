/* *********************************************************************** *
 * project: org.matsim.*
 * RouteFactory.java
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

import java.util.HashMap;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicLeg.Mode;

import playground.marcel.pt.interfaces.routes.BRoute;
import playground.marcel.pt.interfaces.routes.RouteParser;

public class RouteFactory {

	private final HashMap<BasicLeg.Mode, RouteParser> parsers = new HashMap<BasicLeg.Mode, RouteParser>(5);

	public void registerRouteParser(final Mode mode, final RouteParser parser) {
		parsers.put(mode, parser);
	}
	
	public RouteParser getRouteParserForMode(final Mode mode) {
		return parsers.get(mode);
	}
	
	public BRoute createRouteForMode(final Mode mode, final String stringRepresentation, final double travelTime) {
		RouteParser parser = getRouteParserForMode(mode);
		if (parser == null) {
			throw new IllegalArgumentException("No RouteParser registered for mode " + mode);
		}
		return parser.createRoute(stringRepresentation, travelTime);
	}
}
