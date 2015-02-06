/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.shapes.IKGISAnalyzer;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * Compares a base case with a policy case.
 * Computes the increase/decrease of user benefits and plots these changes. 
 * 
 * @author ikaddoura
 *
 */
public class SpatialWelfareAnalysis {
	private static final Logger log = Logger.getLogger(SpatialWelfareAnalysis.class);
	
	static String runDirectory1; // base case
	static String runDirectory2; // policy case
	
	static String shapeFileZones;
	static String homeActivity;
	static String workActivity;
	
	// the number of persons a single agent represents
	static int scalingFactor;
			
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			runDirectory1 = args[0];		
			log.info("runDirectoryBaseCase: " + runDirectory1);
			
			runDirectory2 = args[1];		
			log.info("runDirectoryPolicyCase: " + runDirectory2);
			
			shapeFileZones = args[2];		
			log.info("shapeFileZones: " + shapeFileZones);
			
			homeActivity = args[3];		
			log.info("homeActivity: " + homeActivity);
			
			workActivity = args[4];		
			log.info("workActivity: " + workActivity);
			
			scalingFactor = Integer.valueOf(args[5]);		
			log.info("scalingFactor: " + scalingFactor);
			
		} else {
			
			runDirectory1 = "../../runs-svn/berlin_internalizationCar/output/baseCase_2/";
			runDirectory2 = "../../runs-svn/berlin_internalizationCar/output/internalization_2/";

			shapeFileZones = "../../shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_1500/berlin_grid_1500.shp";
			homeActivity = "home";
			workActivity = "work";
			scalingFactor = 10;
		}
		
		SpatialWelfareAnalysis congestionEventsWriter = new SpatialWelfareAnalysis();
		congestionEventsWriter.run();
	}

	private void run() {
	
		IKGISAnalyzer gisAnalysis = new IKGISAnalyzer(shapeFileZones, scalingFactor, homeActivity, workActivity);

		// Base Case
		
		Config config1 = ConfigUtils.loadConfig(runDirectory1 + "output_config.xml.gz");
		config1.network().setInputFile(runDirectory1 + "output_network.xml.gz");
		config1.plans().setInputFile(runDirectory1 + "output_plans.xml.gz");
		ScenarioImpl scenario1 = (ScenarioImpl) ScenarioUtils.loadScenario(config1);
		
		Map<Id<Person>, Double> personId2userBenefit_baseCase = getPersonId2UserBenefit(scenario1, runDirectory1);
		Map<Id<Person>, Double> personId2tollPayments_baseCase = getPersonId2TollPayments(scenario1, runDirectory1);
		Map<Id<Person>, Double> personId2welfareContribution_baseCase = calculateSum(personId2userBenefit_baseCase, personId2tollPayments_baseCase);
		
		gisAnalysis.analyzeZones_welfare("baseCase", scenario1, runDirectory1, personId2userBenefit_baseCase, personId2tollPayments_baseCase, personId2welfareContribution_baseCase);
		
		// Policy Case
		
		Config config2 = ConfigUtils.loadConfig(runDirectory2 + "output_config.xml.gz");
		config2.network().setInputFile(runDirectory2 + "output_network.xml.gz");
		config2.plans().setInputFile(runDirectory2 + "output_plans.xml.gz");
		ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.loadScenario(config2);
		
		Map<Id<Person>, Double> personId2userBenefit_policyCase = getPersonId2UserBenefit(scenario2, runDirectory2);
		Map<Id<Person>, Double> personId2tollPayments_policyCase = getPersonId2TollPayments(scenario2, runDirectory2);
		Map<Id<Person>, Double> personId2welfareContribution_policyCase = calculateSum(personId2userBenefit_policyCase, personId2tollPayments_policyCase);

		gisAnalysis.analyzeZones_welfare("policyCase", scenario2, runDirectory2, personId2userBenefit_policyCase, personId2tollPayments_policyCase, personId2welfareContribution_policyCase);

		// Comparison
		
		Map<Id<Person>, Double> personId2userBenefit_difference = calculateDifference(scenario1, personId2userBenefit_policyCase, personId2userBenefit_baseCase);
		Map<Id<Person>, Double> personId2tollPayments_difference = calculateDifference(scenario1, personId2tollPayments_policyCase, personId2tollPayments_baseCase);
		Map<Id<Person>, Double> personId2welfareContribution_difference = calculateDifference(scenario1, personId2welfareContribution_policyCase, personId2welfareContribution_baseCase);
	
		gisAnalysis.analyzeZones_welfare("policyCase_baseCase_comparison", scenario2, runDirectory2, personId2userBenefit_difference, personId2tollPayments_difference, personId2welfareContribution_difference);
		
	}

	private Map<Id<Person>, Double> calculateSum(Map<Id<Person>, Double> personId2userBenefit, Map<Id<Person>, Double> personId2tollPayments) {
		Map<Id<Person>, Double> personId2Sum = new HashMap<Id<Person>, Double>();
		
		for (Id<Person> id : personId2userBenefit.keySet()) {
			if (personId2tollPayments.containsKey(id)) {
				personId2Sum.put(id, personId2userBenefit.get(id) + Math.abs(personId2tollPayments.get(id)));
			} else {
				personId2Sum.put(id, personId2userBenefit.get(id));
			}
		}
		return personId2Sum;
	}

	private Map<Id<Person>, Double> calculateDifference(ScenarioImpl scenario, Map<Id<Person>, Double> personId2value1, Map<Id<Person>, Double> personId2value2) {
		Map<Id<Person>, Double> personId2difference = new HashMap<Id<Person>, Double>();
		
		for (Id<Person> id : scenario.getPopulation().getPersons().keySet()) {
			double value1 = 0.;
			double value2 = 0.;
			if (personId2value1.containsKey(id)){
				value1 = personId2value1.get(id);
			}
			if (personId2value2.containsKey(id)) {
				value2 = personId2value2.get(id);
			}
			personId2difference.put(id, value1 - value2);
		}
		return personId2difference;
	}

	private Map<Id<Person>, Double> getPersonId2TollPayments(ScenarioImpl scenario, String runDirectory) {
		
		EventsManager events = EventsUtils.createEventsManager();
		
		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		events.addHandler(moneyHandler);
			
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + scenario.getConfig().controler().getLastIteration() + "/" + scenario.getConfig().controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");
		
		return moneyHandler.getPersonId2amount();
	}

	private Map<Id<Person>, Double> getPersonId2UserBenefit(Scenario scenario, String runDirectory) {
			
		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(scenario.getConfig(), WelfareMeasure.SELECTED, true);
		userBenefitsCalculator_selected.calculateUtility_money(scenario.getPopulation());
		Map<Id<Person>, Double> personId2userBenefit = userBenefitsCalculator_selected.getPersonId2MonetizedUtility();
		return personId2userBenefit;
	}
			 
}
		

