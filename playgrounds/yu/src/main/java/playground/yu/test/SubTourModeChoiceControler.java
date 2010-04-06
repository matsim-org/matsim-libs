/* *********************************************************************** *
 * project: org.matsim.*
 * SubTourModeChoiceControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.test;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.yu.scoring.CharyparNagelScoringFunctionFactoryWithWalk;

/**
 * @author yu
 * 
 */
public class SubTourModeChoiceControler extends Controler {

	public SubTourModeChoiceControler(String args) {
		super(args);
	}

	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		MyStrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	public static void main(String[] args) {
		Controler controler = new SubTourModeChoiceControler(args[0]);
		controler
				.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(
						new ScenarioLoaderImpl(args[0]).loadScenario()
								.getConfig().charyparNagelScoring()));
		controler.setWriteEventsInterval(Integer.parseInt(args[1]));
		controler.setCreateGraphs(Boolean.parseBoolean(args[2]));
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
