/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler2
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

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.algorithms.PlanCalcType;
import org.matsim.scoring.ScoringFunctionFactory;


/**
 * Controler for first zurich scenario test run of estimated scoring function.
 * @author dgrether
 *
 */
public class BKickControler3 extends Controler {

	public BKickControler3(String configFileName) {
		super(configFileName);
		
	}
	
	public BKickControler3(Config conf){
		super(conf);
	}

	public BKickControler3(String[] args) {
		super(args);
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new BKickScoringFunctionFactory(this.config.charyparNagelScoring());
	}

	@Override
	protected void setup(){
		BKickTravelCostCalculator tcc = new BKickTravelCostCalculator(this.config.charyparNagelScoring());
		super.setTravelCostCalculator(tcc);
		BKickFreespeedTravelTimeCost fttc = new BKickFreespeedTravelTimeCost(this.config.charyparNagelScoring());
//		super.setFreespeedTravelTimeCost(fttc);
		super.setup();
		tcc.setTravelTimeCalculator(super.getTravelTimeCalculator());
	}
	
	@Override
	protected Population loadPopulation() {
		Population pop = super.loadPopulation();
		pop.addAlgorithm(new PlanCalcType());
		pop.runAlgorithms();
		return pop;
	}
	
	public static void main(String[] args) {
		String []args2 = {"../bkick/routerTest/configRouterTest.xml"};
		args = args2;
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new BKickControler3(args);
			controler.run();
		}
		System.exit(0);
	}

}
