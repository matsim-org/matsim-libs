/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.routedistance;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;

/**
 * @author mrieser
 */
public class RouteDistanceCalculatorManagerTest {

	private final static Logger log = Logger.getLogger(RouteDistanceCalculatorManagerTest.class);

	@Test
	public void testSetGetCalculator() {
		RouteDistanceCalculatorManager manager = new RouteDistanceCalculatorManager();
		CountingRouteDistanceCalculator counterA = new CountingRouteDistanceCalculator();
		CountingRouteDistanceCalculator counterB = new CountingRouteDistanceCalculator();
		CountingRouteDistanceCalculator counterC = new CountingRouteDistanceCalculator();

		// test init / default
		Assert.assertNull(manager.getCalculator(Route.class));
		Assert.assertNull(manager.getCalculator(DummyRouteA.class));
		Assert.assertNull(manager.getCalculator(DummyRouteB.class));
		Assert.assertNull(manager.getCalculator(DummyRouteC.class));

		// register A, and implicitly C
		manager.setCalculator(DummyRouteA.class, counterA);
		Assert.assertNull(manager.getCalculator(Route.class));
		Assert.assertEquals(counterA, manager.getCalculator(DummyRouteA.class));
		Assert.assertNull(manager.getCalculator(DummyRouteB.class));
		Assert.assertEquals(counterA, manager.getCalculator(DummyRouteC.class));

		// register B
		manager.setCalculator(DummyRouteB.class, counterB);
		Assert.assertNull(manager.getCalculator(Route.class));
		Assert.assertEquals(counterA, manager.getCalculator(DummyRouteA.class));
		Assert.assertEquals(counterB, manager.getCalculator(DummyRouteB.class));
		Assert.assertEquals(counterA, manager.getCalculator(DummyRouteC.class));

		// register C
		manager.setCalculator(DummyRouteC.class, counterC);
		Assert.assertNull(manager.getCalculator(Route.class));
		Assert.assertEquals(counterA, manager.getCalculator(DummyRouteA.class));
		Assert.assertEquals(counterB, manager.getCalculator(DummyRouteB.class));
		Assert.assertEquals(counterC, manager.getCalculator(DummyRouteC.class));
	}

	@Test
	public void testCalcDistance() {
		RouteDistanceCalculatorManager manager = new RouteDistanceCalculatorManager();
		CountingRouteDistanceCalculator counterA = new CountingRouteDistanceCalculator();
		CountingRouteDistanceCalculator counterB = new CountingRouteDistanceCalculator();
		CountingRouteDistanceCalculator counterC = new CountingRouteDistanceCalculator();
		Route rA1 = new DummyRouteA();
		Route rA2 = new DummyRouteA();
		Route rB = new DummyRouteB();
		Route rC1 = new DummyRouteC();
		Route rC2 = new DummyRouteC();

		manager.setCalculator(DummyRouteA.class, counterA);
		manager.setCalculator(DummyRouteB.class, counterB);

		Assert.assertEquals(0, counterA.invocationCount);
		Assert.assertEquals(0, counterB.invocationCount);

		manager.calcDistance(rA1);
		Assert.assertEquals(1, counterA.invocationCount);
		Assert.assertEquals(0, counterB.invocationCount);

		manager.calcDistance(rB);
		Assert.assertEquals(1, counterA.invocationCount);
		Assert.assertEquals(1, counterB.invocationCount);

		manager.calcDistance(rC1);
		Assert.assertEquals(2, counterA.invocationCount);
		Assert.assertEquals(1, counterB.invocationCount);
		Assert.assertEquals(0, counterC.invocationCount);
		Assert.assertEquals(rC1, counterA.lastRoute);

		manager.setCalculator(DummyRouteC.class, counterC);
		manager.calcDistance(rC2);
		Assert.assertEquals(2, counterA.invocationCount);
		Assert.assertEquals(1, counterB.invocationCount);
		Assert.assertEquals(1, counterC.invocationCount);

		manager.calcDistance(rA2);
		Assert.assertEquals(3, counterA.invocationCount);
		Assert.assertEquals(1, counterB.invocationCount);
		Assert.assertEquals(1, counterC.invocationCount);
		Assert.assertEquals(rA2, counterA.lastRoute);
		Assert.assertEquals(rB, counterB.lastRoute);
		Assert.assertEquals(rC2, counterC.lastRoute);
	}

	@Test
	public void testCalcDistance_caching() {
		RouteDistanceCalculatorManager manager = new RouteDistanceCalculatorManager();
		CountingRouteDistanceCalculator counterA = new CountingRouteDistanceCalculator();
		DummyRouteA rA1 = new DummyRouteA();
		DummyRouteA rA2 = new DummyRouteA();

		manager.setCalculator(DummyRouteA.class, counterA);
		Assert.assertEquals(0, counterA.invocationCount);

		manager.calcDistance(rA1);
		Assert.assertEquals(1, counterA.invocationCount);

		manager.calcDistance(rA1); // repeat
		Assert.assertEquals(1, counterA.invocationCount);

		manager.calcDistance(rA2);
		Assert.assertEquals(2, counterA.invocationCount);

		manager.calcDistance(rA1); // again
		Assert.assertEquals(2, counterA.invocationCount);

		rA1.resetDistance();
		manager.calcDistance(rA1); // this time resetted
		Assert.assertEquals(3, counterA.invocationCount);
	}

	@Test
	public void testCalcDistance_missingCalculator() {
		RouteDistanceCalculatorManager manager = new RouteDistanceCalculatorManager();
		manager.setCalculator(DummyRouteC.class, new CountingRouteDistanceCalculator());

		Route r1 = new DummyRouteA();
		try {
			double d = manager.calcDistance(r1);
			Assert.fail("Expected exception. Got distance " + d);
		} catch (IllegalArgumentException e) {
			log.info("Catched expected exception.", e);
		}

	}

	private static abstract class DummyRoute implements Route {

		double dist = Double.NaN;

		@Override
		public double getDistance() {
			return dist;
		}

		@Override
		public void setDistance(double distance) {
			this.dist = distance;
		}

		public void resetDistance() {
			this.dist = Double.NaN;
		}

		@Override
		public double getTravelTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTravelTime(double travelTime) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getStartLinkId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getEndLinkId() {
			throw new UnsupportedOperationException();
		}

	}

	/*package*/ static class DummyRouteA extends DummyRoute { } ;
	/*package*/ static class DummyRouteB extends DummyRoute { } ;
	/*package*/ static class DummyRouteC extends DummyRouteA { } ;

	/*package*/ static class CountingRouteDistanceCalculator implements RouteDistanceCalculator {

		/*package*/ int invocationCount = 0;
		/*package*/ Route lastRoute = null;

		@Override
		public double calcDistance(final Route route) {
			this.invocationCount++;
			this.lastRoute = route;
			return 5.0;
		}

	}
}
