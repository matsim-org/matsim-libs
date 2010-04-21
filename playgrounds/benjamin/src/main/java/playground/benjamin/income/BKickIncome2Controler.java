/* *********************************************************************** *
 * project: org.matsim.*
 * BKickIncomeControler2
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
package playground.benjamin.income;

import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.households.PersonHouseholdMapping;

import playground.benjamin.BkControler;

/**
 * @author dgrether
 *
 */
public class BKickIncome2Controler extends BkControler {

	private PersonHouseholdMapping personHouseholdMapping;

	public BKickIncome2Controler(String arg) {
		super(arg);
	}


	public BKickIncome2Controler(String[] args) {
		super(args);
	}


	public BKickIncome2Controler(Config config) {
		super(config);
	}

	@Override
	protected void setUp() {
		this.scenarioData.getConfig().global().setNumberOfThreads(1);
		this.personHouseholdMapping = new PersonHouseholdMapping(this.getScenario().getHouseholds());
		
		ScoringFunctionFactory scoringFactory = new BKickIncome2ScoringFunctionFactory(this.getScenario().getConfig().charyparNagelScoring(), personHouseholdMapping, this.getNetwork());
		TravelCostCalculatorFactory travelCostCalculatorFactory = new Income2TravelCostCalculatorFactory(personHouseholdMapping);
		
		setTravelCostCalculatorFactory(travelCostCalculatorFactory);
		this.setScoringFunctionFactory(scoringFactory);
		super.setUp();
	}

	public static void main(String[] args) {
		//	String config = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/config.xml"; //can also be included in runConfigurations/arguments/programArguments
		//	String[] args2 = {config};
		//	args = args2;
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final BKickIncome2Controler controler = new BKickIncome2Controler(args);
			controler.run();
		}
	}


}
