/* *********************************************************************** *
 * project: org.matsim.*
 * PtRouteParserTest.java
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

import playground.marcel.pt.implementations.routes.PtRouteParser;
import playground.marcel.pt.interfaces.routes.PtRoute;

public class PtRouteParserTest extends MatsimTestCase {

	public void testCreateRoute() {
		final PtRouteParser parser = new PtRouteParser();
		
		final PtRoute route1 = parser.createRoute("3 7 4", 7.5*3600);
		assertEquals(7.5*3600.0, route1.getTravelTime(), EPSILON);
		assertEquals(new IdImpl(3), route1.getEnterStop());
		assertEquals(new IdImpl(7), route1.getExitStop());
		assertEquals(new IdImpl(4), route1.getLine());
	}
}
