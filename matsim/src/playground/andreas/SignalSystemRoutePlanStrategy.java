/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.andreas;

import org.matsim.controler.Controler;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.selectors.RandomPlanSelector;


/**
 * @author dgrether
 *
 */
public class SignalSystemRoutePlanStrategy extends PlanStrategy {

	public SignalSystemRoutePlanStrategy(Controler controler) {
		super(new RandomPlanSelector());
		this.addStrategyModule(new ReRouteDijkstraTurningMoves(controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator()));
		
	}

	
	
	
	
}
