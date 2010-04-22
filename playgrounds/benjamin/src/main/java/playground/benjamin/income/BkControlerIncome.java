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
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.benjamin.BkControler;

/**
 * @author bkick
 * @author michaz
 *
 */
public class BkControlerIncome extends BkControler {

	private PersonHouseholdMapping personHouseholdMapping;

	public BkControlerIncome(String arg) {
		super(arg);
	}
	
	public BkControlerIncome(String[] args) {
		super(args);
	}

	public BkControlerIncome(Config config) {
		super(config);
	}

	@Override
	protected void setUp() {
		this.scenarioData.getConfig().global().setNumberOfThreads(1);
		this.personHouseholdMapping = new PersonHouseholdMapping(this.getScenario().getHouseholds());
		
		//setting the needed scoring function
		ScoringFunctionFactory scoringFactory = new IncomeScoringFunctionFactory(this.getScenario().getConfig().charyparNagelScoring(), personHouseholdMapping, this.getNetwork());
		
		
		this.setScoringFunctionFactory(scoringFactory);
		super.setUp();
	}

	private void installTravelCostCalculatorFactory() {
		RoadPricingScheme roadPricingScheme = super.getRoadPricing().getRoadPricingScheme();
		
		//setting the travel cost calculator for the router
		TravelCostCalculatorFactory travelCostCalculatorFactory = new IncomeTravelCostCalculatorFactory(personHouseholdMapping, roadPricingScheme);
		setTravelCostCalculatorFactory(travelCostCalculatorFactory);
	}

	private void addInstallTravelCostCalculatorFactoryControlerListener() {
		addControlerListener(new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				installTravelCostCalculatorFactory();
			}
			
		});
	}
	
	public static void main(String[] args) {
		//	String config = BkPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/config.xml"; //can also be included in runConfigurations/arguments/programArguments
		//	String[] args2 = {config};
		//	args = args2;
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final BkControlerIncome controler = new BkControlerIncome(args);
			controler.addInstallTravelCostCalculatorFactoryControlerListener();
			controler.run();
		}
	}


}
