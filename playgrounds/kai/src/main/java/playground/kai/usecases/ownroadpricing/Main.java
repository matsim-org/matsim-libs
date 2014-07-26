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
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.*;

/**
 * @author nagel
 *
 */
public class Main {
	
	public static enum Constants { marginalUtilityOfMoney } ; // would need to go to a global place

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

		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			double mum = 1. ; // (somehow calculate marginal utility of money)
			person.getCustomAttributes().put(Constants.marginalUtilityOfMoney.toString(),  mum ) ;
		}

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

		// the scoring function pulls searches by itself if the custom attribute is there, and uses it if yes
		
		// set the travel disutility calculation
		if (!RoadPricingScheme.TOLL_TYPE_AREA.equals(scheme.getType())) { 
			// (area-toll requires a regular TravelCost, no toll-specific one.)
			controler.setTravelDisutilityFactory(new TravelDisutilityFactory() {
				@Override
				public TravelDisutility createTravelDisutility(final TravelTime timeCalculator, final PlanCalcScoreConfigGroup cnScoringGroup) {
					final TravelDisutilityFactory factory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
					final TravelDisutility previousTravelDisutility = factory.createTravelDisutility(timeCalculator, cnScoringGroup);
					// TravelDisutilityIncludingToll searches by itself if the custom attribute is there, and uses it if yes 
					return new TravelDisutilityIncludingToll( previousTravelDisutility, scheme, config );
				}
			}) ;
		}



	}

}
