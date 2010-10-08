/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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
package playground.benjamin.income2;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.households.Households;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.population.algorithms.PlanCalcType;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author nagel
 *
 */
public class BkIncomeControlerListener implements StartupListener {
	
	private PersonHouseholdMapping personHouseholdMapping;
	
	BkIncomeControlerListener() {}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler() ;
		Scenario scenario = controler.getScenario() ;
		
		scenario.getConfig().global().setNumberOfThreads(1);
		
		new PlanCalcType().run(scenario.getPopulation());
		
		// this does not (yet?) work:
		Households hh = scenario.getScenarioElement( Households.class ) ;
		System.err.println( "hh: " + hh ) ;
		
		this.personHouseholdMapping = new PersonHouseholdMapping( ((ScenarioImpl) scenario).getHouseholds() );
		
		/*		Setting the needed scoring function.
		Remark: parameters must be set in several classes and independently for scoring and router!*/
		ScoringFunctionFactory scoringFactory = new IncomeScoringFunctionFactory(scenario.getConfig(), personHouseholdMapping, scenario.getNetwork());
		
		controler.setScoringFunctionFactory(scoringFactory);

		installTravelCostCalculatorFactory(controler);
	}
	
	private void installTravelCostCalculatorFactory(Controler controler) {
		//returns null, if there is no road pricing
		if (controler.getConfig().scenario().isUseRoadpricing()){
			RoadPricingScheme roadPricingScheme = controler.getRoadPricing().getRoadPricingScheme();
			
			/*		Setting travel cost calculator for the router.
			Remark: parameters must be set in several classes and independently for scoring and router!*/
			TravelCostCalculatorFactory travelCostCalculatorFactory = new IncomeTollTravelCostCalculatorFactory(personHouseholdMapping, roadPricingScheme);
			controler.setTravelCostCalculatorFactory(travelCostCalculatorFactory);
		}
		else{
			/*		Setting travel cost calculator for the router.
			Remark: parameters must be set in several classes and independently for scoring and router!*/
			TravelCostCalculatorFactory travelCostCalculatorFactory = new IncomeTravelCostCalculatorFactory(personHouseholdMapping);
			controler.setTravelCostCalculatorFactory(travelCostCalculatorFactory);
		}
	}
	


}
