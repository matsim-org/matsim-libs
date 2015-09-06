/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.population;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;

/*
 * Dumps all plans of the Plansfile except the selected one.
 * Additionally the score could be reseted.
 */
public class PlansDumping {

//	private static String configFileName = "../matsim/mysimulations/kt-zurich/config.xml";
//	private static String networkFile = "../matsim/mysimulations/kt-zurich/input/network.xml";
	private static String populationFile = "C:/Users/Christoph/Desktop/Valora Kundenpotentiale/0001/20140417/2013.ch071.10pct.output_plans_selected.xml.gz";
	private static String populationOutFile = "C:/Users/Christoph/Desktop/Valora Kundenpotentiale/0001/20140417/2013.ch071.10pct.output_plans_selected_no_routes.xml.gz";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		plans.addAlgorithm(new PersonAlgorithm() {
			@Override
			public void run(Person person) {
				PersonImpl.removeUnselectedPlans(((PersonImpl) person));
				Plan plan = person.getSelectedPlan();
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						leg.setRoute(null);
					}
				}
			}
		});
		PopulationWriter populationWriter = new PopulationWriter(plans);
		populationWriter.startStreaming(populationOutFile);
		plans.addAlgorithm(populationWriter);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(populationFile);
		populationWriter.closeStreaming();
		
		System.out.println("Done");
	}

}
