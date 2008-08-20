/* *********************************************************************** *
 * project: org.matsim.*
 * LimitedKnowledge.java
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

package playground.christoph.knowledge;

import org.matsim.network.NetworkLayer;
import org.matsim.replanning.modules.ReRouteDijkstra;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

public class LimitedKnowledge extends ReRouteDijkstra {		

	// Erstmal alle Daten übernehmen... 
	public LimitedKnowledge(NetworkLayer network, TravelCost travelCostCalc, TravelTime travelTimeCalc)
	{	
		super(network, travelCostCalc, travelTimeCalc);
		System.out.println("----------------------LimitedKnowledge Router runs!----------------------");
		
	}	


/*
public class LimitedKnowledge implements StrategyModuleI {

	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlePlan(Plan plan) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}
*/
}
