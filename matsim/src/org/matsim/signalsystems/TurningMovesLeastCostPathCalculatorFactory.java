/* *********************************************************************** *
 * project: org.matsim.*
 * TurningMovesLeastCostPathCalculatorFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems;

import org.matsim.interfaces.core.v01.Network;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.LeastCostPathCalculatorFactory;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;


/**
 * @author dgrether
 *
 */
public class TurningMovesLeastCostPathCalculatorFactory implements
		LeastCostPathCalculatorFactory {

	public TurningMovesLeastCostPathCalculatorFactory(){
		
	}
	
	/**
	 * @see org.matsim.router.util.LeastCostPathCalculatorFactory#createPathCalculator(org.matsim.interfaces.core.v01.Network, org.matsim.router.util.TravelCost, org.matsim.router.util.TravelTime)
	 */
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelCost travelCosts, TravelTime travelTimes) {

		
		return null;
	}

}
