/* *********************************************************************** *
 * project: org.matsim.*
 * RouteFactoryTest.java
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

package playground.marcel.pt.routes;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.testcases.MatsimTestCase;

import playground.marcel.pt.implementations.routes.CarRouteParser;
import playground.marcel.pt.implementations.routes.PtRouteParser;
import playground.marcel.pt.implementations.routes.RouteFactory;
import playground.marcel.pt.interfaces.routes.CarRoute;
import playground.marcel.pt.interfaces.routes.PtRoute;

public class RouteFactoryTest extends MatsimTestCase {

	static private final Logger log = Logger.getLogger(RouteFactoryTest.class);
	
	public void testCreateRouteForMode() {
		final RouteFactory factory = new RouteFactory();
		factory.registerRouteParser(Mode.car, new CarRouteParser());
		factory.registerRouteParser(Mode.pt, new PtRouteParser());
		assertTrue(factory.createRouteForMode(Mode.car, "2 6 3", 7.0*3600) instanceof CarRoute);
		assertTrue(factory.createRouteForMode(Mode.pt, "2 6 3", 7.0*3600) instanceof PtRoute);
	}
	
	public void testCreateRouteForMode_UnsupportedMode() {
		final RouteFactory factory = new RouteFactory();
		factory.registerRouteParser(Mode.car, new CarRouteParser());
		factory.registerRouteParser(Mode.pt, new PtRouteParser());
		assertNull(factory.getRouteParserForMode(Mode.bike));
		try {
			factory.createRouteForMode(Mode.bus, "3 2 1", 6.5*3500);
			fail("Requesting a route for an unknown mode should result in an Exception.");
		} catch (IllegalArgumentException expected) {
			log.debug("catched expected exception.", expected);
		}
		
	}
	
}
