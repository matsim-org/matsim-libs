/* *********************************************************************** *
 * project: org.matsim.*
 * CloneablePlansCalcRoute.java
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

package playground.christoph.router;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;

/*
 * Extended version of PlansCalcRoute that tries to create 
 * deep clones including clones of the time- and cost calculators).
 */
public class CloneablePlansCalcRoute extends PlansCalcRoute implements Cloneable{
	
	protected PersonalizableTravelCost costCalculator;
	protected TravelTime timeCalculator;
	protected LeastCostPathCalculatorFactory factory;
		
	private final static Logger log = Logger.getLogger(CloneablePlansCalcRoute.class);
				
	public CloneablePlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final PersonalizableTravelCost costCalculator,
			final TravelTime timeCalculator, LeastCostPathCalculatorFactory factory){
		super(group, network, costCalculator, timeCalculator, factory);

		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.factory = factory;
	}
	
	/*
	 * If no LeastCostPathCalculatorFactory is given use by Default
	 * a DijkstraFactory.
	 */
	public CloneablePlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final PersonalizableTravelCost costCalculator, final TravelTime timeCalculator) {
		this(group, network, costCalculator, timeCalculator, new DijkstraFactory());
	}
		
	@Override
	public CloneablePlansCalcRoute clone() {
		
		PersonalizableTravelCost travelCostClone = null;
		if (costCalculator instanceof Cloneable) {
			try {
				Method method;
				method = costCalculator.getClass().getMethod("clone", new Class[]{});
				travelCostClone = costCalculator.getClass().cast(method.invoke(costCalculator, new Object[]{}));
			} catch (Exception e) {
				Gbl.errorMsg(e);
			} 
		}
		
		// not cloneable or an Exception occurred
		if (travelCostClone == null) {
			travelCostClone = costCalculator;
			log.warn("Could not clone the Travel Cost Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		TravelTime travelTimeClone = null;
		if (timeCalculator instanceof Cloneable) {
			try {
				Method method;
				method = timeCalculator.getClass().getMethod("clone", new Class[]{});
				travelTimeClone = timeCalculator.getClass().cast(method.invoke(timeCalculator, new Object[]{}));
			} catch (Exception e) {
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occurred
		if (travelTimeClone == null) {
			travelTimeClone = timeCalculator;
			log.warn("Could not clone the Travel Time Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		CloneablePlansCalcRoute clone;
		clone = new CloneablePlansCalcRoute(configGroup, network, travelCostClone, travelTimeClone, factory);
	
		return clone;
	}
}
