/* *********************************************************************** *
 * project: org.matsim.*
 * CarRouteParserTest.java
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

import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.marcel.pt.implementations.routes.CarRouteParser;
import playground.marcel.pt.interfaces.routes.CarRoute;

public class CarRouteParserTest extends MatsimTestCase {

	public void testCreateRoute_simple() {
		final CarRouteParser parser = new CarRouteParser();
		
		final CarRoute route1 = parser.createRoute("3 7 4", 7.5*3600);
		assertEquals(7.5*3600.0, route1.getTravelTime(), EPSILON);
		assertEquals(null, route1.getDepartureLinkId());
		assertEquals(null, route1.getArrivalLinkId());
		assertEquals(3, route1.getLinkIds().size());
		assertEquals(new IdImpl(3), route1.getLinkIds().get(0));
		assertEquals(new IdImpl(7), route1.getLinkIds().get(1));
		assertEquals(new IdImpl(4), route1.getLinkIds().get(2));
	}
	
	public void testCreateRoute_withAdditionalSpaces() {
		final CarRouteParser parser = new CarRouteParser();
		
		final CarRoute route1 = parser.createRoute(" \t3\n 7\t \t4\n\t \n", 8.5*3600);
		assertEquals(8.5*3600.0, route1.getTravelTime(), EPSILON);
		assertEquals(null, route1.getDepartureLinkId());
		assertEquals(null, route1.getArrivalLinkId());
		assertEquals(3, route1.getLinkIds().size());
		assertEquals(new IdImpl(3), route1.getLinkIds().get(0));
		assertEquals(new IdImpl(7), route1.getLinkIds().get(1));
		assertEquals(new IdImpl(4), route1.getLinkIds().get(2));
	}
}
