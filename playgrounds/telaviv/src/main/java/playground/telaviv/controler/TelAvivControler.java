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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricing;

import playground.telaviv.core.mobsim.qsim.TTAQSimFactory;
import playground.telaviv.locationchoice.matsimdc.DCScoringFunctionFactory;

public final class TelAvivControler {
		
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: TelAvivControler config-file [dtd-file]");
			System.out.println();
		} else {
			
			Config config = ConfigUtils.loadConfig(args[0]);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
			final Controler controler = new Controler(scenario);
			
			/*
			 * Add road pricing contrib.
			 * It registers a adapted TravelDisutilityFactory which replaces the one used by default.
			 */
			 controler.addControlerListener(new RoadPricing());
			
			// use an adapted MobsimFactory
			controler.setMobsimFactory(new TTAQSimFactory());
			
			controler.addControlerListener(new TelAvivControlerListener());
			
			/*
			 * We use a Scoring Function that get the Facility Opening Times from
			 * the Facilities instead of the Config File.
			 * This is now included in the location choice scoring function.
			 */
//			PlanCalcScoreConfigGroup pcsConfigGroup = config.planCalcScore();
//			controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(pcsConfigGroup, scenario));
//			controler.setScoringFunctionFactory(new CharyparNagelOpenTimesAndDesiresScoringFunctionFactory(pcsConfigGroup, scenario));

			/*
			 * Add location choice contrib 
			 */
			DestinationChoiceBestResponseContext dcContext = new DestinationChoiceBestResponseContext(scenario);
			dcContext.init();
			
			/* 
			 * Add location choice ScoringFunctionFactory to controler.
			 * 
			 * It uses an activity scoring function that get opening times from facilities.
			 * Typical activity duration are read from agents' desires and stored in the
			 * DCPrefs, which are used by the scoring function. Those two features are
			 * what is the difference between MATSims default scoring and the
			 * CharyparNagelOpenTimesAndDesiresScoringFunctionFactory.
			 * 
			 * In this way scoringFunction does not need to create new, identical k-vals by itself.    
			 */
			// does not use desires and also does not respect money events!
	  		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(config, controler, dcContext); 	
			controler.setScoringFunctionFactory(dcScoringFunctionFactory);	

			controler.addControlerListener(new DestinationChoiceInitializer(dcContext));
			
			LocationChoiceConfigGroup lcConfigGroup = config.locationchoice();
			double restraintFcnExp = Double.parseDouble(lcConfigGroup.getRestraintFcnExp());
			double restraintFcnFactor = Double.parseDouble(lcConfigGroup.getRestraintFcnFactor());
			if (restraintFcnExp > 0.0 && restraintFcnFactor > 0.0) {		
				controler.addControlerListener(new FacilitiesLoadCalculator(dcContext.getFacilityPenalties()));
			}
			
			controler.run();
		}
		System.exit(0);
	}

}