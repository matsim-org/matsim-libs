/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.andreas.intersection.dijkstra;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * @author dgrether
 * @deprecated this class is no longer needed due to new routing infrastructure
 * where a common LeastCostPathCalculator is used that is to be set in the 
 * Controler
 */
@Deprecated
public class SignalSystemRoutePlanStrategy extends PlanStrategy {

	public SignalSystemRoutePlanStrategy(Controler controler) {
		super(new RandomPlanSelector());
		this.addStrategyModule(new ReRouteDijkstraTurningMoves(controler.getConfig(), controler.getNetwork(),
				controler.createTravelCostCalculator(), controler.getTravelTimeCalculator()));
	}

}
