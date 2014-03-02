/* *********************************************************************** *
 * project: org.matsim.*
 * InitialRoutesController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.dissertation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.analysis.christoph.ActivitiesAnalyzer;
import org.matsim.contrib.analysis.christoph.TripsAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.christoph.controler.WithinDayInitialRoutesControlerListener;
import playground.christoph.scoring.DesiresAndOpenTimesScoringFunctionFactory;

public class InitialRoutesController {

	private static final Logger log = Logger.getLogger(InitialRoutesController.class);
		
	private static boolean useWithinDayReplanning = true;
	private static double duringLegReroutingShare = 0.10;
	private static boolean duringLegRerouting = true;
	private static boolean initialLegRerouting = true;
	private static boolean useFacilityOpenTimes = true;
	
	public static void main (String[] args) {
		
		if (args.length == 0) return;
		
		// parse parameter from command line
		for (int i = 1; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-duringLegReroutingShare")) {
				i++;
				duringLegReroutingShare = Double.parseDouble(args[i]);
				if (duringLegReroutingShare > 1.0) duringLegReroutingShare = 1.0;
				else if (duringLegReroutingShare < 0.0) duringLegReroutingShare = 0.0;
				log.info("during leg re-routing share: " + duringLegReroutingShare);
			} else if (args[i].equalsIgnoreCase("-duringLegRerouting")) {
				i++;
				duringLegRerouting = Boolean.parseBoolean(args[i]);
				log.info("during leg rerouting enabled: " + duringLegRerouting);
			} else if (args[i].equalsIgnoreCase("-initialLegRerouting")) {
				i++;
				initialLegRerouting = Boolean.parseBoolean(args[i]);
				log.info("initial leg re-routing enabled: " + initialLegRerouting);
			} else if (args[i].equalsIgnoreCase("-useWithinDayReplanning")) {
				i++;
				useWithinDayReplanning = Boolean.parseBoolean(args[i]);
				log.info("use within-day replanning: " + useWithinDayReplanning);
			} else if (args[i].equalsIgnoreCase("-useOpenTimesFromFacilities")) {
				i++;
				useFacilityOpenTimes = Boolean.parseBoolean(args[i]);
				log.info("use open times from facilities: " + useFacilityOpenTimes);
			} else log.warn("Unknown Parameter: " + args[i]);
		}
		
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = null;
		
		/*
		 * Ensure that initial Routes are empty.
		 */
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						leg.setRoute(null);
					}
				}
			}
		}
		
		controler = new Controler(scenario);			
		/*
		 * Add some analysis modules to the controler.
		 */
		controler.addControlerListener(new TripsAnalyzer());
		controler.addControlerListener(new ActivitiesAnalyzer());
		
		if (useWithinDayReplanning) {
			WithinDayInitialRoutesControlerListener controlerListener = new WithinDayInitialRoutesControlerListener();
			controlerListener.setDuringLegReroutingEnabled(duringLegRerouting);
			controlerListener.setDuringLegReroutingShare(duringLegReroutingShare);
			controlerListener.setInitialLegReroutingEnabled(initialLegRerouting);
			controler.addControlerListener(controlerListener);
		}
		
		/*
		 * Use a scoring function which uses opening times from the facilities.
		 */
		if (useFacilityOpenTimes) {
//			controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(config.planCalcScore(), scenario));
			controler.setScoringFunctionFactory(new DesiresAndOpenTimesScoringFunctionFactory(config.planCalcScore(), scenario));
		}
		
		controler.run();
	}
}
