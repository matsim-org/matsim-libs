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
package playground.kai.usecases.ownroadpricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.RoadPricingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
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
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.TravelDisutilityIncludingToll;

/**
 * @author nagel
 *
 */
public class Main {

	public static void main(String[] args) {
		
		final Config config = ConfigUtils.loadConfig(args[0]) ;
		
		final Scenario sc = ScenarioUtils.loadScenario(config) ;
		
		// add road pricing scheme to agents:
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl() ;
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
			rpReader.parse(rpConfig.getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		sc.addScenarioElement( RoadPricingScheme.ELEMENT_NAME, scheme);

		final Controler controler = new Controler(sc) ;
		
		// add the events handler to calculate the tolls paid by agents
		final CalcPaidToll tollCalc = new CalcPaidToll(sc.getNetwork(), scheme);
		controler.getEvents().addHandler(tollCalc);

		controler.setScoringFunctionFactory(new ScoringFunctionFactory(){
			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				ScoringFunction sf = ControlerUtils.createDefaultScoringFunctionFactory(sc)
						.createNewScoringFunction(plan) ;

				// person-specific utl of money:
				Person person = plan.getPerson() ;
				double marginalUtilityOfMoney = 1. ;
				
				return sf ;
			}
		});
		
		// replace the travelCostCalculator with a toll-dependent one if required
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType()) 
				|| RoadPricingScheme.TOLL_TYPE_CORDON.equals(scheme.getType())) {
			// area-toll requires a regular TravelCost, no toll-specific one.
			controler.setTravelDisutilityFactory(new TravelDisutilityFactory() {
				@Override
				public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
					final TravelDisutility previousTravelDisutility = ControlerUtils.createDefaultTravelDisutilityFactory()
							.createTravelDisutility(timeCalculator, cnScoringGroup);
					
					// at this point, the previous travel disutility calculator factory may or may not know something about person-specific
					// marginal utl of money.  --??
					
					return new TravelDisutilityIncludingToll( previousTravelDisutility, scheme, controler.getConfig() );
				}
			});
		}
		
		controler.addControlerListener( new AfterMobsimListener(){
			@Override
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				// evaluate the final tolls paid by the agents and add them to their scores
				tollCalc.sendMoneyEvents(Time.MIDNIGHT, event.getControler().getEvents());
			}
		});


	}

}
