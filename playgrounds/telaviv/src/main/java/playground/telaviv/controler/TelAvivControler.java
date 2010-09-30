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
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.telaviv.replanning.LocationChoiceStrategyManagerConfigLoader;
import playground.telaviv.replanning.TTAStrategyManager;

public class TelAvivControler extends Controler {

	public TelAvivControler(final String[] args)
	{
		super(args);
	}
	
	/*
	 * We use a Scoring Function that get the Facility Opening Times from
	 * the Facilities instead of the Config File.
	 */
	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelOpenTimesScoringFunctionFactory(this.config.charyparNagelScoring(), this.getFacilities());
	}
		
	/*
	 * Use a TTAStrategyManager that ignores TTA Agents when doing
	 * the replanning. 
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new TTAStrategyManager(this.scenarioData);
		LocationChoiceStrategyManagerConfigLoader.load(this, manager);
		return manager;
	}	

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: TelAvivControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new TelAvivControler(args);
			controler.run();
		}
		System.exit(0);
	}
}
