/* *********************************************************************** *
 * project: org.matsim.*
 * TelAvivControler.java
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

package playground.telaviv.controler;

import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.telaviv.core.mobsim.qsim.TTAQSimFactory;
import playground.telaviv.replanning.TTAStrategyManager;

public class TelAvivControler extends Controler {

	public TelAvivControler(final String[] args) {
		super(args);
	}
	
//	/*
//	 * We use a Scoring Function that get the Facility Opening Times from
//	 * the Facilities instead of the Config File.
//	 */
//	@Override
//	protected ScoringFunctionFactory loadScoringFunctionFactory() {
//		return new CharyparNagelOpenTimesScoringFunctionFactory(this.config.planCalcScore(), this.getScenario());
//	}
	
	@Override
	protected void loadData() {
		super.loadData();
		
		// connect facilities to links
		new WorldConnectLocations(config).connectFacilitiesWithLinks(this.scenarioData.getActivityFacilities(), (NetworkImpl) network);
	}
	
	/*
	 * Use a TTAStrategyManager that ignores TTA Agents when doing
	 * the replanning.
	 * 
	 * Use LocationChoiceStrategyManagerConfigLoader which performs location choice for
	 * main shopping activities.
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new TTAStrategyManager(this.scenarioData);
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}	

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: TelAvivControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new TelAvivControler(args);
			
			// use an adapted MobsimFactory
			controler.setMobsimFactory(new TTAQSimFactory());
			
			/*
			 * We use a Scoring Function that get the Facility Opening Times from
			 * the Facilities instead of the Config File.
			 */
			controler.setScoringFunctionFactory(
					new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario())
					) ;

			controler.run();
		}
		System.exit(0);
	}
}