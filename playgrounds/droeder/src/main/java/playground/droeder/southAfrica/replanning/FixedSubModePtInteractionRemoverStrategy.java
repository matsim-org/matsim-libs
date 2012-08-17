/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.southAfrica.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;

/**
 * @author droeder
 *
 */
public class FixedSubModePtInteractionRemoverStrategy implements PlanStrategyModule {
	
	//TODO make it multithreadded again!
	/**
	 * This class provides a strategy to remove pt-interactions from a plan, but changes the 
	 * legmode of the "real" pt-leg not to <code>TransportMode.pt</code>. Instead it keeps the 
	 * original mode
	 * 
	 * @param c
	 */
	public FixedSubModePtInteractionRemoverStrategy(Controler c){
//		super(c.getConfig().global());
	}
//
//	@Override
//	public PlanAlgorithm getPlanAlgoInstance() {
//		return new FixedPtSubModePtInteractionRemover();
//	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.replanning.PlanStrategyModule#prepareReplanning()
	 */
	@Override
	public void prepareReplanning() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.replanning.PlanStrategyModule#handlePlan(org.matsim.api.core.v01.population.Plan)
	 */
	@Override
	public void handlePlan(Plan plan) {
		// TODO Auto-generated method stub
		new FixedPtSubModePtInteractionRemover().run(plan);
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.replanning.PlanStrategyModule#finishReplanning()
	 */
	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub
		
	}

}
