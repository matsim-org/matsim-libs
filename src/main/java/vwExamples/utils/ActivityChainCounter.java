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

package vwExamples.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.StringJoiner;

/**
 * @author saxer
 */
public class ActivityChainCounter {

    public static void main(String[] args) {

        // Create a Scenario
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        // Fill this Scenario with a population.
        new PopulationReader(scenario).readFile(
                "D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw272.0.1\\vw272.0.1.output_plans.xml.gz");
//		String randomOrderedPop = "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\vw270_0.1_CT_0.1\\Population\\populationWithCTdemand_sel.xml.gz";
//		StreamingPopulationWriter filteredPop = new StreamingPopulationWriter();
//		filteredPop.startStreaming(randomOrderedPop);

        int personCounter = 0;
        int personMultiWorkCounter = 0;
        for (Person person : scenario.getPopulation().getPersons().values()) {

            if (checkMultiWorkPlans(person.getSelectedPlan())) {
                personMultiWorkCounter++;
            }
//			filteredPop.writePerson(person);
            personCounter++;
        }

        System.out.println("Persons: " + personCounter + " || " + "MultiWorker > 2: " + personMultiWorkCounter);

//		filteredPop.closeStreaming();

    }

    public static boolean checkMultiWorkPlans(Plan plan) {
        StringJoiner joiner = new StringJoiner("-");

        String requiredChain = "work-work-work-work-work-work-work-work-work";
        for (PlanElement pe : plan.getPlanElements()) {
            if (pe instanceof Activity) {
                String act = pe.toString();

                if (act.contains("home")) {
                    act = "home";
                } else if (act.contains("work")) {
                    act = "work";
                } else if (act.contains("shopping")) {
                    act = "shopping";
                } else if (act.contains("other")) {
                    act = "other";
                } else if (act.contains("leisure")) {
                    act = "leisure";
                }

                joiner.add(act);
            }

        }

        return joiner.toString().contains(requiredChain);

    }
}
