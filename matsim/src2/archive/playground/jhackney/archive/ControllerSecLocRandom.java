/* *********************************************************************** *
 * project: org.matsim.*
 * ControllerSecLocRandom.java
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

package playground.jhackney.controler;

import org.matsim.controler.Controler;


public class ControllerSecLocRandom extends Controler {

	public ControllerSecLocRandom(final String[] args) {
		super(args);
	}

//	@Override
//	/**
//	 * This is a test StrategyManager to see if the replanning works within the social network iterations.
//	 * @author jhackney
//	 * @return
//	 */
//	protected StrategyManager loadStrategyManager() {
//		StrategyManager manager = new StrategyManager();
//
//		String maxvalue = this.config.findParam("strategy", "maxAgentPlanMemorySize");
//		manager.setMaxPlansPerAgent(Integer.parseInt(maxvalue));
//
//		// Best-scoring plan chosen each iteration
//		PlanStrategy strategy1 = new PlanStrategy(new BestPlanSelector());
//
//		// Social Network Facility Exchange test
//		System.out.println(this.getClass()+": adding StrategyModule. NOTE THAT YOU SHOULD EXCHANGE KNOWLEDGE BASED ON ITS VALUE");
//		strategy1.addStrategyModule(new SNRandomFacilitySwitcher());
//		//strategy1.addStrategyModule(new TimeAllocationMutator());
//
//
//		// Social Network Facility Exchange for all agents
//		manager.addStrategy(strategy1, 1.0);
//		return manager;
//	}

	public static void main(final String[] args) {
		final Controler controler = new ControllerSecLocRandom(args);
		controler.addControlerListener(new ControllerListenerSecLocRandom());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
