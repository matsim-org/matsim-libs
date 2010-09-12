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

import org.matsim.api.core.v01.population.Route;

import playground.mrieser.core.mobsim.utils.ClassBasedMap;

/**
 * Manages a set of {@link RouteDistanceCalculator}s to calculate / retrieve the
 * distance of different types of {@link Route}s.
 *
 * @author mrieser
 */
public class RouteDistanceCalculatorManager {

	private final ClassBasedMap<Route, RouteDistanceCalculator> calculators = new ClassBasedMap<Route, RouteDistanceCalculator>();

	/**
	 * If necessary, calculates the distance of a route, before returning it.
	 *
	 * @param route
	 * @return the distance of the route
	 * @throw IllegalArgumentException if no route calculator is set to handle the given type of route.
	 */
	public double calcDistance(Route route) {
		Double dist = route.getDistance();
		if (dist != null && !Double.isNaN(dist.doubleValue())) {
			return dist.doubleValue();
		}
		RouteDistanceCalculator calc = getCalculator(route.getClass());
		if (calc == null) {
			throw new IllegalArgumentException("No distance calculator known for route of type " + route.getClass().getCanonicalName());
		}
		double d = calc.calcDistance(route);
		route.setDistance(d);
		return d;
	}

	/**
	 * Sets a specific {@link RouteDistanceCalculator} to be used for a specific type of {@link Route}s.
	 * The given calculator will be used for all route instances that are of type <code>routeClass</code>
	 * or of any sub-type of it (except there is another calculator set for the specific sub-type).
	 *
	 * @param routeClass
	 * @param calculator
	 * @return the previously assigned calculator for the given route class, or <code>null</code> if none was set before.
	 */
	public RouteDistanceCalculator setCalculator(Class<? extends Route> routeClass, RouteDistanceCalculator calculator) {
		return this.calculators.put(routeClass, calculator);
	}

	/*package*/ RouteDistanceCalculator getCalculator(Class<? extends Route> routeClass) {
		return this.calculators.get(routeClass);
	}
}
