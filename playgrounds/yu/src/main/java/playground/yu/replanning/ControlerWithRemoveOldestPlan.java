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
import org.matsim.core.replanning.StrategyManagerImpl;
import org.matsim.core.replanning.StrategyManagerConfigLoader;

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
	}

	@Override
	protected StrategyManagerImpl loadStrategyManager() {
		StrategyManagerImpl manager = new StrategyManagerWithRemoveOldestPlan();
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new ControlerWithRemoveOldestPlan(args);
		// controler.addControlerListener(new OnePersonPlanScoreMonitor());
		controler.setWriteEventsInterval(0);
		controler.setCreateGraphs(false);
		controler.run();
	}
}
