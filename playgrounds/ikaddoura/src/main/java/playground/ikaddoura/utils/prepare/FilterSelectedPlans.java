/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.utils.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
* @author ikaddoura
*/

public class FilterSelectedPlans {
	
	private static final Logger log = Logger.getLogger(FilterSelectedPlans.class);
	
	private final static String inputPlans = "/Users/ihab/Desktop/ils4i/kaddoura/bln-dz-time/output_route_time_baseCase_1000it/perf6.0_lateArrival-18.0_asas-true_actDurBin3600.0_tolerance3600.0_pricing-NoPricing_tollBlendFactor0.0_2017-02-16_15-28-38/output_plans.xml.gz";
	private final static String outputPlans = "/Users/ihab/Documents/workspace/runs-svn/berlin-dz-time/input/input_0.1sample/run_194c.150.plans.selected-1000it.route.time.output.plans-selected.xml.gz";
	private static final String[] attributes = {"OpeningClosingTimes"};
	
	public static void main(String[] args) {
		
		FilterSelectedPlans filter = new FilterSelectedPlans();
		filter.run(inputPlans, outputPlans, attributes);
	}
	
	public void run (final String inputPlans, final String outputPlans, final String[] attributes) {
		
		log.info("Accounting for the following attributes:");
		for (String attribute : attributes) {
			log.info(attribute);
		}
		log.info("Other person attributes will not appear in the output plans file.");
		
		Scenario scOutput;
		Scenario scInput = LoadMyScenarios.loadScenarioFromPlans(inputPlans);
		scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();
		
		for (Person p : scInput.getPopulation().getPersons().values()){
			Plan selectedPlan = p.getSelectedPlan();
			PopulationFactory factory = popOutput.getFactory();
			Person personNew = factory.createPerson(p.getId());
			
			for (String attribute : attributes) {
				personNew.getAttributes().putAttribute(attribute, p.getAttributes().getAttribute(attribute));
			}
									
			popOutput.addPerson(personNew);
			personNew.addPlan(selectedPlan);
		}
		
		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

