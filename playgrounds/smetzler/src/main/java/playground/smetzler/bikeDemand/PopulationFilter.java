/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.smetzler.bikeDemand;


import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import java.util.Random;

/**
 * Filters a population by mode.
 * 
 * @author ikaddoura
 *
 */

// TODO modify filter: filter only bike trips, get only 1/10 of the trip to reach 1% demad for berlin 

public class PopulationFilter {

	static String inputPlansFile;
	static String outputPlansFile;
	static String networkFile;

	private Scenario scenario_input;
	private Scenario scenario_output;
	private static final Logger log = Logger.getLogger(PopulationFilter.class);
	private Random random = new Random(1234);


	public static void main(String[] args) {


		inputPlansFile =  "C:/users/Ettan/VSPworkspace/bvg.run189.10pct/bvg.run189.10pct.100.plans.selected.genericPt.xml.gz";
		outputPlansFile = "C:/users/Ettan/VSPworkspace/bvg.run189.10pct/bvg.run189.10pct.100.plans.selected_bikeonly_10percent_clean.xml.gz";


		PopulationFilter filter = new PopulationFilter();
		filter.run();		
	}

	private void run() {


		Config config1 = ConfigUtils.createConfig();
		//config1.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.createScenario(config1);
		StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
		Config config2 = ConfigUtils.createConfig();
		scenario_output = ScenarioUtils.loadScenario(config2);

		// to limit the no of persons, because my computer cant manage all of them
		// only works when comment line 128 (//throw new IllegalArgumentException("no node with id " + id); ) in networkUtils.java
		spr.addAlgorithm(new PersonAlgorithm() {

			@Override
			public void run(Person person) {


				Plan selectedPlan = person.getSelectedPlan();
				boolean planContainsBikeLeg = false;

				List<PlanElement> planElements = selectedPlan.getPlanElements();
				for (int i = 0, n = planElements.size(); i < n; i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						if (leg.getMode().equals(TransportMode.bike)){
							// leg has bike mode
							// get 1/100 only, be care sample is already 10%
//							if (random.nextDouble() > 0.90) {
								planContainsBikeLeg = true;
								//System.out.println("bike Leg");
//							}
						} else {
							// no bike mode
						}
					}
				}

				if (planContainsBikeLeg){

					Person personCopy = scenario_output.getPopulation().getFactory().createPerson(person.getId());
					List<PlanElement> planElements2 = selectedPlan.getPlanElements();
					for (int i = 0, n = planElements2.size(); i < n; i++) {
						PlanElement pe = planElements2.get(i);
						if (pe instanceof Activity) {
							Activity act = (Activity) pe;
							act.setFacilityId(null);
							act.setLinkId(null);		
						}

						if (pe instanceof Leg) {
							Leg leg = (Leg) pe;
							leg.setRoute(null);
						}
					}




					personCopy.addPlan(selectedPlan);
					scenario_output.getPopulation().addPerson(personCopy);
				}	

			}
		});
		spr.readFile(inputPlansFile);


		//PopulationWriter popWriter = new PopulationWriter(scenario_output.getPopulation(), scenario_input.getNetwork());
		PopulationWriter popWriter = new PopulationWriter(scenario_output.getPopulation(), null);
		popWriter.write(outputPlansFile);

		//log.info("Number of selected plans in input population: " + spr.get);
		log.info("Number of selected plans in output population: " + scenario_output.getPopulation().getPersons().size());

	}

}
