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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nagel
 *
 */
public class Main2 {

	public static void main(String[] args) {

		final Config config = ConfigUtils.loadConfig(args[0]) ;

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// add road pricing scheme to scenario:
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl() ;
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
			rpReader.parse(rpConfig.getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		scenario.addScenarioElement( RoadPricingScheme.ELEMENT_NAME, scheme);

		final Map<Person,Double> map = new HashMap<Person,Double>() ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			double mum = 1. ;
			map.put(person, mum) ;
		}
		MarginalUtilityOfMoneyLookup muml = new MarginalUtilityOfMoneyLookup() {
			@Override
			public double getMarginalUtilityOfMoney(Person person) {
				return map.get(person) ;
			}
		} ;

		// ---

		final Controler controler = new Controler(scenario) ;

		// add the events handler to calculate the tolls paid by agents
		final CalcPaidToll tollCalc = new CalcPaidToll(scenario.getNetwork(), scheme);
		controler.getEvents().addHandler(tollCalc);
		controler.addControlerListener( new AfterMobsimListener(){
			@Override
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				// evaluate the final tolls paid by the agents and add them to their scores
				tollCalc.sendMoneyEvents(Time.MIDNIGHT, event.getControler().getEvents());
			}
		});

		// set the scoring function.  this is one scoring function per agent
		controler.setScoringFunctionFactory(new ScoringFunctionFactory(){
			@Override
			public ScoringFunction createNewScoringFunction(final Person person) {
				final ScoringFunctionFactory factory = ControlerDefaults.createDefaultScoringFunctionFactory(scenario);
				//					factory.setMarginalUtilityOfMoney( muml ) ;
				ScoringFunction sf = factory.createNewScoringFunction(person) ;
				return sf ;
			}
		});

		// set the travel disutility calculation.  let us pretend that this is one "general" function since this is considered as a service
		if (!RoadPricingScheme.TOLL_TYPE_AREA.equals(scheme.getType())) { 
			// (area-toll requires a regular TravelCost, no toll-specific one.)
			controler.setTravelDisutilityFactory(new TravelDisutilityFactory() {
				@Override
				public TravelDisutility createTravelDisutility(final TravelTime timeCalculator, final PlanCalcScoreConfigGroup cnScoringGroup) {
					final TravelDisutilityFactory factory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
//					factory.setMarginalUtilityOfMoneyLookup(... ) ;
					final TravelDisutility previousTravelDisutility = factory.createTravelDisutility(timeCalculator, cnScoringGroup);
//					return new TravelDisutilityIncludingToll( previousTravelDisutility, scheme, 1. );
//					return new TravelDisutilityIncludingToll( previousTravelDisutility, scheme, muml );
					throw new RuntimeException("deprecated") ;
					
				}
			}) ;
		}



	}

}
