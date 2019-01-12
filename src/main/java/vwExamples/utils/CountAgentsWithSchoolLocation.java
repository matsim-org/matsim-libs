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
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

/**
 * @author saxer
 */
public class CountAgentsWithSchoolLocation {

    public static void main(String[] args) {

        int studentCounter = 0;
        // Create a Scenario
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        // Fill this Scenario with a population.
        new PopulationReader(scenario).readFile("E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\mergedPlans_filtered_Att_Stud_old.xml.gz");

        for (Person person : scenario.getPopulation().getPersons().values()) {
//
//			 String schoolLoc = (String)
//			 person.getAttributes().getAttribute("locationOfSchool");
//			 if (!schoolLoc.equals("-99"))
//			 {
//			 studentCounter++;
//			 }
            List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();

            for (PlanElement planElement : planElements) {

                if (planElement instanceof Activity) {
                    Activity act = (Activity) planElement;
                    if (act.getType().startsWith("education")) {
                        studentCounter++;
                        break;
                    }
                }
            }

            System.out.println(studentCounter);

        }
    }
}
