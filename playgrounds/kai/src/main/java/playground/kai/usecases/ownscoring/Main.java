/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.usecases.ownscoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public class Main {

	public static void main(String[] args) {

			final Config config = ConfigUtils.loadConfig(args[0]) ;
			
			final Scenario sc = ScenarioUtils.loadScenario(config) ;
			
			Controler ctrl = new Controler(sc) ;
			
			ctrl.setScoringFunctionFactory(new ScoringFunctionFactory(){
				@Override
				public ScoringFunction createNewScoringFunction(Plan plan) {
					CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
					SumScoringFunction sum = new SumScoringFunction() ;
					sum.addScoringFunction(new CharyparNagelLegScoring(params, sc.getNetwork()));
					sum.addScoringFunction(new CharyparNagelActivityScoring(params));
					sum.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
					sum.addScoringFunction(new CharyparNagelMoneyScoring(params)) ;
					return sum ;
				}
			});
			
			ctrl.setTravelDisutilityFactory(new TravelDisutilityFactory(){
				@Override
				public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
					final TravelTime ttimeLookup = timeCalculator ;
					final PlanCalcScoreConfigGroup scConfig = cnScoringGroup ;
					TravelDisutility td = new TravelDisutility(){
						@Override
						public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
							// time:
							double ttime1 =  ttimeLookup.getLinkTravelTime(link, time, person, vehicle) ;
							double utl = ttime1 * (scConfig.getTraveling_utils_hr() + scConfig.getPerforming_utils_hr())/3600. ;
							// toll:
							return utl ;
						}
						@Override
						public double getLinkMinimumTravelDisutility(Link link) {
							return link.getLength() / link.getFreespeed() ;
						}
					} ;
					return td ;
				}
			});
		
	}

}
