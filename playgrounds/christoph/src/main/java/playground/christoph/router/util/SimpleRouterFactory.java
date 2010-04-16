/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleRouterFactory.java
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
package playground.christoph.router.util;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

/*
 * A simple LeastCostPathCalculatorFactory for Routing Modules that
 * ignore TravelCosts and TravelTimes as for example a Random Router
 * does.
 * 
 * Each SimpleRouter implements PersonalizableTravelCost and Cloneable.
 * This Factory should be use in combination with CloneablePlansCalcRoute.
 * There the SimpleRouter instance is cloned therefore we don't clone it
 * here again! Otherwise the CloneablePlansCalcRoute object can't hand 
 * a handled person to the SimpleRouter.
 * 
 */
public class SimpleRouterFactory implements LeastCostPathCalculatorFactory{

	private static final Logger log = Logger.getLogger(SimpleRouterFactory.class);
	
//	private LeastCostPathCalculator calculator;
	
	public SimpleRouterFactory()
	{
	}
	
//	public SimpleRouterFactory(LeastCostPathCalculator calculator)
//	{
//		this.calculator = calculator;
//	}
	
	public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes)
	{
		if (travelCosts instanceof SimpleRouter)
		{
			return ((LeastCostPathCalculator)travelCosts);
		}
		else return null;
		/*
		 * We don't clone travelCosts and travelTimes Objects - they should already
		 * be clones created by ClonablePlansCalcRoute! 
		 */
//		LeastCostPathCalculator calculatorClone = null;
//		if (calculator instanceof Cloneable)
//		{
//			try
//			{
//				Method method;
//				method = calculator.getClass().getMethod("clone", new Class[]{});
//				calculatorClone = calculator.getClass().cast(method.invoke(calculator, new Object[]{}));
//				return calculatorClone;
//			}
//			catch (Exception e)
//			{
//				Gbl.errorMsg(e);
//			} 
//		}
//		/*
//		 *  Not cloneable or an Exception occured when trying to Clone
//		 *  Now try to create a new Calculator Object with an empty Constructor
//		 */
//		if (calculatorClone == null)
//		{
//			try
//			{
//				calculatorClone = this.calculator.getClass().newInstance();
//				return calculatorClone;
//			} 
//			catch (Exception e) 
//			{
//				Gbl.errorMsg(e);
//			}
//		}
//		/*
//		 * We tried everything but we can't get a new Calculator Object
//		 * so we finally use the existing one.
//		 */
//		if (calculatorClone == null)
//		{
//			calculatorClone = calculator;
//			log.warn("Could not clone the Least Cost Path Calculator - use reference to the existing Calculator and hope the best...");		
//		}
//		return calculator;
	}

}
