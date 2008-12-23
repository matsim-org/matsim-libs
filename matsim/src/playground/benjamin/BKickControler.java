/* *********************************************************************** *
 * project: org.matsim.*																															*
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
package playground.benjamin;

import org.matsim.controler.Controler;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PlanCalcType;
import org.matsim.run.OTFVis;
import org.matsim.scoring.ScoringFunctionFactory;


/**
 * @author dgrether
 *
 */
public class BKickControler extends Controler {

	public BKickControler(String configFileName) {
		super(configFileName);
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new BKickScoringFunctionFactory(this.config.charyparNagelScoring());
	}

	@Override
	protected Population loadPopulation() {
		Population pop = super.loadPopulation();
		pop.addAlgorithm(new PlanCalcType());
		pop.runAlgorithms();
		return pop;
	}
	
	
	
	 public static void main(String[] args){
			String equilExampleConfig = "examples/equil/configOTF.xml";
			String oneRouteNoModeTest = "../studies/bkickhoefer/oneRouteTwoModeTest/config.xml";
//			String config = equilExampleConfig;
			String config = oneRouteNoModeTest;
			
			BKickControler c = new BKickControler(config);
			c.setOverwriteFiles(true);
			c.run();
			
			int lastIteration = c.getConfig().controler().getLastIteration();
			
//			String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/Snapshot";
			String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/"+lastIteration+".otfvis.mvi";
			
			String[] visargs = {out};
			
			OTFVis.main(new String[] {out});	
	 }

	
	
}
