/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFactoryTest.java
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

package org.matsim.core.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class NetworkFactoryTest extends MatsimTestCase {

	public void testSetRouteFactory() {
        PopulationFactoryImpl factory = (PopulationFactoryImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();

		// test default
		Route carRoute = factory.createRoute(NetworkRoute.class, null, null);
		assertTrue(carRoute instanceof LinkNetworkRouteImpl);

		Route route = factory.createRoute(Route.class, null, null);
		assertTrue(route instanceof GenericRouteImpl);

		// overwrite car-mode
		factory.setRouteFactory(CarRouteMock.class, new CarRouteMockFactory());
		// add pt-mode
		factory.setRouteFactory(PtRouteMock.class, new PtRouteMockFactory());

		// test car-mode
		carRoute = factory.createRoute(CarRouteMock.class, null, null);
		assertTrue(carRoute instanceof CarRouteMock);

		// add pt-mode
		Route ptRoute = factory.createRoute(PtRouteMock.class, null, null);
		assertTrue(ptRoute instanceof PtRouteMock);

		// remove pt-mode
		factory.setRouteFactory(PtRouteMock.class, null);

		// test pt again
		route = factory.createRoute(PtRouteMock.class, null, null);
		assertTrue(route instanceof GenericRouteImpl);
	}

	/*package*/ static class CarRouteMock extends AbstractRoute implements Cloneable {
		CarRouteMock(final Id<Link> startLinkId, final Id<Link> endLinkId) {
			super(startLinkId, endLinkId);
		}
		@Override
		public CarRouteMock clone() {
			return (CarRouteMock) super.clone();
		}
		@Override
		public String getRouteDescription() {
			return null;
		}
		@Override
		public void setRouteDescription(String routeDescription) {
		}
		@Override
		public String getRouteType() {
			return null;
		}
	}

	/*package*/ static class PtRouteMock extends AbstractRoute implements Cloneable {
		PtRouteMock(final Id<Link> startLinkId, final Id<Link> endLinkId) {
			super(startLinkId, endLinkId);
		}
		@Override
		public PtRouteMock clone() {
			return (PtRouteMock) super.clone();
		}
		@Override
		public String getRouteDescription() {
			return null;
		}
		@Override
		public void setRouteDescription(String routeDescription) {
		}
		@Override
		public String getRouteType() {
			return null;
		}
	}

	/*package*/ static class CarRouteMockFactory implements RouteFactory {
		@Override
		public Route createRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
			return new CarRouteMock(startLinkId, endLinkId);
		}
		
		@Override
		public String getCreatedRouteType() {
			return "carMock";
		}

	}

	/*package*/ static class PtRouteMockFactory implements RouteFactory {
		@Override
		public Route createRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
			return new PtRouteMock(startLinkId, endLinkId);
		}
		
		@Override
		public String getCreatedRouteType() {
			return "ptMock";
		}

	}
}
