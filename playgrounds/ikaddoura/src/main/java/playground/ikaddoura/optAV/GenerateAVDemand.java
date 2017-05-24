/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.optAV;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class GenerateAVDemand {

	private static final Logger log = Logger.getLogger(GenerateAVDemand.class);
	
	private static final String inputDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_scenario_2016/be_122/";
	private static final String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV-be122/input/";
	
	private static final String inputPlansFile = "be_122.output_plans.xml.gz";
	
	private static final double taxiTripShare = 0.5;
	
	private static final boolean specificArea = true;
	private static final double minX = 4554761.;
	private static final double minY = 5793603.;
	private static final double maxX = 4631345.;
	private static final double maxY = 5846740.;

	private static final String[] attributes = {};
//	private static final String[] attributes = {"OpeningClosingTimes"};
	
	// ####################################################################
		
	private static String inputPlans;
	private static String outputPlans; 
	
	private static int counterTaxiTrips = 0;
	private static int counterCarTrips = 0;	

	private static Random random = MatsimRandom.getLocalInstance();
	
	public static void main(String[] args) {			
		
		inputPlans = inputDirectory + inputPlansFile;
		
		String inputPlansFileName = inputPlansFile;
		if (inputPlansFile.endsWith(".xml.gz")) {
			inputPlansFileName = inputPlansFileName.replaceAll(".xml.gz", "");
		} else if (inputPlansFileName.endsWith(".xml")) {
			inputPlansFileName = inputPlansFileName.replaceAll(".xml", "");
		} else {
			throw new RuntimeException("plans file " + inputPlansFile + " should end with .xml or .xml.gz. Aborting...");
		}
		outputPlans = outputDirectory + inputPlansFileName + ".avTripShare" + taxiTripShare + ".specificArea-" + specificArea + ".xml.gz";
		
		GenerateAVDemand generateAVDemand = new GenerateAVDemand();
		generateAVDemand.run();
	}

	private void run() {
		if (specificArea) {
			createTaxiTripsForAgentsWithAllTripsInSpecificArea();
		} else {
			createTaxiTripsForAllAgents();	
		}
	}

	private void createTaxiTripsForAgentsWithAllTripsInSpecificArea() {
		log.info("taxi trip share: " + taxiTripShare);

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario scenarioInput = ScenarioUtils.loadScenario(config);
		
		Scenario scenarioOutput = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population populationOutput = scenarioOutput.getPopulation();
		
		for (Person person : scenarioInput.getPopulation().getPersons().values()){
			
			PopulationFactory factory = populationOutput.getFactory();
			Person personClone = factory.createPerson(person.getId());
			for (String attribute : attributes) {
				personClone.getAttributes().putAttribute(attribute, person.getAttributes().getAttribute(attribute));
			}
			populationOutput.addPerson(personClone);
			personClone.addPlan(person.getSelectedPlan());
			
			boolean allActivitiesInBoundary = true;
			List<PlanElement> planElements = personClone.getSelectedPlan().getPlanElements();
			
			for (int i = 0, n = planElements.size(); i < n; i++) {
				PlanElement pe = planElements.get(i);
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.getCoord() == null) throw new RuntimeException("Activity without coordinate. Maybe use the link from / to node coordinates instead. Aborting...");
					if (act.getCoord().getX() > maxX || act.getCoord().getX() < minX || act.getCoord().getY() > maxY || act.getCoord().getY() < minY) {
						allActivitiesInBoundary = false;
						break;
					}
				}
			}
			
			if (allActivitiesInBoundary) {
				// adjust the personClone's selected plan
				for (int i = 0, n = planElements.size(); i < n; i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						if (leg.getMode().equals(TransportMode.car)){
							// leg has car mode
							if (random.nextDouble() < taxiTripShare) {
								leg.setMode("taxi");
								leg.setRoute(null);
								leg.setTravelTime(0);
								counterTaxiTrips++;
							} else {
								counterCarTrips++;
							}
						}
					}
				}
			}
		}
		
		new PopulationWriter(scenarioOutput.getPopulation()).write(outputPlans);
		log.info("Number of car trips: " + counterCarTrips);
		log.info("Number of taxi trips: " + counterTaxiTrips);
		log.info("Share of taxi trips: " + counterTaxiTrips / (double) (counterCarTrips + counterTaxiTrips) );
	}

	public void createTaxiTripsForAllAgents() {
		
		log.info("taxi trip share: " + taxiTripShare);

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario scenarioInput = ScenarioUtils.loadScenario(config);
		
		Scenario scenarioOutput = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population populationOutput = scenarioOutput.getPopulation();
		
		for (Person person : scenarioInput.getPopulation().getPersons().values()){
			
			PopulationFactory factory = populationOutput.getFactory();
			Person personClone = factory.createPerson(person.getId());
			for (String attribute : attributes) {
				personClone.getAttributes().putAttribute(attribute, person.getAttributes().getAttribute(attribute));
			}
			populationOutput.addPerson(personClone);
			personClone.addPlan(person.getSelectedPlan());
			
			// adjust the personClone's selected plan
			List<PlanElement> planElements = personClone.getSelectedPlan().getPlanElements();
			for (int i = 0, n = planElements.size(); i < n; i++) {
				PlanElement pe = planElements.get(i);
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getMode().equals(TransportMode.car)){
						// leg has car mode
						if (random.nextDouble() < taxiTripShare) {
							leg.setMode("taxi");
							leg.setRoute(null);
							leg.setTravelTime(0);
							counterTaxiTrips++;
						} else {
							counterCarTrips++;
						}
					}
				}
			}
		}
		
		new PopulationWriter(scenarioOutput.getPopulation()).write(outputPlans);
		log.info("Number of car trips: " + counterCarTrips);
		log.info("Number of taxi trips: " + counterTaxiTrips);
		log.info("Share of taxi trips: " + counterTaxiTrips / (double) (counterCarTrips + counterTaxiTrips) );
	}

}

