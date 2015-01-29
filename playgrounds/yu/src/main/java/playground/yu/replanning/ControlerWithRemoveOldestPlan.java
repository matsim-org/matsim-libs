/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerWithRemoveOldestPlan.java
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

/**
 *
 */
package playground.yu.replanning;

import org.matsim.core.controler.Controler;

/**
 * test the effect with "removeOldestPlan"
 *
 * @author yu
 *
 */
public class ControlerWithRemoveOldestPlan extends Controler {

	/**
	 * @param args
	 */
	public ControlerWithRemoveOldestPlan(String[] args) {
		super(args);
		throw new RuntimeException("overriding loadStrategyManager no longer possible; thus this class is no longer working.  kai, oct'14") ;
}

	public ControlerWithRemoveOldestPlan(String configFilename) {
		super(configFilename);
	}

//	@Override
//	protected StrategyManager loadStrategyManager() {
//		StrategyManager manager = new StrategyManagerWithRemoveOldestPlan();
//		StrategyManagerConfigLoader.load(this, manager);
//		return manager;
//	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new ControlerWithRemoveOldestPlan(args);
		// controler.addControlerListener(new OnePersonPlanScoreMonitor());
		controler.getConfig().controler().setWriteEventsInterval(0);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.run();
	}
}
